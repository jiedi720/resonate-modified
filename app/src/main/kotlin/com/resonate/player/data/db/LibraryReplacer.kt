package com.resonate.player.data.db

import androidx.room.withTransaction

/**
 * Atomically replaces the scanned library while preserving everything the user
 * owns: play stats, favorites, playlist membership, the queue, and cached
 * chroma colors. Reconciliation is by `uri`, never by MediaStore `_ID` — the
 * OS reissues IDs across rescans (§7).
 */
suspend fun ResonateDatabase.replaceLibrary(
    songs: List<SongEntity>,
    albums: List<AlbumEntity>,
    artists: List<ArtistEntity>,
    folders: List<FolderEntity>,
    genres: List<GenreEntity>,
) {
    withTransaction {
        val statBackup = playStatDao().backupByUri()
        val playlistBackup = playlistDao().songBackupByUri()
        val queueBackup = queueDao().backupByUri()
        val chromaBackup = libraryAggregateDao().chromaBackup().associateBy { it.id }

        songDao().deleteAll()
        songs.chunked(BATCH).forEach { songDao().insertAll(it) }

        libraryAggregateDao().apply {
            deleteAlbums()
            deleteArtists()
            deleteFolders()
            deleteGenres()
            insertAlbums(
                albums.map { album ->
                    chromaBackup[album.id]?.let {
                        album.copy(
                            chromaPrimary = it.chromaPrimary,
                            chromaSecondary = it.chromaSecondary,
                            chromaOnColor = it.chromaOnColor,
                        )
                    } ?: album
                }
            )
            insertArtists(artists)
            insertFolders(folders)
            insertGenres(genres)
        }

        val idByUri = HashMap<String, Long>(songs.size)
        for (song in songs) idByUri[song.uri] = song.id

        playStatDao().deleteAll()
        playStatDao().insertAll(
            statBackup.mapNotNull { stat ->
                idByUri[stat.uri]?.let {
                    PlayStatEntity(it, stat.playCount, stat.lastPlayedAt, stat.isFavorite)
                }
            }
        )

        playlistDao().deleteAllPlaylistSongs()
        playlistDao().insertPlaylistSongs(
            playlistBackup.mapNotNull { item ->
                idByUri[item.uri]?.let { PlaylistSongEntity(item.playlistId, it, item.position) }
            }
        )

        queueDao().deleteAll()
        queueDao().insertAll(
            queueBackup.mapNotNull { item ->
                idByUri[item.uri]?.let { QueueItemEntity(item.position, it) }
            }
        )
    }
}

private const val BATCH = 500
