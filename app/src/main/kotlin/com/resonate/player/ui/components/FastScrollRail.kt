package com.resonate.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.resonate.player.data.db.LetterBucket
import com.resonate.player.ui.theme.ResonateTheme
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class LetterIndexEntry(val letter: String, val firstItemIndex: Int)

/**
 * Collapses raw first-letter buckets into A–Z plus a '#' bucket and computes
 * the first list index of each letter for a given sort direction.
 */
fun buildLetterIndex(buckets: List<LetterBucket>, ascending: Boolean): List<LetterIndexEntry> {
    val merged = LinkedHashMap<String, Int>()
    val ordered = if (ascending) buckets else buckets.asReversed()
    for (bucket in ordered) {
        val key = bucket.letter.firstOrNull()?.uppercaseChar()?.let {
            if (it in 'A'..'Z') it.toString() else "#"
        } ?: "#"
        merged[key] = (merged[key] ?: 0) + bucket.count
    }
    var index = 0
    return merged.map { (letter, count) ->
        LetterIndexEntry(letter, index).also { index += count }
    }
}

/** §2.2: fast-scroll rail with a letter bubble on Songs/Albums/Artists. */
@Composable
fun FastScrollRail(
    entries: ImmutableList<LetterIndexEntry>,
    onJump: (itemIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.size < 2) return
    val haptics = LocalHapticFeedback.current
    var railHeightPx by remember { mutableStateOf(0) }
    var activeLetter by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
        activeLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .padding(end = 40.dp)
                    .size(48.dp)
                    .background(ResonateTheme.colors.surfaceRaised, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    style = ResonateTheme.type.displaySm,
                    color = ResonateTheme.colors.bone,
                )
            }
        }
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .onSizeChanged { railHeightPx = it.height }
                .pointerInput(entries) {
                    fun handle(y: Float) {
                        if (railHeightPx <= 0) return
                        val fraction = (y / railHeightPx).coerceIn(0f, 0.999f)
                        val entry = entries[(fraction * entries.size).toInt()]
                        if (entry.letter != activeLetter) {
                            activeLetter = entry.letter
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onJump(entry.firstItemIndex)
                        }
                    }
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val change = down.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                handle(change.position.y)
                            } else {
                                activeLetter = null
                            }
                        }
                    }
                },
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            entries.forEach { entry ->
                Text(
                    text = entry.letter,
                    style = ResonateTheme.type.caption,
                    color = if (entry.letter == activeLetter) ResonateTheme.colors.accent
                    else ResonateTheme.colors.muted,
                )
            }
        }
    }
}
