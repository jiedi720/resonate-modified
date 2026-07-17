package com.resonate.player.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonate.player.R
import com.resonate.player.ui.components.ArtworkPlaceholder
import com.resonate.player.ui.components.TrackRow
import com.resonate.player.ui.theme.ResonateTheme

// Static sample content; replaced by real data in step 3.
private val sampleCards = listOf(
    "Midnight Drive" to "Neon Halls",
    "Glass Bloom" to "Cascara",
    "Low Tide" to "Ferns",
    "Static Silk" to "Vantablack",
)

private val sampleTracks = listOf(
    Triple("Slow Motion Countdown", "Halogen Fields", "3:47"),
    Triple("Peach Static", "Modern Ruins", "2:58"),
    Triple("Terracotta", "Iso Wave", "4:12"),
)

@Composable
fun HomeScreen(onShuffleAll: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(key = "header", contentType = "header") {
            Text(
                text = stringResource(R.string.tab_home),
                style = ResonateTheme.type.displaySm,
                color = ResonateTheme.colors.bone,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
        item(key = "shuffle", contentType = "shuffle") {
            Button(
                onClick = onShuffleAll,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ResonateTheme.colors.accent,
                    contentColor = ResonateTheme.colors.onAccent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.home_shuffle_everything),
                    style = ResonateTheme.type.label,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        item(key = "jump-back-in", contentType = "section") {
            SectionHeader(stringResource(R.string.home_jump_back_in))
            CardRow()
        }
        item(key = "recently-added", contentType = "section") {
            SectionHeader(stringResource(R.string.home_recently_added))
            CardRow()
        }
        item(key = "most-played-header", contentType = "eyebrow") {
            SectionHeader(stringResource(R.string.home_most_played))
        }
        items(
            count = sampleTracks.size,
            key = { "most-played-$it" },
            contentType = { "track" },
        ) { index ->
            val (title, artist, duration) = sampleTracks[index]
            TrackRow(title = title, subtitle = artist, duration = duration, onClick = { })
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = ResonateTheme.type.caption,
        color = ResonateTheme.colors.muted,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun CardRow() {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = sampleCards.size,
            key = { it },
            contentType = { "card" },
        ) { index ->
            val (title, artist) = sampleCards[index]
            Column(modifier = Modifier.width(140.dp)) {
                ArtworkPlaceholder(
                    modifier = Modifier.size(140.dp),
                    cornerRadius = 12.dp,
                )
                Text(
                    text = title,
                    style = ResonateTheme.type.title,
                    color = ResonateTheme.colors.bone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = artist,
                    style = ResonateTheme.type.body,
                    color = ResonateTheme.colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
