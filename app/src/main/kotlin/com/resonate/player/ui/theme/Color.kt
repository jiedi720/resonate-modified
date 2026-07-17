package com.resonate.player.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// §1.2 design tokens — dark is the origin, light is a port.
val Ink = Color(0xFF0D0B14)
val Surface = Color(0xFF171425)
val SurfaceRaised = Color(0xFF221D33)
val Hairline = Color(0xFF2E2842)
val Bone = Color(0xFFEFEAF6)
val Muted = Color(0xFF9A93AD)
val Pulse = Color(0xFFFF5C6E)
val Mint = Color(0xFF5BE9C8)

val InkLight = Color(0xFFF6F4FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceRaisedLight = Color(0xFFEDEAF4)
val HairlineLight = Color(0xFFDCD7E6)
val BoneLight = Color(0xFF16121F)
val MutedLight = Color(0xFF635C74)

/**
 * The app's color slots. `accent`/`onAccent` default to [pulse]-on-ink and are
 * the slots the chroma engine overrides when artwork colors are available.
 */
@Immutable
data class ResonateColors(
    val ink: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val hairline: Color,
    val bone: Color,
    val muted: Color,
    val pulse: Color,
    val mint: Color,
    val accent: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val DarkResonateColors = ResonateColors(
    ink = Ink,
    surface = Surface,
    surfaceRaised = SurfaceRaised,
    hairline = Hairline,
    bone = Bone,
    muted = Muted,
    pulse = Pulse,
    mint = Mint,
    accent = Pulse,
    onAccent = Ink,
    isDark = true,
)

val LightResonateColors = ResonateColors(
    ink = InkLight,
    surface = SurfaceLight,
    surfaceRaised = SurfaceRaisedLight,
    hairline = HairlineLight,
    bone = BoneLight,
    muted = MutedLight,
    pulse = Pulse,
    mint = Mint,
    accent = Pulse,
    onAccent = InkLight,
    isDark = false,
)

val LocalResonateColors = staticCompositionLocalOf { DarkResonateColors }
