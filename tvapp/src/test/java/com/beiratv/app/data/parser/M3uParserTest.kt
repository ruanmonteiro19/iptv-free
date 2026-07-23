package com.beiratv.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesMetadataAndQualityWithoutDependingOnUi() {
        val content = listOf(
            "#EXTM3U",
            "#EXTINF:-1 tvg-id=\"caze.br\" tvg-name=\"CazéTV\" tvg-logo=\"https://example/logo.png\" group-title=\"Sports\",CazéTV 1080p",
            "https://example/live.m3u8"
        ).joinToString("\n")

        val rows = M3uParser.parse(content, "test-source")
        assertEquals(1, rows.size)
        assertEquals("caze.br", rows.first().tvgId)
        assertEquals("Sports", rows.first().groupTitle)
        assertEquals(1080, rows.first().qualityHint)
    }
}
