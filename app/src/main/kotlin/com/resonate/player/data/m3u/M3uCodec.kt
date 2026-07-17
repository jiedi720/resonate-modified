package com.resonate.player.data.m3u

/**
 * §2.6: M3U8 in and out — an app you can leave is an app people trust.
 * Pure functions; no Android types.
 */
object M3uCodec {

    data class Entry(
        val path: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
    )

    fun write(entries: List<Entry>): String = buildString {
        append("#EXTM3U\n")
        for (entry in entries) {
            val seconds = (entry.durationMs / 1000).coerceAtLeast(0)
            append("#EXTINF:").append(seconds).append(',')
            if (entry.artist.isNotBlank()) append(entry.artist).append(" - ")
            append(entry.title).append('\n')
            append(entry.path).append('\n')
        }
    }

    /** Returns the file paths in order; comments and blanks skipped. */
    fun parse(text: String): List<String> =
        text.lineSequence()
            .map { it.trim().removePrefix("﻿") }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.replace('\\', '/') }
            .toList()

    /**
     * Matches parsed paths against the library: exact full path first, then
     * unique file name. Unmatched paths are dropped (they may live on another
     * device — half these files came from ten different places).
     */
    fun matchToLibrary(
        paths: List<String>,
        byFullPath: Map<String, Long>,
        byFileName: Map<String, List<Long>>,
    ): List<Long> = paths.mapNotNull { path ->
        byFullPath[path]
            ?: byFullPath[path.removePrefix("file://")]
            ?: byFileName[path.substringAfterLast('/')]?.singleOrNull()
    }
}
