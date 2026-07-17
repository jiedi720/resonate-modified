package com.resonate.player.data.m3u

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uCodecTest {

    @Test
    fun `write then parse roundtrips paths`() {
        val entries = listOf(
            M3uCodec.Entry("/storage/emulated/0/Music/a.mp3", "Song A", "Artist A", 183_000),
            M3uCodec.Entry("/storage/emulated/0/Downloads/b.flac", "Song B", "", 240_500),
        )
        val text = M3uCodec.write(entries)
        val parsed = M3uCodec.parse(text)
        assertEquals(entries.map { it.path }, parsed)
    }

    @Test
    fun `parse skips comments blanks and directives`() {
        val text = """
            #EXTM3U
            #EXTINF:180,Foo - Bar

            /music/one.mp3
            #PLAYLIST:test
            C:\Users\me\Music\two.mp3
        """.trimIndent()
        assertEquals(listOf("/music/one.mp3", "C:/Users/me/Music/two.mp3"), M3uCodec.parse(text))
    }

    @Test
    fun `matching prefers full path then unique filename`() {
        val byFullPath = mapOf("/music/one.mp3" to 1L)
        val byFileName = mapOf(
            "one.mp3" to listOf(1L),
            "two.mp3" to listOf(2L),
            "dupe.mp3" to listOf(3L, 4L),
        )
        val matched = M3uCodec.matchToLibrary(
            paths = listOf(
                "/music/one.mp3",       // full-path hit
                "/elsewhere/two.mp3",   // filename hit
                "/elsewhere/dupe.mp3",  // ambiguous — dropped
                "/gone/three.mp3",      // no hit — dropped
            ),
            byFullPath = byFullPath,
            byFileName = byFileName,
        )
        assertEquals(listOf(1L, 2L), matched)
    }

    @Test
    fun `extinf line carries artist and duration seconds`() {
        val text = M3uCodec.write(
            listOf(M3uCodec.Entry("/m/x.mp3", "Title", "Artist", 63_000))
        )
        val lines = text.lines()
        assertEquals("#EXTINF:63,Artist - Title", lines[1])
    }
}
