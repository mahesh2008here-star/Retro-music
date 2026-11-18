package com.example.data.firebase

import android.content.Context
import com.example.R
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
import com.example.model.toFirestoreTrackFields
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LibraryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing library.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        photoUrl: String?,
        preferredQuality: AudioQualityPreference = AudioQualityPreference.HIGH
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val safeName = displayName.trim().take(80).ifBlank { "MasterWave Listener" }
            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredQuality" to preferredQuality.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                email?.takeIf { it.isNotBlank() }?.let { payload["email"] = it.take(120) }
                photoUrl?.takeIf { it.isNotBlank() }?.let { payload["photoUrl"] = it.take(500) }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                email?.takeIf { it.isNotBlank() }?.let { updates["email"] = it.take(120) }
                photoUrl?.takeIf { it.isNotBlank() }?.let { updates["photoUrl"] = it.take(500) }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredQuality(userId: String, quality: AudioQualityPreference): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredQuality" to quality.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeLikedTracks(userId: String): Flow<List<LikedTrackDoc>> =
        db.collection("users").document(userId).collection("liked_tracks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LikedTrackDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observePlaylists(userId: String): Flow<List<PlaylistDoc>> =
        db.collection("users").document(userId).collection("playlists")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlaylistDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observePlaylistTracks(userId: String, playlistId: String): Flow<List<PlaylistTrackDoc>> =
        db.collection("users").document(userId)
            .collection("playlists").document(playlistId)
            .collection("tracks")
            .orderBy("position", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlaylistTrackDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeSavedAlbums(userId: String): Flow<List<SavedAlbumDoc>> =
        db.collection("users").document(userId).collection("saved_albums")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavedAlbumDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeFollowedArtists(userId: String): Flow<List<FollowedArtistDoc>> =
        db.collection("users").document(userId).collection("followed_artists")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FollowedArtistDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeListeningHistory(userId: String): Flow<List<ListeningHistoryDoc>> =
        db.collection("users").document(userId).collection("listening_history")
            .orderBy("playedAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ListeningHistoryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun toggleLikeTrack(userId: String, track: Track, currentlyLiked: Boolean): Result<Boolean> {
        val docId = track.safeFirestoreId
        val docPath = "users/$userId/liked_tracks/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("liked_tracks").document(docId)
            if (currentlyLiked) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = track.toFirestoreTrackFields(uid).apply {
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (currentlyLiked) OperationType.DELETE else OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(userId: String, name: String, description: String = ""): Result<String> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || trimmedName.length > 80) {
            return Result.failure(IllegalArgumentException("Playlist name must be 1–80 characters."))
        }
        val docRef = db.collection("users").document(userId).collection("playlists").document()
        return try {
            val uid = requireUserId(userId)
            val payload = mapOf(
                "userId" to uid,
                "name" to trimmedName,
                "description" to description.trim().take(240),
                "artworkUrl" to "",
                "trackCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun renamePlaylist(
        userId: String,
        playlistId: String,
        newName: String,
        newDescription: String? = null
    ): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed.length > 80) {
            return Result.failure(IllegalArgumentException("Playlist name must be 1–80 characters."))
        }
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId(userId)
            require(playlistId.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "name" to trimmed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newDescription != null) {
                updates["description"] = newDescription.trim().take(240)
            }
            db.collection("users").document(uid)
                .collection("playlists").document(playlistId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId(userId)
            require(playlistId.isNotBlank())
            val playlistRef = db.collection("users").document(uid)
                .collection("playlists").document(playlistId)
            val trackDocs = playlistRef.collection("tracks").get().await()
            val batch = db.batch()
            for (doc in trackDocs.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(playlistRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTrackToPlaylist(userId: String, playlist: PlaylistDoc, track: Track): Result<Unit> {
        val docPath = "users/$userId/playlists/${playlist.id}/tracks"
        return try {
            val uid = requireUserId(userId)
            val playlistRef = db.collection("users").document(uid)
                .collection("playlists").document(playlist.id)
            val existingTracks = playlistRef.collection("tracks").get().await()
            val nextPosition = (existingTracks.documents.maxOfOrNull {
                (it.getLong("position") ?: 0L)
            } ?: -1L) + 1L

            val entryRef = playlistRef.collection("tracks").document()
            val payload = track.toFirestoreTrackFields(uid).apply {
                put("playlistId", playlist.id)
                put("position", nextPosition.coerceIn(0L, 5000L))
                put("createdAt", FieldValue.serverTimestamp())
            }
            entryRef.set(payload).await()

            val playlistUpdates = mutableMapOf<String, Any>(
                "trackCount" to (existingTracks.size() + 1).toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (playlist.artworkUrl.isBlank() && track.artworkUrl.isNotBlank()) {
                playlistUpdates["artworkUrl"] = track.artworkUrl.take(500)
            }
            playlistRef.update(playlistUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeTrackFromPlaylist(
        userId: String,
        playlistId: String,
        entryId: String,
        remainingCount: Long
    ): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId/tracks/$entryId"
        return try {
            val uid = requireUserId(userId)
            val playlistRef = db.collection("users").document(uid)
                .collection("playlists").document(playlistId)
            playlistRef.collection("tracks").document(entryId).delete().await()
            playlistRef.update(
                mapOf(
                    "trackCount" to remainingCount.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun swapPlaylistTrackPositions(
        userId: String,
        playlistId: String,
        first: PlaylistTrackDoc,
        second: PlaylistTrackDoc
    ): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId/tracks"
        return try {
            val uid = requireUserId(userId)
            val tracksRef = db.collection("users").document(uid)
                .collection("playlists").document(playlistId)
                .collection("tracks")
            val batch = db.batch()
            batch.update(tracksRef.document(first.id), mapOf("position" to second.position))
            batch.update(tracksRef.document(second.id), mapOf("position" to first.position))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSaveAlbum(userId: String, album: Album, currentlySaved: Boolean): Result<Boolean> {
        val docId = album.safeFirestoreId
        val docPath = "users/$userId/saved_albums/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("saved_albums").document(docId)
            if (currentlySaved) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "albumKey" to album.key.take(128),
                    "source" to album.source.name,
                    "title" to album.title.take(160).ifBlank { "Untitled Album" },
                    "artist" to album.artist.take(120).ifBlank { "Various Artists" },
                    "artworkUrl" to album.artworkUrl.take(500),
                    "externalUrl" to album.externalUrl.take(500),
                    "releaseYear" to album.releaseYear.take(16),
                    "trackCount" to album.trackCount.toLong().coerceIn(0L, 500L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (currentlySaved) OperationType.DELETE else OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFollowArtist(userId: String, artist: Artist, currentlyFollowed: Boolean): Result<Boolean> {
        val docId = artist.safeFirestoreId
        val docPath = "users/$userId/followed_artists/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("followed_artists").document(docId)
            if (currentlyFollowed) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "artistKey" to artist.key.take(128),
                    "source" to artist.source.name,
                    "name" to artist.name.take(120).ifBlank { "Artist" },
                    "artworkUrl" to artist.artworkUrl.take(500),
                    "genre" to artist.genre.take(60),
                    "externalUrl" to artist.externalUrl.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (currentlyFollowed) OperationType.DELETE else OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordListeningHistory(userId: String, track: Track): Result<Unit> {
        val docId = track.safeFirestoreId
        val docPath = "users/$userId/listening_history/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("listening_history").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "playedAt" to FieldValue.serverTimestamp(),
                        "streamUrl" to track.streamUrl.take(500),
                        "artworkUrl" to track.artworkUrl.take(500)
                    )
                ).await()
            } else {
                val payload = track.toFirestoreTrackFields(uid).apply {
                    put("playedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
