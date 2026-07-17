package com.resonate.player.ui.theme

import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * §1.1 guardrail: no artwork color may produce an illegible accent. WCAG:
 * >= 3:1 for UI components against the background, >= 4.5:1 for text on the
 * accent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChromaClampTest {

    private val ink = 0xFF0D0B14.toInt()
    private val bone = 0xFFEFEAF6.toInt()

    private fun assertClampHolds(raw: Int) {
        val (accent, onAccent) = ChromaEngine.clampForTheme(
            raw = raw,
            isDark = true,
            background = ink,
            onDarkText = ink,
            onLightText = bone,
        )
        assertTrue(
            "accent vs background too low for #${Integer.toHexString(raw)}",
            ColorUtils.calculateContrast(accent, ink) >= 3.0,
        )
        assertTrue(
            "onAccent vs accent too low for #${Integer.toHexString(raw)}",
            ColorUtils.calculateContrast(onAccent, accent) >= 4.5,
        )
    }

    @Test
    fun `near-black artwork is clamped to visibility`() = assertClampHolds(0xFF0A0A0A.toInt())

    @Test
    fun `near-white artwork keeps text contrast`() = assertClampHolds(0xFFFAFAFA.toInt())

    @Test
    fun `mid grey artwork passes both gates`() = assertClampHolds(0xFF777777.toInt())

    @Test
    fun `saturated primaries pass both gates`() {
        listOf(0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF)
            .forEach { assertClampHolds(it.toInt()) }
    }

    @Test
    fun `dark navy like ink itself is pushed apart`() = assertClampHolds(0xFF0D0B14.toInt())
}
