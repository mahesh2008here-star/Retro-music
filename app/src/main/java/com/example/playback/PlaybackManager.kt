package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.RepeatModeState
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val volume: Float = 1f,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class PlaybackManager(
    context: Context,
    private val onTrackStarted: (Track) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var pendingAction: ((MediaController) -> Unit)? = null
    private var lastReportedTrackKey: String? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressTicker() else stopProgressTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            val ctrl = controller
            val duration = ctrl?.duration?.takeIf { it > 0L } ?: _state.value.currentTrack?.durationMs ?: 0L
            _state.update {
                it.copy(
                    isBuffering = buffering,
                    durationMs = duration,
                    positionMs = ctrl?.currentPosition?.coerceAtLeast(0L) ?: it.positionMs
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncFromController()
            val current = _state.value.currentTrack
            if (current != null && current.key != lastReportedTrackKey) {
                lastReportedTrackKey = current.key
                onTrackStarted(current)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatModeState.ALL
                Player.REPEAT_MODE_ONE -> RepeatModeState.ONE
                else -> RepeatModeState.OFF
            }
            _state.update { it.copy(repeatMode = mapped) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error: ${error.errorCodeName}", error)
            val msg = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network interrupted while streaming audio."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                    "Stream URL expired or unavailable from provider."
                else -> error.localizedMessage ?: "Playback unavailable for this track."
            }
            _state.update { it.copy(isPlaying = false, isBuffering = false, errorMessage = msg) }
            stopProgressTicker()
        }
    }

    init {
        connectController()
    }

    private fun connectController() {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MasterWaveMediaService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val ctrl = future.get()
                    controller = ctrl
                    ctrl.addListener(playerListener)
                    syncFromController()
                    pendingAction?.let { action ->
                        pendingAction = null
                        action(ctrl)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect MediaController", e)
                    _state.update { it.copy(errorMessage = "Audio engine initialization failed.") }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun withController(action: (MediaController) -> Unit) {
        val ctrl = controller
        if (ctrl != null) {
            action(ctrl)
        } else {
            pendingAction = action
            if (controllerFuture == null) connectController()
        }
    }

    fun playTrack(track: Track, contextTracks: List<Track> = listOf(track)) {
        if (!track.isPlayableInApp) {
            _state.update {
                it.copy(
                    errorMessage = "${track.title} is ${track.capability.badgeLabel} and cannot stream inside MasterWave."
                )
            }
            return
        }
        val playableList = contextTracks.filter { it.isPlayableInApp }.ifEmpty { listOf(track) }
        val targetIndex = playableList.indexOfFirst { it.key == track.key }.coerceAtLeast(0)
        playQueue(playableList, targetIndex)
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        val playable = tracks.filter { it.isPlayableInApp }
        if (playable.isEmpty()) {
            _state.update { it.copy(errorMessage = "No playable audio streams found in this selection.") }
            return
        }
        val safeIndex = startIndex.coerceIn(0, playable.lastIndex)
        val selected = playable[safeIndex]

        _state.update {
            it.copy(
                queue = playable,
                currentIndex = safeIndex,
                currentTrack = selected,
                isBuffering = true,
                positionMs = 0L,
                durationMs = selected.durationMs,
                errorMessage = null
            )
        }

        withController { ctrl ->
            val mediaItems = playable.map { it.toMediaItem() }
            ctrl.setMediaItems(mediaItems, safeIndex, 0L)
            ctrl.prepare()
            ctrl.play()
            if (selected.key != lastReportedTrackKey) {
                lastReportedTrackKey = selected.key
                onTrackStarted(selected)
            }
        }
    }

    fun togglePlayPause() {
        withController { ctrl ->
            if (ctrl.isPlaying) {
                ctrl.pause()
            } else {
                if (ctrl.playbackState == Player.STATE_IDLE && _state.value.currentTrack != null) {
                    ctrl.prepare()
                }
                ctrl.play()
            }
        }
    }

    fun next() {
        withController { ctrl ->
            if (ctrl.hasNextMediaItem()) {
                ctrl.seekToNextMediaItem()
                ctrl.play()
            } else if (_state.value.queue.isNotEmpty() && _state.value.repeatMode == RepeatModeState.ALL) {
                ctrl.seekTo(0, 0L)
                ctrl.play()
            }
        }
    }

    fun previous() {
        withController { ctrl ->
            if (ctrl.currentPosition > 3500L) {
                ctrl.seekTo(0L)
            } else if (ctrl.hasPreviousMediaItem()) {
                ctrl.seekToPreviousMediaItem()
                ctrl.play()
            } else {
                ctrl.seekTo(0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        _state.update { it.copy(positionMs = clamped) }
        withController { ctrl -> ctrl.seekTo(clamped) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.update { it.copy(volume = clamped) }
        withController { ctrl -> ctrl.volume = clamped }
    }

    fun toggleShuffle() {
        withController { ctrl ->
            ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        withController { ctrl ->
            ctrl.repeatMode = when (ctrl.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun addToQueue(track: Track) {
        if (!track.isPlayableInApp) return
        val currentQueue = _state.value.queue
        if (currentQueue.isEmpty()) {
            playTrack(track)
            return
        }
        val updated = currentQueue + track
        _state.update { it.copy(queue = updated) }
        withController { ctrl ->
            ctrl.addMediaItem(track.toMediaItem())
        }
    }

    fun playNextInQueue(track: Track) {
        if (!track.isPlayableInApp) return
        val currentQueue = _state.value.queue
        if (currentQueue.isEmpty()) {
            playTrack(track)
            return
        }
        val insertIndex = (_state.value.currentIndex + 1).coerceIn(0, currentQueue.size)
        val updated = currentQueue.toMutableList().apply { add(insertIndex, track) }
        _state.update { it.copy(queue = updated) }
        withController { ctrl ->
            ctrl.addMediaItem(insertIndex, track.toMediaItem())
        }
    }

    fun playQueueIndex(index: Int) {
        val queue = _state.value.queue
        if (index !in queue.indices) return
        _state.update {
            it.copy(
                currentIndex = index,
                currentTrack = queue[index],
                errorMessage = null
            )
        }
        withController { ctrl ->
            ctrl.seekTo(index, 0L)
            ctrl.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val current = _state.value
        if (index !in current.queue.indices) return
        val updated = current.queue.toMutableList().apply { removeAt(index) }
        if (updated.isEmpty()) {
            clearQueue()
            return
        }
        val newIndex = when {
            index < current.currentIndex -> current.currentIndex - 1
            index == current.currentIndex -> current.currentIndex.coerceAtMost(updated.lastIndex)
            else -> current.currentIndex
        }
        _state.update {
            it.copy(
                queue = updated,
                currentIndex = newIndex,
                currentTrack = updated.getOrNull(newIndex)
            )
        }
        withController { ctrl ->
            ctrl.removeMediaItem(index)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val current = _state.value
        val queue = current.queue
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return
        val mutable = queue.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        val updatedCurrentIndex = when (current.currentIndex) {
            fromIndex -> toIndex
            in (minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) -> {
                if (fromIndex < toIndex) current.currentIndex - 1 else current.currentIndex + 1
            }
            else -> current.currentIndex
        }
        _state.update {
            it.copy(
                queue = mutable,
                currentIndex = updatedCurrentIndex,
                currentTrack = mutable.getOrNull(updatedCurrentIndex)
            )
        }
        withController { ctrl ->
            ctrl.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun clearQueue() {
        stopProgressTicker()
        lastReportedTrackKey = null
        _state.update {
            PlaybackUiState(
                volume = it.volume,
                shuffleEnabled = it.shuffleEnabled,
                repeatMode = it.repeatMode
            )
        }
        withController { ctrl ->
            ctrl.stop()
            ctrl.clearMediaItems()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun syncFromController() {
        val ctrl = controller ?: return
        val queue = _state.value.queue
        val idx = ctrl.currentMediaItemIndex.takeIf { it in queue.indices } ?: _state.value.currentIndex
        val track = queue.getOrNull(idx) ?: _state.value.currentTrack
        val duration = ctrl.duration.takeIf { it > 0L } ?: track?.durationMs ?: 0L
        _state.update {
            it.copy(
                currentIndex = idx,
                currentTrack = track,
                isPlaying = ctrl.isPlaying,
                isBuffering = ctrl.playbackState == Player.STATE_BUFFERING,
                positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                shuffleEnabled = ctrl.shuffleModeEnabled,
                volume = ctrl.volume
            )
        }
    }

    private fun startProgressTicker() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null && ctrl.isPlaying) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.takeIf { it > 0L } ?: _state.value.durationMs
                    _state.update {
                        if (it.positionMs != pos || it.durationMs != dur) {
                            it.copy(positionMs = pos, durationMs = dur)
                        } else it
                    }
                }
                delay(400L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTicker()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private fun Track.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .apply {
                if (artworkUrl.isNotBlank()) {
                    setArtworkUri(Uri.parse(artworkUrl))
                }
            }
            .build()
        return MediaItem.Builder()
            .setMediaId(key)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private companion object {
        const val TAG = "PlaybackManager"
    }
}
