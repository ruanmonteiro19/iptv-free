package com.beiratv.app.data.normalization

import com.beiratv.app.data.local.ChannelSourceEntity
import com.beiratv.app.data.parser.ParsedChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelNormalizerTest {
    @Test
    fun qualitySuffixesDoNotCreateDifferentNames() {
        assertEquals("Canal X", ChannelNameNormalizer.displayName("BR | Canal X FHD"))
        assertEquals("canal x", ChannelNameNormalizer.normalized("Canal X 1080p"))
        assertEquals("canal x", ChannelNameNormalizer.normalized("Canal X HD"))
    }

    @Test
    fun accentFoldingSupportsBrazilianSearch() {
        assertEquals("belem", TextNormalizer.fold("Belém"))
        assertEquals("cazetv", TextNormalizer.fold("CazéTV"))
    }

    @Test
    fun sameChannelFromMultipleSourcesBecomesOneChannelWithBackups() {
        val sources = mapOf(
            "a" to ChannelSourceEntity("a", "A", "https://a", 90),
            "b" to ChannelSourceEntity("b", "B", "https://b", 80)
        )
        val rows = listOf(
            ParsedChannel("a", "Canal X FHD", null, "https://a/x.m3u8", "Variedades", "canal.x", null, 1080),
            ParsedChannel("b", "Canal X 720p", null, "https://b/x.m3u8", "Variedades", "canal.x", null, 720)
        )
        val result = ChannelConsolidator.consolidate(rows, sources, emptyMap(), emptyMap())
        assertEquals(1, result.size)
        assertEquals(2, result.first().streams.size)
    }

    @Test
    fun regionalsRemainDistinct() {
        val sources = mapOf(
            "sp" to ChannelSourceEntity("sp", "SP", "https://sp", 80, region = "BR-SP"),
            "rj" to ChannelSourceEntity("rj", "RJ", "https://rj", 80, region = "BR-RJ")
        )
        val rows = listOf(
            ParsedChannel("sp", "Globo São Paulo", null, "https://sp/globo", "Abertos", "globo", null),
            ParsedChannel("rj", "Globo Rio de Janeiro", null, "https://rj/globo", "Abertos", "globo", null)
        )
        val result = ChannelConsolidator.consolidate(rows, sources, emptyMap(), emptyMap())
        assertEquals(2, result.size)
    }


    @Test
    fun sameRegionalNetworkInSameStateBecomesBackups() {
        val sources = mapOf(
            "sp1" to ChannelSourceEntity("sp1", "SP1", "https://sp1", 80, region = "BR-SP"),
            "sp2" to ChannelSourceEntity("sp2", "SP2", "https://sp2", 75, region = "BR-SP")
        )
        val rows = listOf(
            ParsedChannel("sp1", "Globo São Paulo", null, "https://sp1/globo", "Abertos", null, null),
            ParsedChannel("sp2", "TV Globo São Paulo HD", null, "https://sp2/globo", "Abertos", null, null)
        )
        val result = ChannelConsolidator.consolidate(rows, sources, emptyMap(), emptyMap())
        assertEquals(1, result.size)
        assertEquals(2, result.first().streams.size)
    }

    @Test
    fun premiumNamesAreNotAutoIncludedFromUnverifiedSources() {
        val source = ChannelSourceEntity("public", "Public", "https://example", 50, verifiedLicensed = false)
        assertFalse(PremiumChannelPolicy.isAllowed("SporTV Alternativo 1080p", source))
        assertFalse(PremiumChannelPolicy.isAllowed("Premiere Clubes", source))
        assertTrue(PremiumChannelPolicy.isAllowed("CazéTV", source))
    }

    @Test
    fun sportsCategoryIsPrioritized() {
        assertEquals("Esportes", CategoryClassifier.classify("Sports", "Red Bull TV", null))
        assertTrue(CategoryClassifier.order("Esportes") < CategoryClassifier.order("Variedades"))
    }
}
