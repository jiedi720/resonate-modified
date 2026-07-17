package com.resonate.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonate.player.R
import com.resonate.player.ui.theme.ResonateTheme

/** §1.4: 64dp row, 48dp artwork, mono duration. Unsupported rows dim (§4). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    duration: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null,
    supported: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(
                if (selected) {
                    Modifier.background(com.resonate.player.ui.theme.ResonateTheme.colors.surfaceRaised)
                } else {
                    Modifier
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp)
            .alpha(if (supported) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(
            uri = artworkUri,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = title,
                style = ResonateTheme.type.title,
                color = ResonateTheme.colors.bone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (supported) subtitle else stringResource(R.string.unsupported_format),
                style = ResonateTheme.type.body,
                color = ResonateTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = duration,
            style = ResonateTheme.type.mono,
            color = ResonateTheme.colors.muted,
        )
    }
}

/** Album detail variant: track number in the artwork slot, mono-aligned. */
@Composable
fun NumberedTrackRow(
    number: Int,
    title: String,
    subtitle: String,
    duration: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supported: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .alpha(if (supported) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(32.dp)) {
            Text(
                text = number.toString(),
                style = ResonateTheme.type.mono,
                color = ResonateTheme.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = title,
                style = ResonateTheme.type.title,
                color = ResonateTheme.colors.bone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (supported) subtitle else stringResource(R.string.unsupported_format),
                style = ResonateTheme.type.body,
                color = ResonateTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = duration,
            style = ResonateTheme.type.mono,
            color = ResonateTheme.colors.muted,
        )
    }
}

/** Paging placeholder row — fixed height so the scrollbar extent is stable. */
@Composable
fun TrackRowPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkPlaceholder(modifier = Modifier.size(48.dp))
    }
}
