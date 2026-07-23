package com.beiratv.app.data.parser

import java.io.BufferedReader
import java.io.StringReader

/** Lightweight parsed row. Consolidation happens after every source is parsed. */
data class ParsedChannel(
    val sourceId: String,
    val name: String,
    val logo: String?,
    val streamUrl: String,
    val groupTitle: String,
    val tvgId: String?,
    val tvgName: String?,
    val qualityHint: Int? = null
)

object M3uParser {
    private val tvgIdRegex = Regex("""tvg-id="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val tvgNameRegex = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val tvgLogoRegex = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val groupTitleRegex = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val qualityRegex = Regex("""(?i)(2160|1080|720|576|540|480|360)\s*[pi]?""")

    fun parse(content: String, sourceId: String): List<ParsedChannel> {
        val channels = ArrayList<ParsedChannel>(512)
        val reader = BufferedReader(StringReader(content))

        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentLogo: String? = null
        var currentCategory = "Geral"
        var currentName: String? = null

        reader.forEachLine { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF:", ignoreCase = true) -> {
                    currentTvgId = tvgIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    currentTvgName = tvgNameRegex.find(trimmed)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    currentLogo = tvgLogoRegex.find(trimmed)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    currentCategory = groupTitleRegex.find(trimmed)?.groupValues?.getOrNull(1)
                        ?.trim()?.takeIf { it.isNotBlank() } ?: "Geral"
                    currentName = trimmed.substringAfterLast(',', "").trim().ifBlank { currentTvgName }
                }

                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    val name = currentName ?: currentTvgName ?: "Canal ${channels.size + 1}"
                    val quality = qualityRegex.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    channels += ParsedChannel(
                        sourceId = sourceId,
                        name = name,
                        logo = currentLogo,
                        streamUrl = trimmed,
                        groupTitle = currentCategory,
                        tvgId = currentTvgId,
                        tvgName = currentTvgName,
                        qualityHint = quality
                    )
                    currentTvgId = null
                    currentTvgName = null
                    currentLogo = null
                    currentCategory = "Geral"
                    currentName = null
                }
            }
        }
        return channels
    }
}
