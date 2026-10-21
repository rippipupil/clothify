package com.rippipupil.clothify.data.style

import com.rippipupil.clothify.domain.TagVocabulary
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object GeminiRequest {
    private val prompt = buildString {
        appendLine("Eres un estilista. Analiza la prenda principal (o el outfit) de la foto, que es ropa de hombre.")
        appendLine("Devuelve entre 3 y 7 etiquetas que la describan: exactamente 1 tipo de prenda y, si se ven,")
        appendLine("estilo, color y corte o material. Usa solo ids de esta lista (id: significado):")
        TagVocabulary.all.forEach { appendLine("${it.id}: ${it.label}") }
    }

    fun body(base64Jpeg: String): String = buildJsonObject {
        putJsonArray("contents") {
            addJsonObject {
                putJsonArray("parts") {
                    addJsonObject { put("text", prompt) }
                    addJsonObject {
                        putJsonObject("inlineData") {
                            put("mimeType", "image/jpeg")
                            put("data", base64Jpeg)
                        }
                    }
                }
            }
        }
        putJsonObject("generationConfig") {
            put("responseMimeType", "application/json")
            putJsonObject("responseSchema") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("tags") {
                        put("type", "ARRAY")
                        putJsonObject("items") {
                            put("type", "STRING")
                            putJsonArray("enum") { TagVocabulary.all.forEach { add(JsonPrimitive(it.id)) } }
                        }
                    }
                }
                putJsonArray("required") { add("tags") }
            }
        }
    }.toString()
}
