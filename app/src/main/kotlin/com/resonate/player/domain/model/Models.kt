package com.resonate.player.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val folderId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val isSupported: Boolean,
    val artworkUri: String?,
    val fileName: String,
    val dateAddedSec: Long = 0L,
    val dateModifiedSec: Long = 0L,
)

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val artistId: Long,
    val artistName: String,
    val year: Int,
    val songCount: Int,
    val artworkUri: String?,
)

@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
)

@Immutable
data class Folder(
    val id: Long,
    val path: String,
    val name: String,
    val songCount: Int,
)

@Immutable
data class Genre(
    val id: Long,
    val name: String,
    val songCount: Int,
)

/** m:ss / h:mm:ss for the mono timecode slots. */
fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
