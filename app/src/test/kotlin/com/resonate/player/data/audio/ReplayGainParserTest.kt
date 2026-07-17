package com.resonate.player.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayGainParserTest {

    @Test
    fun `standard formats parse`() {
        assertEquals(-6.54f, ReplayGainParser.parseGainDb("-6.54 dB"))
        assertEquals(3.2f, ReplayGainParser.parseGainDb("+3.2 dB"))
        assertEquals(0f, ReplayGainParser.parseGainDb("0.00 dB"))
        assertEquals(-2.5f, ReplayGainParser.parseGainDb("-2.5dB"))
    }

    @Test
    fun `unicode minus and comma decimals parse`() {
        assertEquals(-4.5f, ReplayGainParser.parseGainDb("−4.5 dB"))
        assertEquals(-4.5f, ReplayGainParser.parseGainDb("-4,5 dB"))
    }

    @Test
    fun `garbage and absurd values are rejected`() {
        assertNull(ReplayGainParser.parseGainDb(null))
        assertNull(ReplayGainParser.parseGainDb(""))
        assertNull(ReplayGainParser.parseGainDb("loud"))
        assertNull(ReplayGainParser.parseGainDb("-99 dB"))
        assertNull(ReplayGainParser.parseGainDb("120 dB"))
    }
}
