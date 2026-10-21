package com.rippipupil.clothify

import android.content.Context
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.location.LocationProvider
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.PriceChecker
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.pool.SettingsBudgetGate
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.settings.settingsDataStore
import com.rippipupil.clothify.data.sources.AliExpressClient
import com.rippipupil.clothify.data.sources.AliExpressPriceFetcher
import com.rippipupil.clothify.data.sources.WallapopPriceFetcher
import com.rippipupil.clothify.data.sources.AliExpressSource
import com.rippipupil.clothify.data.sources.GoogleShoppingSource
import com.rippipupil.clothify.data.sources.Throttle
import com.rippipupil.clothify.data.sources.VintedSource
import com.rippipupil.clothify.data.sources.WallapopSource
import com.rippipupil.clothify.data.sources.WebViewVintedCookieProvider
import com.rippipupil.clothify.data.style.ClipStyleAnalyzer
import com.rippipupil.clothify.data.style.FallbackStyleAnalyzer
import com.rippipupil.clothify.data.style.GeminiStyleAnalyzer
import com.rippipupil.clothify.data.style.OnnxClipEmbedder
import com.rippipupil.clothify.data.style.StyleAnalyzer
import com.rippipupil.clothify.data.style.VisualRanker
import android.graphics.BitmapFactory
import kotlinx.coroutines.withContext
import okhttp3.Request
import com.rippipupil.clothify.domain.QueryPlanner
import com.rippipupil.clothify.domain.Recommender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Crea y conecta todas las piezas de la app (inyección de dependencias manual). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val db = ClothifyDatabase.create(appContext)

    val settings = SettingsRepository(appContext.settingsDataStore)
    val profile = ProfileRepository(db)
    val basket = BasketRepository(db)
    val location = LocationProvider(appContext)
    val recommender = Recommender(Random.Default)

    /** CLIP en el móvil (si el modelo viene en el APK). */
    val clipEmbedder = OnnxClipEmbedder(appContext)
    private val tagEmbeddings by lazy { OnnxClipEmbedder.loadTagEmbeddings(appContext) }

    /** Gemini primero; si falla o no hay clave, análisis en el móvil con CLIP. */
    val styleAnalyzer: StyleAnalyzer = FallbackStyleAnalyzer(
        primary = GeminiStyleAnalyzer(http, BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL),
        fallback = ClipStyleAnalyzer(clipEmbedder) { tagEmbeddings },
    )

    /** Reordena "Buscar con foto" por parecido visual (solo si el modelo CLIP viene en el APK). */
    val visualRanker = VisualRanker(clipEmbedder) { url -> downloadThumbnail(url) }

    private suspend fun downloadThumbnail(url: String) = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@use null
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        }.getOrNull()
    }

    val aliExpress = AliExpressClient(
        http,
        BuildConfig.ALIEXPRESS_APP_KEY,
        BuildConfig.ALIEXPRESS_APP_SECRET,
        BuildConfig.ALIEXPRESS_TRACKING_ID,
    )

    val priceChecker = PriceChecker(
        basket,
        listOf(WallapopPriceFetcher(http, Throttle(UNOFFICIAL_INTERVAL_MS)), AliExpressPriceFetcher(aliExpress)),
    )

    val pool = ProductPoolRepository(
        db = db,
        sources = listOf(
            WallapopSource(http, Throttle(UNOFFICIAL_INTERVAL_MS)),
            VintedSource(http, WebViewVintedCookieProvider(appContext), Throttle(UNOFFICIAL_INTERVAL_MS)),
            GoogleShoppingSource(http, BuildConfig.SERPAPI_KEY, SettingsBudgetGate(settings)),
            AliExpressSource(aliExpress, Throttle(OFFICIAL_INTERVAL_MS)),
        ),
        planner = QueryPlanner(Random.Default),
        profile = profile,
        settings = settings,
    )

    private companion object {
        const val UNOFFICIAL_INTERVAL_MS = 3_000L
        const val OFFICIAL_INTERVAL_MS = 1_000L
    }
}
