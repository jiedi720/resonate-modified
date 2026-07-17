package com.resonate.player.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.resonate.player.data.db.AlbumBrowseDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Raw colors extracted from artwork; clamped per-theme at apply time. */
@Immutable
data class ChromaColors(
    val primary: Int,
    val secondary: Int,
)

/**
 * §1.1 chroma bleed. Extraction runs once per album on a 64×64 downscale
 * (§6.4) and is cached in Room, surviving rescans via [LibraryReplacer].
 */
@Singleton
class ChromaEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumBrowseDao: AlbumBrowseDao,
) {

    suspend fun chromaForAlbum(albumId: Long, artworkUri: String?): ChromaColors? {
        albumBrowseDao.chromaFor(albumId)?.let { cached ->
            val primary = cached.chromaPrimary ?: return@let
            return ChromaColors(primary, cached.chromaSecondary ?: primary)
        }
        if (artworkUri == null) return null

        val extracted = withContext(Dispatchers.IO) {
            decodeSmall(artworkUri)?.let { bitmap ->
                try {
                    extract(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
        } ?: return null

        albumBrowseDao.setChroma(albumId, extracted.primary, extracted.secondary, 0)
        return extracted
    }

    /** §6.4: decode at ~64px — palette quality doesn't need more. */
    private fun decodeSmall(uriString: String): Bitmap? = try {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) {
            null
        } else {
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= TARGET_SIZE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: Exception) {
        null
    }

    private fun extract(bitmap: Bitmap): ChromaColors? {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val primary = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: return null
        val secondary = palette.mutedSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: primary
        return ChromaColors(primary, secondary)
    }

    companion object {
        private const val TARGET_SIZE = 64

        /**
         * §1.1 guardrail: clamp for legibility before use — ugly artwork must
         * not break the UI. Returns accent + onAccent for the given theme.
         */
        fun clampForTheme(raw: Int, isDark: Boolean, background: Int, onDarkText: Int, onLightText: Int): Pair<Int, Int> {
            var accent = raw
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(accent, hsl)

            // The accent sits on the app background (seek track, active tab):
            // push lightness until it separates at >= 3:1 (WCAG for UI parts).
            var guard = 0
            while (ColorUtils.calculateContrast(accent, background) < 3.0 && guard < 20) {
                hsl[2] = if (isDark) (hsl[2] + 0.05f).coerceAtMost(0.95f)
                else (hsl[2] - 0.05f).coerceAtLeast(0.05f)
                accent = ColorUtils.HSLToColor(hsl)
                guard++
            }

            // Text/icons on the accent (play button): >= 4.5:1 (WCAG AA).
            val onAccent = when {
                ColorUtils.calculateContrast(onDarkText, accent) >= 4.5 -> onDarkText
                ColorUtils.calculateContrast(onLightText, accent) >= 4.5 -> onLightText
                else -> {
                    // Force the accent light enough that dark text passes.
                    var adjusted = accent
                    val h = FloatArray(3)
                    ColorUtils.colorToHSL(adjusted, h)
                    var g = 0
                    while (ColorUtils.calculateContrast(onDarkText, adjusted) < 4.5 && g < 20) {
                        h[2] = (h[2] + 0.05f).coerceAtMost(0.97f)
                        adjusted = ColorUtils.HSLToColor(h)
                        g++
                    }
                    accent = adjusted
                    onDarkText
                }
            }
            return accent to onAccent
        }
    }
}
