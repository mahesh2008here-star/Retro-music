package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferencesState
import com.example.data.remote.CatalogFeed
import com.example.data.remote.MusicCatalogRepository
import com.example.model.Album
import com.example.model.AudioQualityPreference
import com.example.model.GenreMood
import com.example.model.GlassQualityMode
import com.example.model.SearchResults
import com.example.model.ThemeModePreference
import com.example.model.Track
import com.example.playback.PlaybackManager
import com.example.playback.PlaybackUiState
import com.example.ui.artwork.ArtworkPalette
import com.example.ui.artwork.ArtworkPaletteExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    val catalogRepository = MusicCatalogRepository()
    private val preferencesRepository = PreferencesRepository(application)
    private val paletteExtractor = ArtworkPaletteExtractor(application)

    private var onTrackPlayedCallback: ((Track) -> Unit)? = null

    val playbackManager = PlaybackManager(
        context = application,
        onTrackStarted = { track -> onTrackPlayedCallback?.invoke(track) }
    )

    val playbackState: StateFlow<PlaybackUiState> = playbackManager.state

    val preferencesState: StateFlow<UserPreferencesState> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesState())

    private val _catalogState = MutableStateFlow<UiState<CatalogFeed>>(UiState.Loading)
    val catalogState: StateFlow<UiState<CatalogFeed>> = _catalogState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow<UiState<SearchResults>>(UiState.Success(SearchResults()))
    val searchState: StateFlow<UiState<SearchResults>> = _searchState.asStateFlow()

    private val _selectedGenre = MutableStateFlow<GenreMood?>(null)
    val selectedGenre: StateFlow<GenreMood?> = _selectedGenre.asStateFlow()

    private val _genreTracksState = MutableStateFlow<UiState<List<Track>>>(UiState.Success(emptyList()))
    val genreTracksState: StateFlow<UiState<List<Track>>> = _genreTracksState.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _albumTracksState = MutableStateFlow<UiState<List<Track>>>(UiState.Success(emptyList()))
    val albumTracksState: StateFlow<UiState<List<Track>>> = _albumTracksState.asStateFlow()

    private val _artworkPalette = MutableStateFlow(ArtworkPalette())
    val artworkPalette: StateFlow<ArtworkPalette> = _artworkPalette.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCatalog()
        observeCurrentTrackPalette()
    }

    fun attachHistoryRecorder(recorder: (Track) -> Unit) {
        onTrackPlayedCallback = recorder
    }

    fun loadCatalog(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _catalogState.value = UiState.Loading
            try {
                val feed = catalogRepository.loadHomeCatalog(
                    forceRefresh = forceRefresh,
                    dataSaver = preferencesState.value.dataSaver
                )
                _catalogState.value = UiState.Success(feed)
            } catch (e: Exception) {
                _catalogState.value = UiState.Error(
                    e.localizedMessage ?: "Unable to reach music catalogs. Check network connectivity."
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = UiState.Success(SearchResults())
            return
        }
        searchJob = viewModelScope.launch {
            delay(320L)
            executeSearch(query, saveHistory = false)
        }
    }

    fun submitSearch(query: String = _searchQuery.value) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        searchJob?.cancel()
        if (trimmed.isBlank()) return
        searchJob = viewModelScope.launch {
            preferencesRepository.addRecentSearch(trimmed)
            executeSearch(trimmed, saveHistory = true)
        }
    }

    private suspend fun executeSearch(query: String, saveHistory: Boolean) {
        _searchState.value = UiState.Loading
        catalogRepository.searchAll(query, dataSaver = preferencesState.value.dataSaver)
            .onSuccess { results ->
                _searchState.value = UiState.Success(results)
            }
            .onFailure { e ->
                _searchState.value = UiState.Error(
                    e.localizedMessage ?: "Search failed. Please verify connectivity."
                )
            }
    }

    fun openGenre(genre: GenreMood) {
        _selectedGenre.value = genre
        viewModelScope.launch {
            _genreTracksState.value = UiState.Loading
            catalogRepository.fetchGenreTracks(genre, preferencesState.value.dataSaver)
                .onSuccess { tracks -> _genreTracksState.value = UiState.Success(tracks) }
                .onFailure { e ->
                    _genreTracksState.value = UiState.Error(e.localizedMessage ?: "Failed to load genre")
                }
        }
    }

    fun closeGenre() {
        _selectedGenre.value = null
    }

    fun openAlbum(album: Album) {
        _selectedAlbum.value = album
        viewModelScope.launch {
            _albumTracksState.value = UiState.Loading
            catalogRepository.fetchAlbumTracks(album, preferencesState.value.dataSaver)
                .onSuccess { tracks -> _albumTracksState.value = UiState.Success(tracks) }
                .onFailure { e ->
                    _albumTracksState.value = UiState.Error(e.localizedMessage ?: "Failed to load album tracks")
                }
        }
    }

    fun closeAlbum() {
        _selectedAlbum.value = null
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch { preferencesRepository.removeRecentSearch(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { preferencesRepository.clearRecentSearches() }
    }

    fun setThemeMode(mode: ThemeModePreference) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setGlassQuality(mode: GlassQualityMode) {
        viewModelScope.launch { preferencesRepository.setGlassQuality(mode) }
    }

    fun setAudioQuality(quality: AudioQualityPreference) {
        viewModelScope.launch { preferencesRepository.setAudioQuality(quality) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReducedMotion(enabled) }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDataSaver(enabled) }
    }

    fun clearAllCaches() {
        catalogRepository.clearMemoryCache()
        paletteExtractor.clearCache()
        viewModelScope.launch {
            preferencesRepository.clearRecentSearches()
        }
        loadCatalog(forceRefresh = true)
    }

    private fun observeCurrentTrackPalette() {
        var lastUrl = ""
        viewModelScope.launch {
            playbackState.collect { state ->
                val artUrl = state.currentTrack?.artworkUrl.orEmpty()
                if (artUrl != lastUrl) {
                    lastUrl = artUrl
                    _artworkPalette.value = if (artUrl.isBlank()) {
                        ArtworkPalette()
                    } else {
                        paletteExtractor.extract(artUrl)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
