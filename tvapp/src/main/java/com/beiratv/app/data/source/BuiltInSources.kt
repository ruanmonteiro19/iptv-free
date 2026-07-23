package com.beiratv.app.data.source

import com.beiratv.app.data.local.ChannelSourceEntity

/**
 * Built-in sources are intentionally hidden from the end-user UI.
 * verifiedLicensed must only be true when TekasTV has explicit redistribution rights.
 */
object BuiltInSources {
    const val LOGOS_URL = "https://raw.githubusercontent.com/ruanmonteiro19/iptv-free/refs/heads/main/logos.json"
    const val ALIASES_URL = "https://raw.githubusercontent.com/ruanmonteiro19/iptv-free/refs/heads/main/aliases.json"
    const val EPG_URL = "https://raw.githubusercontent.com/ruanmonteiro19/iptv-free/refs/heads/main/epg.xml"

    val channels: List<ChannelSourceEntity> = listOf(
        ChannelSourceEntity(
            id = "tekastv-main",
            name = "TekasTV Brasil",
            url = "https://raw.githubusercontent.com/ruanmonteiro19/iptv-free/refs/heads/main/brasil_maximo_publico.m3u",
            priority = 100,
            region = "BR",
            verifiedLicensed = false
        ),
        ChannelSourceEntity("iptv-org-br", "Brasil", "https://iptv-org.github.io/iptv/countries/br.m3u", 90, region = "BR"),
        ChannelSourceEntity("iptv-org-pa", "Pará", "https://iptv-org.github.io/iptv/subdivisions/br-pa.m3u", 82, region = "BR-PA"),
        ChannelSourceEntity("iptv-org-rj", "Rio de Janeiro", "https://iptv-org.github.io/iptv/subdivisions/br-rj.m3u", 82, region = "BR-RJ"),
        ChannelSourceEntity("iptv-org-sp", "São Paulo", "https://iptv-org.github.io/iptv/subdivisions/br-sp.m3u", 82, region = "BR-SP"),
        ChannelSourceEntity("iptv-org-rio", "Rio de Janeiro", "https://iptv-org.github.io/iptv/cities/brrio.m3u", 78, region = "BR-RJ-RIO"),
        ChannelSourceEntity("iptv-org-cfo", "Cabo Frio", "https://iptv-org.github.io/iptv/cities/brcfo.m3u", 76, region = "BR-RJ-CABO-FRIO"),
        ChannelSourceEntity("iptv-org-mrc", "Maricá", "https://iptv-org.github.io/iptv/cities/brmrc.m3u", 76, region = "BR-RJ-MARICA"),
        ChannelSourceEntity("iptv-org-nfu", "Nova Friburgo", "https://iptv-org.github.io/iptv/cities/brnfu.m3u", 76, region = "BR-RJ-NOVA-FRIBURGO"),
        ChannelSourceEntity("iptv-org-sao", "São Paulo", "https://iptv-org.github.io/iptv/cities/brsao.m3u", 78, region = "BR-SP-SAO-PAULO"),
        ChannelSourceEntity("iptv-org-gus", "Guarulhos", "https://iptv-org.github.io/iptv/cities/brgus.m3u", 76, region = "BR-SP-GUARULHOS"),
        ChannelSourceEntity("iptv-org-osa", "Osasco", "https://iptv-org.github.io/iptv/cities/brosa.m3u", 76, region = "BR-SP-OSASCO"),
        ChannelSourceEntity("iptv-org-iig", "Itapetininga", "https://iptv-org.github.io/iptv/cities/briig.m3u", 76, region = "BR-SP-ITAPETININGA"),
        ChannelSourceEntity("iptv-org-aru", "Araçatuba", "https://iptv-org.github.io/iptv/cities/braru.m3u", 76, region = "BR-SP-ARACATUBA"),
        ChannelSourceEntity("iptv-org-szo", "Sertãozinho", "https://iptv-org.github.io/iptv/cities/brszo.m3u", 76, region = "BR-SP-SERTAOZINHO"),
        ChannelSourceEntity("iptv-org-cas", "Castanhal", "https://iptv-org.github.io/iptv/cities/brcas.m3u", 80, region = "BR-PA-CASTANHAL")
    )
}
