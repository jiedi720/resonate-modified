package com.resonate.player.ui.widget

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.resonate.player.MainActivity
import com.resonate.player.R
import com.resonate.player.playback.PlaybackService

class ResonateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResonateWidget()
}

/** §2.8: 4×2 home-screen widget — artwork + transport, one session behind it. */
class ResonateWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[KEY_TITLE] ?: context.getString(R.string.app_name)
            val artist = prefs[KEY_ARTIST].orEmpty()
            val playing = prefs[KEY_PLAYING] ?: false
            val artworkUri = prefs[KEY_ARTWORK]

            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Ink))
                    .cornerRadius(16.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (artworkUri != null) {
                    Image(
                        provider = ImageProvider(
                            Icon.createWithContentUri(Uri.parse(artworkUri))
                        ),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .size(88.dp)
                            .cornerRadius(12.dp),
                    )
                    Spacer(modifier = GlanceModifier.width(12.dp))
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(Bone),
                            fontSize = 15.sp,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = artist,
                        style = TextStyle(
                            color = ColorProvider(Muted),
                            fontSize = 13.sp,
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WidgetButton(
                            context = context,
                            iconRes = R.drawable.ic_widget_prev,
                            contentDescription = context.getString(R.string.cd_previous),
                            action = PlaybackService.ACTION_WIDGET_PREV,
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetButton(
                            context = context,
                            iconRes = if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                            contentDescription = context.getString(
                                if (playing) R.string.cd_pause else R.string.cd_play
                            ),
                            action = PlaybackService.ACTION_WIDGET_PLAY_PAUSE,
                            accent = true,
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetButton(
                            context = context,
                            iconRes = R.drawable.ic_widget_next,
                            contentDescription = context.getString(R.string.cd_next),
                            action = PlaybackService.ACTION_WIDGET_NEXT,
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetButton(
        context: Context,
        iconRes: Int,
        contentDescription: String,
        action: String,
        accent: Boolean = false,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier
                .size(40.dp)
                .cornerRadius(20.dp)
                .background(ColorProvider(if (accent) Pulse else SurfaceRaised))
                .padding(8.dp)
                .clickable(
                    androidx.glance.appwidget.action.actionStartService(
                        Intent(context, PlaybackService::class.java).setAction(action),
                        isForegroundService = true,
                    )
                ),
        )
    }

    companion object {
        val KEY_TITLE = stringPreferencesKey("title")
        val KEY_ARTIST = stringPreferencesKey("artist")
        val KEY_PLAYING = booleanPreferencesKey("playing")
        val KEY_ARTWORK = stringPreferencesKey("artworkUri")

        private val Ink = Color(0xFF0D0B14)
        private val SurfaceRaised = Color(0xFF221D33)
        private val Bone = Color(0xFFEFEAF6)
        private val Muted = Color(0xFF9A93AD)
        private val Pulse = Color(0xFFFF5C6E)
    }
}
