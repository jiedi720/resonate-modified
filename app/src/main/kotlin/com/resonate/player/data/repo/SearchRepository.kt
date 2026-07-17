package com.resonate.player.data.repo

import com.resonate.player.data.db.AlbumBrowseDao
import com.resonate.player.data.db.ArtistBrowseDao
import com.resonate.player.data.db.FolderBrowseDao
import com.resonate.player.data.db.SongBrowseDao
import com.resonate.player.data.db.SongSearchRow
import com.resonate.player.domain.model.Album
import com.resonate.player.domain.model.Artist
import com.resonate.player.domain.model.Folder
import com.resonate.player.domain.model.Song
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * §2.3: instant, local, fuzzy, diacritic-insensitive. The index is a compact
 * normalized-string projection held only while the search screen collects it
 * (WhileSubscribed upstream) — the full library never sits in memory idle.
 */
@Singleton
class SearchRepository @Inject constructor(
    songBrowseDao: SongBrowseDao,
    albumBrowseDao: AlbumBrowseDao,
    artistBrowseDao: ArtistBrowseDao,
    folderBrowseDao: FolderBrowseDao,
) {

    class Index(
        val songs: List<Pair<SongSearchRow, String>>,
        val albums: List<Pair<Album, String>>,
        val artists: List<Pair<Artist, String>>,
        val folders: List<Pair<Folder, String>>,
    )

    data class Results(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val folders: List<Folder>,
    ) {
        val isEmpty: Boolean
            get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && folders.isEmpty()
    }

    val index: Flow<Index> = combine(
        songBrowseDao.songSearchRows(),
        albumBrowseDao.allAlbums(),
        artistBrowseDao.allArtists(),
        folderBrowseDao.allFolders(),
    ) { songs, albums, artists, folders ->
        Index(
            songs = songs.map { it to normalize("${it.title} ${it.artistName} ${it.albumName} ${it.fileName}") },
            albums = albums.map { it.toDomain() to normalize("${it.name} ${it.artistName}") },
            artists = artists.map { it.toDomain() to normalize(it.name) },
            folders = folders.map { it.toDomain() to normalize("${it.name} ${it.path}") },
        )
    }

    fun search(index: Index, rawQuery: String): Results {
        val query = normalize(rawQuery.trim())
        if (query.isEmpty()) return Results(emptyList(), emptyList(), emptyList(), emptyList())
        val tokens = query.split(WHITESPACE).filter { it.isNotEmpty() }

        fun matches(haystack: String): Boolean = matchesTokens(haystack, tokens)

        return Results(
            songs = index.songs.asSequence()
                .filter { matches(it.second) }
                .take(MAX_SONGS)
                .map { it.first.toSong() }
                .toList(),
            albums = index.albums.asSequence()
                .filter { matches(it.second) }
                .take(MAX_GROUP)
                .map { it.first }
                .toList(),
            artists = index.artists.asSequence()
                .filter { matches(it.second) }
                .take(MAX_GROUP)
                .map { it.first }
                .toList(),
            folders = index.folders.asSequence()
                .filter { matches(it.second) }
                .take(MAX_GROUP)
                .map { it.first }
                .toList(),
        )
    }

    companion object {
        private const val MAX_SONGS = 50
        private const val MAX_GROUP = 20
        private val WHITESPACE = Regex("\\s+")
        private val COMBINING_MARKS = Regex("\\p{Mn}+")

        /** Lowercase + strip diacritics: "Beyoncé" and "beyonce" meet in the middle. */
        fun normalize(text: String): String =
            COMBINING_MARKS.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")

        /**
         * Fuzzy: every query token must hit a word-prefix, or failing that,
         * appear as an in-order subsequence ("bynce" still finds beyonce).
         */
        fun matchesTokens(haystack: String, tokens: List<String>): Boolean = tokens.all { token ->
            haystack.contains(token) ||
                haystack.split(' ').any { it.startsWith(token) } ||
                isSubsequence(token, haystack)
        }

        private fun isSubsequence(needle: String, haystack: String): Boolean {
            if (needle.length < 3) return false
            var i = 0
            for (ch in haystack) {
                if (i < needle.length && ch == needle[i]) i++
                if (i == needle.length) return true
            }
            return false
        }
    }
}

private fun SongSearchRow.toSong() = Song(
    id = id,
    uri = uri,
    title = title,
    artist = artistName,
    album = albumName,
    albumId = albumId,
    artistId = artistId,
    folderId = folderId,
    durationMs = durationMs,
    trackNumber = 0,
    discNumber = 0,
    year = 0,
    isSupported = isSupported,
    artworkUri = artworkUriFor(albumId),
    fileName = fileName,
)
