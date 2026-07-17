package com.resonate.player.data.prefs

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class SortField { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION, PLAY_COUNT, NAME, SONG_COUNT, YEAR }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class SortPref(val field: SortField = SortField.TITLE, val ascending: Boolean = true)

@Serializable
data class UserPrefs(
    val songSort: SortPref = SortPref(SortField.TITLE, true),
    val albumSort: SortPref = SortPref(SortField.NAME, true),
    val artistSort: SortPref = SortPref(SortField.NAME, true),
    val genreSort: SortPref = SortPref(SortField.NAME, true),
    val lastLibraryTab: Int = 0,
    val chromaEnabled: Boolean = true,
    /** §3 v1 #12: resume state across app death, together with queue_items. */
    val queueIndex: Int = 0,
    val queuePositionMs: Long = 0,
    val recentSearches: List<String> = emptyList(),
    // §2.7 Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val materialYou: Boolean = false,
    // §2.7 Library
    val minDurationSec: Int = 30,
    // §2.4 speed sheet
    val playbackSpeed: Float = 1f,
    val pitchCorrection: Boolean = true,
    // §2.7 Equalizer
    val eqEnabled: Boolean = false,
    val eqPreset: Int = -1,
    val eqBandLevels: List<Int> = emptyList(),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
)

private object UserPrefsSerializer : Serializer<UserPrefs> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: UserPrefs = UserPrefs()

    override suspend fun readFrom(input: InputStream): UserPrefs =
        try {
            json.decodeFromString(UserPrefs.serializer(), input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Corrupt prefs", e)
        }

    override suspend fun writeTo(t: UserPrefs, output: OutputStream) {
        output.write(json.encodeToString(UserPrefs.serializer(), t).encodeToByteArray())
    }
}

@Singleton
class UserPrefsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store: DataStore<UserPrefs> = DataStoreFactory.create(
        serializer = UserPrefsSerializer,
        corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler { UserPrefs() },
        produceFile = { context.dataStoreFile("user_prefs.json") },
    )

    val prefs: Flow<UserPrefs> = store.data

    suspend fun update(transform: (UserPrefs) -> UserPrefs) {
        store.updateData(transform)
    }
}
