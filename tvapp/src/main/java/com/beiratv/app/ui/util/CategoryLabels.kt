package com.beiratv.app.ui.util

import java.util.Locale

fun formatCategoryLabel(raw: String): String {
    if (raw.isBlank()) return "Geral"

    val labels = raw
        .split(';', '|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .map { translateCategoryToken(it) }
        .distinct()

    if (labels.isEmpty()) return "Geral"

    return labels.minWithOrNull(
        compareBy<String> { translatedCategoryPriority(it) }
            .thenBy { it.lowercase(Locale.ROOT) }
    ) ?: "Geral"
}

fun categoryPriority(raw: String): Int {
    return translatedCategoryPriority(formatCategoryLabel(raw))
}

private fun translatedCategoryPriority(label: String): Int {
    val normalized = label.lowercase(Locale.ROOT)
    return when {
        "esporte" in normalized -> 0
        "filme" in normalized -> 1
        "série" in normalized || "series" in normalized -> 2
        "notícia" in normalized -> 3
        "entretenimento" in normalized -> 4
        "infantil" in normalized || "família" in normalized -> 5
        "documentário" in normalized -> 6
        "animação" in normalized || "anime" in normalized -> 7
        "música" in normalized -> 8
        "cultura" in normalized || "educação" in normalized -> 9
        "geral" in normalized -> 10
        "natureza" in normalized || "viagem" in normalized -> 11
        "religioso" in normalized -> 12
        "comédia" in normalized -> 13
        "negócios" in normalized -> 14
        "ciência" in normalized -> 15
        "clima" in normalized -> 16
        "culinária" in normalized -> 17
        "local" in normalized -> 18
        "compras" in normalized -> 90
        "outros" in normalized -> 99
        else -> 50
    }
}

private fun translateCategoryToken(value: String): String {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "animation", "anime" -> "Animação"
        "comedy" -> "Comédia"
        "entertainment" -> "Entretenimento"
        "movie", "movies" -> "Filmes"
        "series", "tv series" -> "Séries"
        "classic", "classics" -> "Clássicos"
        "general" -> "Geral"
        "outdoor" -> "Natureza"
        "shop", "shopping" -> "Compras"
        "sport", "sports" -> "Esportes"
        "news" -> "Notícias"
        "documentary", "documentaries" -> "Documentários"
        "kids", "children" -> "Infantil"
        "music" -> "Música"
        "religious", "religion" -> "Religioso"
        "education", "educational" -> "Educação"
        "lifestyle" -> "Estilo de Vida"
        "business" -> "Negócios"
        "culture" -> "Cultura"
        "science" -> "Ciência"
        "travel" -> "Viagens"
        "weather" -> "Clima"
        "family" -> "Família"
        "food", "cooking" -> "Culinária"
        "local" -> "Local"
        "undefined", "other", "others" -> "Outros"
        else -> value.trim()
    }
}
