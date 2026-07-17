package com.resonate.player.data.mediastore

import android.content.Context
import android.database.ContentObserver
import android.provider.MediaStore
import com.resonate.player.data.repo.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Incremental rescan trigger: MediaStore change → debounce → rescan.
 * Registered once the audio permission is granted.
 */
@OptIn(FlowPreview::class)
@Singleton
class LibraryWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LibraryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var started = false

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            changes.tryEmit(Unit)
        }
    }

    fun start() {
        if (started) return
        started = true
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        scope.launch {
            changes.debounce(DEBOUNCE_MS).collect {
                repository.rescan()
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 2_000L
    }
}
