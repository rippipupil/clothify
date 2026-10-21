package com.rippipupil.clothify.data.style

import com.rippipupil.clothify.domain.TagVocabulary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object GeminiResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** Ids válidos del vocabulario, sin duplicados; lista vacía si algo no cuadra. */
    fun parse(responseBody: String): List<String> = runCatching {
        val root = json.parseToJsonElement(responseBody) as JsonObject
        val text = (((((root["candidates"] as JsonArray)[0] as JsonObject)["content"] as JsonObject)["parts"] as JsonArray)[0]
            as JsonObject)["text"].let { (it as JsonPrimitive).content }
        val tags = (json.parseToJsonElement(text) as JsonObject)["tags"] as JsonArray
        tags.map { (it as JsonPrimitive).content }.filter(TagVocabulary::isValid).distinct()
    }.getOrDefault(emptyList())
}
