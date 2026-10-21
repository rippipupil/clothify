package com.rippipupil.clothify.domain

sealed interface ProfileEvent {
    val tagIds: Set<String>
    val delta: Double

    data class PresetChosen(override val tagIds: Set<String>) : ProfileEvent { override val delta = 3.0 }
    data class OwnPhoto(override val tagIds: Set<String>) : ProfileEvent { override val delta = 2.0 }
    data class Liked(override val tagIds: Set<String>) : ProfileEvent { override val delta = 1.0 }
    data class Disliked(override val tagIds: Set<String>) : ProfileEvent { override val delta = -0.5 }
}

object ProfileScorer {
    const val MIN_SCORE = -10.0
    const val MAX_SCORE = 10.0

    fun apply(scores: Map<String, Double>, event: ProfileEvent): Map<String, Double> {
        val result = scores.toMutableMap()
        event.tagIds.filter(TagVocabulary::isValid).forEach { id ->
            result[id] = ((result[id] ?: 0.0) + event.delta).coerceIn(MIN_SCORE, MAX_SCORE)
        }
        return result
    }
}
