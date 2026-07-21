package com.resonate.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Content max widths for large screens. A phone-first UI stretches badly on a
 * tablet (giant artwork, over-long text lines, sparse lists); capping content
 * to a comfortable column and centering it keeps every screen legible from a
 * 360dp phone to a 1200dp tablet in either orientation.
 */
object Responsive {
    /** Lists, settings, search — a readable single column. */
    val ListMaxWidth: Dp = 640.dp

    /** Now Playing artwork + transport — kept reachable, not wall-sized. */
    val PlayerMaxWidth: Dp = 460.dp
}

/**
 * Centers a screen's content in a max-width column on large displays while
 * staying edge-to-edge on phones. The wrapper fills the screen (so scroll and
 * fast-scroll rails still reach the full height); only width is constrained.
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = Responsive.ListMaxWidth,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            content()
        }
    }
}
