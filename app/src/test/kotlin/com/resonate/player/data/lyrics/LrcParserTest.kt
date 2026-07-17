package com.resonate.player.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `timestamped lines become synced lyrics sorted by time`() {
        val lyrics = LrcParser.parse(
            """
            [ti:Song]
            [00:12.50]Second line
            [00:01.00]First line
            """.trimIndent()
        )
        assertTrue(lyrics!!.synced)
        assertEquals(listOf("First line", "Second line"), lyrics.lines.map { it.text })
        assertEquals(1_000L, lyrics.lines[0].timeMs)
        assertEquals(12_500L, lyrics.lines[1].timeMs)
    }

    @Test
    fun `multiple timestamps on one line repeat the text`() {
        val lyrics = LrcParser.parse("[00:05.00][01:05.00]Chorus")
        assertEquals(2, lyrics!!.lines.size)
        assertEquals(5_000L, lyrics.lines[0].timeMs)
        assertEquals(65_000L, lyrics.lines[1].timeMs)
        assertTrue(lyrics.lines.all { it.text == "Chorus" })
    }

    @Test
    fun `plain text becomes unsynced lyrics`() {
        val lyrics = LrcParser.parse("Just some lyrics\nwithout timestamps")
        assertFalse(lyrics!!.synced)
        assertEquals(2, lyrics.lines.size)
        assertNull(lyrics.lines[0].timeMs)
    }

    @Test
    fun `metadata tags and blanks are skipped`() {
        val lyrics = LrcParser.parse("[ar:Artist]\n[al:Album]\n\n[00:01.00]Real line")
        assertEquals(1, lyrics!!.lines.size)
        assertEquals("Real line", lyrics.lines[0].text)
    }

    @Test
    fun `blank or tag-only text yields null`() {
        assertNull(LrcParser.parse("   "))
        assertNull(LrcParser.parse("[ar:Someone]\n[ti:Something]"))
    }

    @Test
    fun `fraction digits normalize to milliseconds`() {
        val lyrics = LrcParser.parse("[00:01.5]A\n[00:02.75]B\n[00:03.123]C\n[00:04]D")
        assertEquals(1_500L, lyrics!!.lines[0].timeMs)
        assertEquals(2_750L, lyrics.lines[1].timeMs)
        assertEquals(3_123L, lyrics.lines[2].timeMs)
        assertEquals(4_000L, lyrics.lines[3].timeMs)
    }

    @Test
    fun `active index follows position`() {
        val lyrics = LrcParser.parse("[00:01.00]A\n[00:05.00]B\n[00:10.00]C")!!
        assertEquals(-1, LrcParser.activeIndex(lyrics, 500))
        assertEquals(0, LrcParser.activeIndex(lyrics, 1_000))
        assertEquals(1, LrcParser.activeIndex(lyrics, 7_000))
        assertEquals(2, LrcParser.activeIndex(lyrics, 60_000))
    }
}
