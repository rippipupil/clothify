package com.rippipupil.clothify.data.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.booleanOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

data class HttpResponse(val code: Int, val body: String)

/** Ejecuta la petición en el hilo de E/S. Lanza IOException si no hay conexión. */
suspend fun OkHttpClient.fetch(request: Request): HttpResponse = withContext(Dispatchers.IO) {
    newCall(request).execute().use { HttpResponse(it.code, it.body?.string().orEmpty()) }
}

/** Convierte un fallo de red en NO_INTERNET y un estado HTTP inesperado en BAD_RESPONSE. */
suspend fun safeFetch(block: suspend () -> SourceResult): SourceResult =
    try {
        block()
    } catch (e: IOException) {
        SourceResult.Failed(SourceFailure.NO_INTERNET)
    }

/** Garantiza un intervalo mínimo entre peticiones a una misma fuente. */
class Throttle(private val minIntervalMs: Long, private val now: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    private var last = Long.MIN_VALUE

    suspend fun await() = mutex.withLock {
        if (last != Long.MIN_VALUE) {
            val wait = last + minIntervalMs - now()
            if (wait > 0) delay(wait)
        }
        last = now()
    }
}

internal val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun parseJsonOrNull(text: String): JsonObject? =
    try {
        lenientJson.parseToJsonElement(text) as? JsonObject
    } catch (e: IllegalArgumentException) {
        null
    }

internal fun JsonElement?.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject
internal fun JsonElement?.arr(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray
internal fun JsonElement?.str(key: String): String? =
    ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.takeIf { it.isString || it.content != "null" }?.content
internal fun JsonElement?.num(key: String): Double? =
    ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.let { it.doubleOrNull ?: it.content.replace(',', '.').toDoubleOrNull() }
internal fun JsonElement?.bool(key: String): Boolean? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.booleanOrNull
