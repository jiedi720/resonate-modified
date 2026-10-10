package com.resonate.player.data.repo

import android.os.SystemClock
import android.util.Log
import com.resonate.player.data.db.SongDao
import com.resonate.player.data.mediastore.MediaStoreScanner
import com.resonate.player.data.prefs.UserPrefsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val found: Int) : ScanState
    data class Done(val total: Int, val tookMs: Long) : ScanState
    data class Failed(val message: String?) : ScanState
}

@Singleton
class LibraryRepository @Inject constructor(
    private val scanner: MediaStoreScanner,
    private val songDao: SongDao,
    private val prefsStore: UserPrefsStore,
) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val songCount: Flow<Int> = songDao.countFlow()

    private val scanMutex = Mutex()

    suspend fun scanIfEmpty() {
        // Never silently index the whole device on first launch. The user must
        // explicitly choose a folder before the first library scan.
        val prefs = prefsStore.prefs.first()
        if (prefs.learningFolderTreeUri != null && songDao.count() == 0) rescan()
    }

    suspend fun rescan() {
        scanMutex.withLock {
            _scanState.value = ScanState.Scanning(0)
            val started = SystemClock.elapsedRealtime()
            try {
                val prefs = prefsStore.prefs.first()
                val total = scanner.fullScan(
                    minDurationSec = prefs.minDurationSec,
                    excludedFolders = prefs.excludedFolders,
                    learningFolderTreeUri = prefs.learningFolderTreeUri,
                ) { found ->
                    _scanState.value = ScanState.Scanning(found)
                }
                val took = SystemClock.elapsedRealtime() - started
                // §8 step 2 definition of done: prove the scan budget with a log.
                Log.i(TAG, "Library scan: $total tracks in ${took}ms")
                _scanState.value = ScanState.Done(total, took)
            } catch (e: SecurityException) {
                Log.w(TAG, "Scan failed: permission revoked", e)
                _scanState.value = ScanState.Failed(e.message)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Scan failed: invalid learning folder selection", e)
                _scanState.value = ScanState.Failed(e.message)
            }
        }
    }

    companion object {
        private const val TAG = "LibraryRepository"
    }
}
