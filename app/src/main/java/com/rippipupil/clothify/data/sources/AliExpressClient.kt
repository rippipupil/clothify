package com.rippipupil.clothify.data.sources

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Llamadas firmadas a la API de afiliados de AliExpress. */
class AliExpressClient(
    private val client: OkHttpClient,
    private val appKey: String,
    private val appSecret: String,
    private val trackingId: String,
    private val baseUrl: String = "https://api-sg.aliexpress.com/",
    private val now: () -> Long = System::currentTimeMillis,
) {
    val configured: Boolean get() = appKey.isNotBlank() && appSecret.isNotBlank()

    /** null si faltan las credenciales; lanza IOException si no hay conexión. */
    suspend fun call(method: String, params: Map<String, String>): HttpResponse? {
        if (!configured) return null
        val all = buildMap {
            putAll(params)
            put("method", method)
            put("app_key", appKey)
            put("timestamp", now().toString())
            put("sign_method", "sha256")
            if (trackingId.isNotBlank()) put("tracking_id", trackingId)
        }
        val url = baseUrl.toHttpUrl().newBuilder().addPathSegment("sync").apply {
            all.forEach { (k, v) -> addQueryParameter(k, v) }
            addQueryParameter("sign", AliExpressSigner.sign(all, appSecret))
        }.build()
        return client.fetch(Request.Builder().url(url).build())
    }

    companion object {
        /** Códigos de error que indican clave, secreto o firma incorrectos. */
        val CREDENTIAL_ERRORS = setOf("InvalidApiKey", "IncompleteSignature", "InvalidSignature", "AppKeyNotExist")
    }
}
