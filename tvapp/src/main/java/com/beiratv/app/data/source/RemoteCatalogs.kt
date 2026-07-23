package com.beiratv.app.data.source

import com.beiratv.app.data.normalization.TextNormalizer
import com.beiratv.app.data.local.ChannelAliasEntity
import com.beiratv.app.data.local.ChannelLogoEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class RemoteCatalogs(private val moshi: Moshi = Moshi.Builder().build()) {
    @Suppress("UNCHECKED_CAST")
    fun parseAliases(json: String): Pair<Map<String, String>, List<ChannelAliasEntity>> {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val map = moshi.adapter<Map<String, Any?>>(type).fromJson(json).orEmpty()
        val lookup = mutableMapOf<String, String>()
        val entities = mutableListOf<ChannelAliasEntity>()
        map.forEach { (canonical, value) ->
            val canonicalKey = TextNormalizer.fold(canonical)
            lookup[canonicalKey] = canonicalKey
            entities += ChannelAliasEntity(canonicalKey, canonicalKey)
            (value as? List<*>)?.filterIsInstance<String>()?.forEach { alias ->
                val key = TextNormalizer.fold(alias)
                lookup[key] = canonicalKey
                entities += ChannelAliasEntity(key, canonicalKey)
            }
        }
        return lookup to entities.distinctBy { it.alias }
    }

    @Suppress("UNCHECKED_CAST")
    fun parseLogos(json: String): Pair<Map<String, String>, List<ChannelLogoEntity>> {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val map = moshi.adapter<Map<String, Any?>>(type).fromJson(json).orEmpty()
        val lookup = mutableMapOf<String, String>()
        val entities = mutableListOf<ChannelLogoEntity>()
        map.forEach { (canonical, raw) ->
            val entry = raw as? Map<*, *>
            val logo = when (raw) {
                is String -> raw
                is Map<*, *> -> raw["logo"] as? String
                else -> null
            } ?: return@forEach
            if (!logo.startsWith("http")) return@forEach
            val canonicalKey = TextNormalizer.fold(canonical)
            lookup[canonicalKey] = logo
            entities += ChannelLogoEntity(canonicalKey, logo)
            (entry?.get("aliases") as? List<*>)?.filterIsInstance<String>()?.forEach { alias ->
                val key = TextNormalizer.fold(alias)
                lookup[key] = logo
                entities += ChannelLogoEntity(key, logo)
            }
        }
        return lookup to entities.distinctBy { it.key }
    }
}
