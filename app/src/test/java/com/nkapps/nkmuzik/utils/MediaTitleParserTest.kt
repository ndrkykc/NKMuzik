package com.nkapps.nkmuzik.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTitleParserTest {

    @Test
    fun parse_streamTitleWithParenthesesPublisher_correctlySeparatesArtistAndTitleAndStripsPublisherTag() {
        val result = MediaTitleParser.parse("Engin Nurşani - Derbederim (Deka Müzik)", "Deka Müzik")
        assertEquals("Derbederim", result.title)
        assertEquals("Engin Nurşani", result.artist)
    }

    @Test
    fun parse_streamTitleWithOfficialVideoNoise_correctlySeparatesArtistAndTitle() {
        val result = MediaTitleParser.parse("Engin Nurşani - Derbederim (Official Music Video)", "Engin Nurşani - Topic")
        assertEquals("Derbederim", result.title)
        assertEquals("Engin Nurşani", result.artist)
    }

    @Test
    fun parse_channelNameWithoutTruncating_preservesFullChannelName() {
        val result = MediaTitleParser.parse("Derbederim (Deka Müzik)", "Deka Müzik")
        assertEquals("Derbederim", result.title)
        assertEquals("Deka Müzik", result.artist)
    }

    @Test
    fun parse_neverReturnsBilinmeyenSanatci() {
        val result1 = MediaTitleParser.parse("Derbederim", "Bilinmeyen Sanatçı")
        assertEquals("Derbederim", result1.title)
        assertEquals("Sanatçı", result1.artist)

        val result2 = MediaTitleParser.parse("Derbederim", "<unknown>")
        assertEquals("Derbederim", result2.title)
        assertEquals("Sanatçı", result2.artist)

        val result3 = MediaTitleParser.parse("Derbederim", "")
        assertEquals("Derbederim", result3.title)
        assertEquals("Sanatçı", result3.artist)
    }

    @Test
    fun parse_blankInputs_returnsDefaultValues() {
        val result = MediaTitleParser.parse("", "")
        assertEquals("Bilinmeyen Parça", result.title)
        assertEquals("Sanatçı", result.artist)
    }

    @Test
    fun parse_titleWithBrackets_correctlySeparatesAndStripsBrackets() {
        val result = MediaTitleParser.parse("Engin Nurşani - Derbederim [Deka Müzik]", "Deka Müzik")
        assertEquals("Derbederim", result.title)
        assertEquals("Engin Nurşani", result.artist)
    }

    @Test
    fun parse_variousSeparators_worksWithEnDashEmDashPipe() {
        val resultEnDash = MediaTitleParser.parse("Engin Nurşani – Derbederim (Deka Müzik)", "Deka Müzik")
        assertEquals("Derbederim", resultEnDash.title)
        assertEquals("Engin Nurşani", resultEnDash.artist)

        val resultPipe = MediaTitleParser.parse("Engin Nurşani | Derbederim (Deka Müzik)", "Deka Müzik")
        assertEquals("Derbederim", resultPipe.title)
        assertEquals("Engin Nurşani", resultPipe.artist)
    }
}
