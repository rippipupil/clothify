package com.rippipupil.clothify.domain

import kotlin.random.Random

/** Decide qué buscar en las fuentes: 2 búsquedas de explotación y 1 de exploración. */
class QueryPlanner(private val random: Random) {

    fun plan(
        scores: Map<String, Double>,
        recentTexts: Set<String>,
        maxPriceEur: Double,
        sizes: UserSizes,
        location: LatLng?,
    ): List<SearchQuery> {
        val used = recentTexts.toMutableSet()
        val builders = listOf({ exploit(scores) }, { exploit(scores) }, { explore(scores) })
        return builders.map { build ->
            var tags = build()
            var retries = 0
            while (textOf(tags) in used && retries++ < MAX_RETRIES) tags = build()
            used += textOf(tags)
            SearchQuery(
                text = textOf(tags),
                tagIds = tags.map { it.id }.toSet(),
                categoryTag = tags.first().id,
                maxPriceEur = maxPriceEur,
                sizes = sizes,
                location = location,
            )
        }
    }

    /** Categoría + hasta 2 etiquetas, elegidas al azar ponderado entre las de puntuación positiva. */
    private fun exploit(scores: Map<String, Double>): List<Tag> {
        val positive = scores.filterValues { it > 0 }.mapNotNull { (id, s) -> TagVocabulary.byId(id)?.let { it to s } }
        val categories = positive.filter { it.first.group == TagGroup.CATEGORY }
        if (categories.isEmpty()) return explore(scores)
        val category = weightedPick(categories)
        val extras = positive.filter { it.first.group != TagGroup.CATEGORY }.toMutableList()
        val chosen = mutableListOf<Tag>()
        val count = random.nextInt(0, MAX_EXTRAS + 1).coerceAtMost(extras.size)
        repeat(count) {
            val pick = weightedPick(extras)
            chosen += pick
            extras.removeAll { it.first == pick }
        }
        return listOf(category) + chosen.sortedBy { it.group.ordinal }
    }

    /** Categoría al azar + un estilo al azar que no le disguste al usuario. */
    private fun explore(scores: Map<String, Double>): List<Tag> {
        val category = CATEGORIES[random.nextInt(CATEGORIES.size)]
        val styles = STYLES.filter { (scores[it.id] ?: 0.0) >= 0 }
        return if (styles.isEmpty()) listOf(category) else listOf(category, styles[random.nextInt(styles.size)])
    }

    private fun weightedPick(items: List<Pair<Tag, Double>>): Tag {
        var r = random.nextDouble() * items.sumOf { it.second }
        for ((tag, weight) in items) {
            r -= weight
            if (r < 0) return tag
        }
        return items.last().first
    }

    private fun textOf(tags: List<Tag>) = tags.joinToString(" ") { it.searchTerm }

    companion object {
        private const val MAX_RETRIES = 20
        private const val MAX_EXTRAS = 2
        private val CATEGORIES = TagVocabulary.all.filter { it.group == TagGroup.CATEGORY }
        private val STYLES = TagVocabulary.all.filter { it.group == TagGroup.STYLE }
    }
}
