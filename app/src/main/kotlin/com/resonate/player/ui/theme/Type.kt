package com.resonate.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.resonate.player.R

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Bricolage = FontFamily(
    variable(R.font.bricolage_grotesque, FontWeight.SemiBold),
    variable(R.font.bricolage_grotesque, FontWeight.Bold),
    variable(R.font.bricolage_grotesque, FontWeight.ExtraBold),
)

val Figtree = FontFamily(
    variable(R.font.figtree, FontWeight.Normal),
    variable(R.font.figtree, FontWeight.Medium),
    variable(R.font.figtree, FontWeight.SemiBold),
)

val JetBrainsMono = FontFamily(
    variable(R.font.jetbrains_mono, FontWeight.Normal),
    variable(R.font.jetbrains_mono, FontWeight.Medium),
)

/** §1.3 type scale. Semantic names; M3 slots are mapped from these. */
@Immutable
data class ResonateTypography(
    val displayLg: TextStyle = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.015).em,
    ),
    val displaySm: TextStyle = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.02.em,
    ),
)

val LocalResonateTypography = staticCompositionLocalOf { ResonateTypography() }

/** M3 slot mapping so Material components inherit the scale. */
fun ResonateTypography.toMaterial() = Typography(
    displayLarge = displayLg,
    displayMedium = displayLg,
    displaySmall = displaySm,
    headlineLarge = displaySm,
    headlineMedium = displaySm,
    headlineSmall = displaySm,
    titleLarge = title.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = title,
    titleSmall = label,
    bodyLarge = body.copy(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = label,
    labelSmall = caption,
)
