package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.LibraryRepository
import com.example.model.Album
import com.example.model.Artist
import com.example.model.AudioQualityPreference
import com.example.model.FollowedArtistDoc
import com.example.model.LikedTrackDoc
import com.example.model.ListeningHistoryDoc
import com.example.model.PlaylistDoc
import com.example.model.PlaylistTrackDoc
import com.example.model.SavedAlbumDoc
import com.example.model.Track
import com.example.model.UserProfileDoc
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    userPhotoUrl: String?
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl
            )
        }
    }

    val userProfile: StateFlow<UserProfileDoc?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "userProfile")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val likedTracksState: StateFlow<UiState<List<LikedTrackDoc>>> =
        repository.observeLikedTracks(currentUserId).toUiState("liked tracks")

    val playlistsState: StateFlow<UiState<List<PlaylistDoc>>> =
        repository.observePlaylists(currentUserId).toUiState("playlists")

    val savedAlbumsState: StateFlow<UiState<List<SavedAlbumDoc>>> =
        repository.observeSavedAlbums(currentUserId).toUiState("saved albums")

    val followedArtistsState: StateFlow<UiState<List<FollowedArtistDoc>>> =
        repository.observeFollowedArtists(currentUserId).toUiState("followed artists")

    val listeningHistoryState: StateFlow<UiState<List<ListeningHistoryDoc>>> =
        repository.observeListeningHistory(currentUserId).toUiState("listening history")

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPlaylistTracksState: StateFlow<UiState<List<PlaylistTrackDoc>>> =
        _selectedPlaylistId.flatMapLatest { playlistId ->
            if (playlistId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observePlaylistTracks(currentUserId, playlistId)
                    .map<List<PlaylistTrackDoc>, UiState<List<PlaylistTrackDoc>>> { UiState.Success(it) }
                    .catch { error ->
                        val msg = unwrapFirestoreError(error, "playlist tracks")
                        emit(UiState.Error(msg))
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val likedTrackKeys: StateFlow<Set<String>> = likedTracksState
        .map { state ->
            (state as? UiState.Success)?.data?.map { it.trackKey }?.toSet() ?: emptySet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val savedAlbumKeys: StateFlow<Set<String>> = savedAlbumsState
        .map { state ->
            (state as? UiState.Success)?.data?.map { it.albumKey }?.toSet() ?: emptySet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val followedArtistKeys: StateFlow<Set<String>> = followedArtistsState
        .map { state ->
            (state as? UiState.Success)?.data?.map { it.artistKey }?.toSet() ?: emptySet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    fun selectPlaylist(playlistId: String?) {
        _selectedPlaylistId.value = playlistId
    }

    fun toggleLike(track: Track) {
        val currentlyLiked = likedTrackKeys.value.contains(track.key)
        viewModelScope.launch {
            repository.toggleLikeTrack(currentUserId, track, currentlyLiked)
                .onSuccess { nowLiked ->
                    _events.tryEmit(if (nowLiked) "Added to Liked Songs" else "Removed from Liked Songs")
                }
                .onFailure { e ->
                    _events.tryEmit(e.localizedMessage ?: "Could not update liked track")
                }
        }
    }

    fun toggleSaveAlbum(album: Album) {
        val currentlySaved = savedAlbumKeys.value.contains(album.key)
        viewModelScope.launch {
            repository.toggleSaveAlbum(currentUserId, album, currentlySaved)
                .onSuccess { saved ->
                    _events.tryEmit(if (saved) "Saved album to Library" else "Removed album from Library")
                }
                .onFailure { e ->
                    _events.tryEmit(e.localizedMessage ?: "Could not save album")
                }
        }
    }

    fun toggleFollowArtist(artist: Artist) {
        val currentlyFollowed = followedArtistKeys.value.contains(artist.key)
        viewModelScope.launch {
            repository.toggleFollowArtist(currentUserId, artist, currentlyFollowed)
                .onSuccess { followed ->
                    _events.tryEmit(if (followed) "Following ${artist.name}" else "Unfollowed ${artist.name}")
                }
                .onFailure { e ->
                    _events.tryEmit(e.localizedMessage ?: "Could not update artist follow")
                }
        }
    }

    fun createPlaylist(name: String, description: String = "", initialTrack: Track? = null) {
        viewModelScope.launch {
            repository.createPlaylist(currentUserId, name, description)
                .onSuccess { newId ->
                    if (initialTrack != null) {
                        val stub = PlaylistDoc(id = newId, userId = currentUserId, name = name)
                        repository.addTrackToPlaylist(currentUserId, stub, initialTrack)
                    }
                    _events.tryEmit("Created playlist \"$name\"")
                }
                .onFailure { e ->
                    _events.tryEmit(e.localizedMessage ?: "Could not create playlist")
                }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String, newDescription: String? = null) {
        viewModelScope.launch {
            repository.renamePlaylist(currentUserId, playlistId, newName, newDescription)
                .onSuccess { _events.tryEmit("Playlist updated") }
                .onFailure { e -> _events.tryEmit(e.localizedMessage ?: "Could not rename playlist") }
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (_selectedPlaylistId.value == playlistId) {
            _selectedPlaylistId.value = null
        }
        viewModelScope.launch {
            repository.deletePlaylist(currentUserId, playlistId)
                .onSuccess { _events.tryEmit("Playlist deleted") }
                .onFailure { e -> _events.tryEmit(e.localizedMessage ?: "Could not delete playlist") }
        }
    }

    fun addTrackToPlaylist(playlist: PlaylistDoc, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(currentUserId, playlist, track)
                .onSuccess { _events.tryEmit("Added to ${playlist.name}") }
                .onFailure { e -> _events.tryEmit(e.localizedMessage ?: "Could not add track") }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, entryId: String, currentSize: Int) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(
                userId = currentUserId,
                playlistId = playlistId,
                entryId = entryId,
                remainingCount = (currentSize - 1).coerceAtLeast(0).toLong()
            ).onFailure { e ->
                _events.tryEmit(e.localizedMessage ?: "Could not remove track")
            }
        }
    }

    fun movePlaylistTrack(playlistId: String, tracks: List<PlaylistTrackDoc>, fromIndex: Int, toIndex: Int) {
        if (fromIndex !in tracks.indices || toIndex !in tracks.indices || fromIndex == toIndex) return
        val first = tracks[fromIndex]
        val second = tracks[toIndex]
        viewModelScope.launch {
            repository.swapPlaylistTrackPositions(currentUserId, playlistId, first, second)
                .onFailure { e -> _events.tryEmit(e.localizedMessage ?: "Could not reorder track") }
        }
    }

    fun recordPlay(track: Track) {
        viewModelScope.launch {
            repository.recordListeningHistory(currentUserId, track)
        }
    }

    fun syncAudioQuality(quality: AudioQualityPreference) {
        viewModelScope.launch {
            repository.updatePreferredQuality(currentUserId, quality)
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapFirestoreError(error, label)
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private fun unwrapFirestoreError(error: Throwable, label: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        return rootError.message ?: "Failed to load $label"
    }

    private companion object {
        const val TAG = "LibraryVM"
    }
}
