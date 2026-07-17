package com.resonate.player.data.lyrics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class LyricsLine(
    val timeMs: Long?,
    val text: String,
)

@Immutable
data class Lyrics(
    val synced: Boolean,
    val lines: ImmutableList<LyricsLine>,
)

/**
 * Parses LRC-format text: `[mm:ss.xx]line`, multiple timestamps per line
 * allowed, metadata tags (`[ar:…]`, `[ti:…]`) skipped. Text without any
 * timestamps becomes unsynced lyrics. Pure Kotlin — unit-testable.
 */
object LrcParser {

    private val TIMESTAMP = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
    private val METADATA_TAG = Regex("^\\[[a-zA-Z#]+:.*]$")

    fun parse(text: String): Lyrics? {
        if (text.isBlank()) return null

        val synced = ArrayList<LyricsLine>()
        val unsynced = ArrayList<LyricsLine>()

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim().removePrefix("﻿")
            if (line.isEmpty()) continue
            if (METADATA_TAG.matches(line)) continue

            val stamps = TIMESTAMP.findAll(line).toList()
            if (stamps.isEmpty()) {
                unsynced += LyricsLine(timeMs = null, text = line)
            } else {
                val content = line.substring(stamps.last().range.last + 1).trim()
                for (stamp in stamps) {
                    val minutes = stamp.groupValues[1].toLong()
                    val seconds = stamp.groupValues[2].toLong()
                    val fractionRaw = stamp.groupValues[3]
                    val fractionMs = when (fractionRaw.length) {
                        0 -> 0L
                        1 -> fractionRaw.toLong() * 100
                        2 -> fractionRaw.toLong() * 10
                        else -> fractionRaw.take(3).toLong()
                    }
                    synced += LyricsLine(
                        timeMs = minutes * 60_000 + seconds * 1_000 + fractionMs,
                        text = content,
                    )
                }
            }
        }

        return when {
            synced.isNotEmpty() -> Lyrics(
                synced = true,
                lines = synced.sortedBy { it.timeMs }.toImmutableList(),
            )
            unsynced.isNotEmpty() -> Lyrics(synced = false, lines = unsynced.toImmutableList())
            else -> null
        }
    }

    /** Index of the line currently being sung, or -1 before the first line. */
    fun activeIndex(lyrics: Lyrics, positionMs: Long): Int {
        if (!lyrics.synced) return -1
        var active = -1
        for ((index, line) in lyrics.lines.withIndex()) {
            val time = line.timeMs ?: continue
            if (time <= positionMs) active = index else break
        }
        return active
    }
}
