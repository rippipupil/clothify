package com.rippipupil.clothify.data.sources

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Vinted solo responde a su API si la petición lleva las cookies de una visita normal a la web.
 * Cargamos vinted.es en un WebView invisible (como haría el navegador) y reutilizamos esas cookies.
 */
class WebViewVintedCookieProvider(private val context: Context) : CookieProvider {
    private val mutex = Mutex()
    private var cached: String? = null

    override suspend fun cookies(forceRefresh: Boolean): String? = mutex.withLock {
        if (!forceRefresh) cached?.let { return@withLock it }
        cached = withTimeoutOrNull(TIMEOUT_MS) { loadCookies() }?.takeIf { it.isNotBlank() }
        cached
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadCookies(): String? = withContext(Dispatchers.Main) {
        val webView = WebView(context.applicationContext)
        try {
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = MOBILE_USER_AGENT
            CookieManager.getInstance().setAcceptCookie(true)
            suspendCancellableCoroutine { cont ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
                webView.loadUrl(HOME)
            }
            delay(SETTLE_MS) // deja que los scripts de la web terminen de poner sus cookies
            CookieManager.getInstance().flush()
            CookieManager.getInstance().getCookie(HOME)
        } finally {
            webView.destroy()
        }
    }

    private companion object {
        const val HOME = "https://www.vinted.es/"
        const val TIMEOUT_MS = 20_000L
        const val SETTLE_MS = 2_000L
    }
}
