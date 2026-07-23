package com.beiratv.app.data.normalization

import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.data.local.ChannelMetadataEntity
import com.beiratv.app.data.local.ChannelStreamEntity
import com.beiratv.app.data.local.ChannelSourceEntity
import com.beiratv.app.data.parser.ParsedChannel
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

data class RegionInfo(val state: String? = null, val city: String? = null) {
    val key: String? = when {
        state == null -> null
        city == null -> state.lowercase(Locale.ROOT)
        else -> "${state.lowercase(Locale.ROOT)}|${TextNormalizer.fold(city).replace(' ', '-')}"
    }
}

data class ConsolidatedChannel(
    val channel: ChannelEntity,
    val streams: List<ChannelStreamEntity>,
    val aliases: Set<String>,
    val metadata: ChannelMetadataEntity
)

object PremiumChannelPolicy {
    private val blockedWhenUnverified = listOf(
        "sportv", "premiere", "espn", "combate", "telecine", "hbo", "cinemax",
        "megapix", "warner channel", "cartoon network", "discovery channel",
        "discovery kids", "animal planet", "history channel", "a e", "a&e",
        "tnt", "space", "universal tv", "studio universal", "sony channel",
        "star channel", "fx", "paramount network", "nickelodeon", "disney channel",
        "national geographic", "nat geo", "food network", "tlc", "adult swim", "amc"
    )

    fun isAllowed(channelName: String, source: ChannelSourceEntity): Boolean {
        if (source.verifiedLicensed) return true
        val name = TextNormalizer.fold(channelName)
        return blockedWhenUnverified.none { TextNormalizer.fold(it) in name }
    }
}

object CategoryClassifier {
    private val ordered = listOf(
        "Esportes", "Abertos", "Notícias", "Filmes e Séries", "Regionais", "Infantil",
        "Documentários", "Variedades", "Música", "Religiosos", "Agro", "Públicos e Educação"
    )

    fun order(category: String): Int = ordered.indexOf(category).let { if (it < 0) 99 else it }

    fun classify(rawGroup: String, name: String, region: RegionInfo?): String {
        val text = TextNormalizer.fold("$rawGroup $name")
        if (region?.state != null && ("local" in text || "regional" in text || isRegionalNetwork(text))) return "Regionais"
        return when {
            any(text, "sport", "esporte", "futebol", "mma", "combat", "poker", "racer", "surf") -> "Esportes"
            any(text, "news", "noticia", "jornal") -> "Notícias"
            any(text, "movie", "filme", "series", "serie", "cinema") -> "Filmes e Séries"
            any(text, "kid", "infantil", "children", "desenho", "anime") -> "Infantil"
            any(text, "documentary", "documentario", "nature", "natureza", "science") -> "Documentários"
            any(text, "music", "musica", "radio") -> "Música"
            any(text, "relig", "gospel", "catolic", "evangel") -> "Religiosos"
            any(text, "agro", "rural", "campo", "fazenda") -> "Agro"
            any(text, "camara", "senado", "justica", "educa", "universidade", "publico", "tv brasil") -> "Públicos e Educação"
            isOpenNetwork(text) -> "Abertos"
            else -> "Variedades"
        }
    }

    private fun isOpenNetwork(text: String): Boolean = any(text,
        "globo", "record", "sbt", "band", "redetv", "rede tv", "gazeta", "tv cultura")

    private fun isRegionalNetwork(text: String): Boolean = any(text,
        "tv liberal", "tv bahia", "verdes mares", "tv mirante", "anhanguera", "centro america",
        "eptv", "tv tem", "rpc", "rbs tv", "nsc tv", "inter tv")

    private fun any(text: String, vararg terms: String): Boolean = terms.any { TextNormalizer.fold(it) in text }
}

object RegionDetector {
    private val stateNames = mapOf(
        "para" to "PA", "sao paulo" to "SP", "rio de janeiro" to "RJ", "minas gerais" to "MG",
        "bahia" to "BA", "parana" to "PR", "rio grande do sul" to "RS", "santa catarina" to "SC",
        "ceara" to "CE", "pernambuco" to "PE", "goias" to "GO", "mato grosso" to "MT",
        "mato grosso do sul" to "MS", "amazonas" to "AM", "maranhao" to "MA"
    )

    fun detect(name: String, sourceRegion: String?): RegionInfo {
        val regionParts = sourceRegion?.split('-').orEmpty()
        val sourceState = regionParts.getOrNull(1)?.takeIf { it.length == 2 }
        val sourceCity = regionParts.drop(2).takeIf { it.isNotEmpty() }?.joinToString(" ")?.replace('-', ' ')
        if (sourceState != null) return RegionInfo(sourceState, sourceCity)

        val folded = TextNormalizer.fold(name)
        val knownAffiliateState = when {
            "tv liberal" in folded -> "PA"
            "tv bahia" in folded -> "BA"
            "verdes mares" in folded -> "CE"
            "tv mirante" in folded -> "MA"
            "centro america" in folded -> "MT"
            Regex("""\brpc\b""").containsMatchIn(folded) -> "PR"
            "rbs tv" in folded -> "RS"
            "nsc tv" in folded -> "SC"
            "anhanguera" in folded -> "GO"
            else -> null
        }
        val detectedState = knownAffiliateState ?: stateNames.entries.firstOrNull { (stateName, _) -> stateName in folded }?.value
        return RegionInfo(detectedState, null)
    }
}

object ChannelNameNormalizer {
    private val qualityTokens = Regex("""(?i)\b(HD|FHD|FULL\s*HD|SD|UHD|4K|2160P|1080P|1080I|720P|576P|480P|360P|H264|H265|HEVC)\b""")
    private val prefixTokens = Regex("""(?i)^\s*(BRASIL|BRA|BR|TV\s*BR|LIVE)\s*([|:_-]+\s*)?""")

    fun displayName(raw: String): String = raw
        .replace(qualityTokens, " ")
        .replace(prefixTokens, "")
        .replace(Regex("\\s*[|:_-]+\\s*$"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { raw.trim() }

    fun normalized(raw: String): String = TextNormalizer.fold(displayName(raw))
}

object BuiltInAliasCatalog {
    val aliases: Map<String, String> = buildMap {
        fun add(canonical: String, vararg values: String) {
            val key = TextNormalizer.fold(canonical)
            put(key, key)
            values.forEach { put(TextNormalizer.fold(it), key) }
        }
        add("record news", "recordnews", "record news hd", "br record news")
        add("caze tv", "cazetv", "caze tv", "caze tv youtube")
        add("red bull tv", "redbull tv", "red bull television")
        add("tv liberal", "liberal belem", "tv liberal belem", "tv liberal belém")
        add("canal do inter", "inter tv futebol", "canal inter")
        add("n sports", "nsports", "n-sports")
        add("woohoo", "woohoo tv", "woohoo surf")
        add("fish tv", "fishtv")
        add("tv brasil", "tvbrasil", "tv brasil hd")
    }
}

object ChannelConsolidator {
    fun consolidate(
        rows: List<ParsedChannel>,
        sources: Map<String, ChannelSourceEntity>,
        logoCatalog: Map<String, String>,
        aliasCatalog: Map<String, String>
    ): List<ConsolidatedChannel> {
        val groups = linkedMapOf<String, MutableList<ParsedChannel>>()
        val effectiveAliases = BuiltInAliasCatalog.aliases + aliasCatalog

        for (row in rows) {
            val source = sources[row.sourceId] ?: continue
            if (!PremiumChannelPolicy.isAllowed(row.name, source)) continue
            val region = RegionDetector.detect(row.name, source.region)
            val normalizedName = ChannelNameNormalizer.normalized(row.name)
            val tvg = row.tvgId?.let(TextNormalizer::fold)?.takeIf { it.isNotBlank() && it != "none" }
            val aliasKey = effectiveAliases[normalizedName]
            val regional = region.key != null && isRegional(row.name, row.groupTitle, source.region)
            val networkKey = detectNetwork(row.name)?.let(TextNormalizer::fold)
            val networkRegionKey = if (regional && networkKey != null) "network:$networkKey|region:${region.key}" else null
            val identityBase = tvg?.let { "tvg:$it" }
                ?: aliasKey?.let { "alias:$it" }
                ?: networkRegionKey
                ?: "name:$normalizedName"
            val regionalSuffix = if (regional && networkRegionKey == null) "|region:${region.key}" else ""
            groups.getOrPut(identityBase + regionalSuffix) { mutableListOf() } += row
        }

        return groups.mapNotNull { (identityKey, duplicates) ->
            if (duplicates.isEmpty()) return@mapNotNull null
            val ranked = duplicates.sortedWith(compareByDescending<ParsedChannel> { score(it, sources[it.sourceId]) }
                .thenByDescending { it.qualityHint ?: 0 })
            val primary = ranked.first()
            val source = sources[primary.sourceId] ?: return@mapNotNull null
            val region = RegionDetector.detect(primary.name, source.region)
            val displayName = ChannelNameNormalizer.displayName(primary.name)
            val normalizedName = ChannelNameNormalizer.normalized(displayName)
            val canonicalId = stableId(identityKey)
            val category = CategoryClassifier.classify(primary.groupTitle, displayName, region)
            val logo = primary.logo?.takeIf { it.startsWith("http") }
                ?: logoCatalog[normalizedName]
                ?: effectiveAliases[normalizedName]?.let { logoCatalog[it] }
                ?: primary.tvgId?.let { logoCatalog[TextNormalizer.fold(it)] }
                ?: primary.tvgName?.let { logoCatalog[TextNormalizer.fold(it)] }
            val streams = ranked
                .distinctBy { it.streamUrl }
                .mapIndexed { index, row ->
                    ChannelStreamEntity(
                        id = stableId("${canonicalId}|${row.sourceId}|${row.streamUrl}"),
                        channelId = canonicalId,
                        sourceId = row.sourceId,
                        streamUrl = row.streamUrl,
                        priority = score(row, sources[row.sourceId]) - index,
                        quality = row.qualityHint,
                        enabled = true
                    )
                }
            val aliases = duplicates.flatMap { row ->
                listOf(row.name, row.tvgName, row.tvgId).filterNotNull().map(TextNormalizer::fold)
            }.toSet()
            val regional = region.state != null && isRegional(primary.name, primary.groupTitle, source.region)
            ConsolidatedChannel(
                channel = ChannelEntity(
                    id = canonicalId,
                    name = displayName,
                    logo = logo,
                    streamUrl = streams.first().streamUrl,
                    category = category,
                    tvgId = primary.tvgId?.takeIf { it.isNotBlank() },
                    isFavorite = false,
                    playlistId = "builtin"
                ),
                streams = streams,
                aliases = aliases,
                metadata = ChannelMetadataEntity(
                    channelId = canonicalId,
                    network = detectNetwork(displayName),
                    state = region.state,
                    city = region.city,
                    isRegional = regional
                )
            )
        }.sortedWith(compareBy<ConsolidatedChannel> { CategoryClassifier.order(it.channel.category) }
            .thenBy { TextNormalizer.fold(it.channel.name) })
    }

    private fun score(row: ParsedChannel, source: ChannelSourceEntity?): Int {
        var score = source?.priority ?: 0
        if (row.streamUrl.startsWith("https://", true)) score += 12
        score += when (row.qualityHint) {
            2160 -> 12
            1080 -> 10
            720 -> 8
            576, 540, 480 -> 5
            else -> 0
        }
        return score
    }

    private fun detectNetwork(name: String): String? {
        val text = TextNormalizer.fold(name)
        val networks = listOf("globo", "record", "sbt", "band", "redetv", "tv cultura", "tv liberal", "inter tv", "eptv", "rpc", "rbs tv", "nsc tv")
        return networks.firstOrNull { TextNormalizer.fold(it) in text }
    }

    private fun isRegional(name: String, group: String, sourceRegion: String?): Boolean {
        val folded = TextNormalizer.fold("$group $name")
        val explicitlyLocal = listOf("local", "regional", "afiliada").any { it in folded }
        val knownAffiliate = listOf("tv liberal", "tv bahia", "verdes mares", "tv mirante", "anhanguera", "centro america", "eptv", "tv tem", "rpc", "rbs tv", "nsc tv", "inter tv")
            .any { TextNormalizer.fold(it) in folded }
        val parts = sourceRegion?.split('-').orEmpty()
        val sourceState = parts.getOrNull(1)
        val sourceCity = parts.drop(2).joinToString(" ").takeIf { it.isNotBlank() }
        val stateNamesByCode = mapOf(
            "PA" to "para", "SP" to "sao paulo", "RJ" to "rio de janeiro", "MG" to "minas gerais",
            "BA" to "bahia", "PR" to "parana", "RS" to "rio grande do sul", "SC" to "santa catarina",
            "CE" to "ceara", "PE" to "pernambuco", "GO" to "goias", "MT" to "mato grosso",
            "MS" to "mato grosso do sul", "AM" to "amazonas", "MA" to "maranhao"
        )
        val mentionsSourceState = sourceState?.uppercase(Locale.ROOT)
            ?.let(stateNamesByCode::get)
            ?.let { TextNormalizer.fold(it) in folded } == true
        val mentionsSourceCity = sourceCity
            ?.let(TextNormalizer::fold)
            ?.takeIf { it.isNotBlank() }
            ?.let { it in folded } == true
        return explicitlyLocal || knownAffiliate || mentionsSourceState || mentionsSourceCity
    }

    private fun stableId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
