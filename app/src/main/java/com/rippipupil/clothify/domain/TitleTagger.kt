package com.rippipupil.clothify.domain

/** Etiqueta un título buscando los sinónimos del vocabulario como palabras completas. */
object TitleTagger {

    private val matchers: List<Pair<String, List<String>>> =
        TagVocabulary.all.map { tag -> tag.id to tag.synonyms.map { " ${TagVocabulary.normalize(it)} " } }

    fun tag(title: String): Set<String> {
        val padded = " ${TagVocabulary.normalize(title)} "
        return matchers
            .filter { (_, synonyms) -> synonyms.any { padded.contains(it) } }
            .mapTo(mutableSetOf()) { it.first }
    }
}
