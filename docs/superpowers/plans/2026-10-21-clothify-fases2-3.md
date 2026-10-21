# Clothify Fases 2–3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir AliExpress oficial, alertas de precio de la cesta, IA de fotos en el móvil (CLIP) y cerrar los pendientes menores.

**Architecture:** Mismo módulo `:app`. Lógica pura en `domain/` o en objetos sin Android; E/S en `data/`; UI en `ui/`. ONNX Runtime para CLIP; WorkManager para las alertas.

**Tech Stack:** lo de la Fase 1 + `com.microsoft.onnxruntime:onnxruntime-android:1.22.0`, `androidx.work:work-runtime-ktx:2.10.5`, `androidx.room:room-testing:2.8.3`.

**Spec:** `docs/superpowers/specs/2026-10-21-clothify-fases2-3-design.md`

## Global Constraints

- Todo lo de la Fase 1 sigue vigente.
- Secretos nuevos solo vía `BuildConfig` (env o `local.properties`): `ALIEXPRESS_APP_KEY`, `ALIEXPRESS_APP_SECRET`, `ALIEXPRESS_TRACKING_ID`.
- Ningún modelo ni clave en git.
- CLIP: 224×224, media (0.48145466, 0.4578275, 0.40821073), desviación (0.26862954, 0.26130258, 0.27577711); 4 etiquetas (1 por grupo).
- Alertas cada 12 h, solo Wallapop y AliExpress.

## Review Focus

1. Base de datos de la Fase 1 ya instalada en el móvil → la migración 1→2 conserva la cesta (Tarea 3).
2. APK sin modelo CLIP (compilación local) → Buscar sigue funcionando con Gemini o etiquetas a mano, sin cierre (Tarea 5).
3. Notificaciones denegadas en Android 13+ → el interruptor vuelve a desactivado, sin cierre (Tarea 4).
4. Artículo de Wallapop vendido (404) → marcado como no disponible, no como bajada de precio (Tarea 3).
5. Respuesta de AliExpress con precio como texto o sin productos → lista vacía, no error (Tarea 2).

---

### Task 1: Pendientes menores
Files: `ui/Messages.kt`, `ui/onboarding/OnboardingViewModel.kt`, `ui/search/SearchScreen.kt`, `AndroidManifest.xml`, `res/xml/file_paths.xml`; tests.
- [ ] Test: `sourceWarning` con Wallapop y Vinted en `NO_INTERNET` → `"Sin conexión"`; con Vinted `BLOCKED` + Wallapop `NO_INTERNET` → `"Sin conexión · Vinted no responde ahora"`.
- [ ] Test: tras `finish().join()`, `state.value == OnboardingState()`.
- [ ] Implementar; `TakePicture` + `FileProvider` (`${applicationId}.files`, ruta `cache/photos/`). Commit `fix: onboarding reset, full-res camera, warning dedup`.

### Task 2: Fuente AliExpress
Files: `data/sources/AliExpressSigner.kt`, `AliExpressParser.kt`, `AliExpressSource.kt`; fixture `aliexpress_query.json`; `app/build.gradle.kts` (BuildConfig); `AppContainer.kt`; tests.
- Produces: `object AliExpressSigner { fun sign(params: Map<String, String>, secret: String): String }`; `object AliExpressParser { fun parseQuery(json, query, now): List<Product>?; fun parseDetailPrices(json): Map<String, Double>? }`; `class AliExpressClient(client, appKey, appSecret, trackingId, baseUrl, now)` con `suspend fun call(method, params): HttpResponse?` (null si faltan credenciales); `class AliExpressSource(api: AliExpressClient, throttle) : ProductSource` (`Source.ALIEXPRESS`).
- [ ] Tests: firma de `{app_key=12345, method=m, timestamp=1, sign_method=sha256}` con secreto `secret` = HMAC-SHA256 hex mayúsculas de `"app_key12345methodmsign_methodsha256timestamp1"`; parser (precio texto, sin imagen descartado, máximo); fuente sin credenciales → `DISABLED` sin petición; petición con `method`, `keywords="sudadera oversize hombre"`, `max_sale_price=6000`, `sign` presente; `error_response` `InvalidApiKey` → `DISABLED`.
- [ ] Implementar; `Source.ALIEXPRESS("AliExpress")`. Commit `feat: aliexpress affiliate source`.

### Task 3: Precios de la cesta (datos)
Files: `data/local/*` (v2 + migración), `data/pool/BasketRepository.kt`, `data/pool/PriceChecker.kt`, `data/sources/WallapopItemParser.kt`; fixture `wallapop_item.json`; tests.
- Produces: `BasketItem(product, addedAtMillis, currentPriceEur: Double?, unavailable: Boolean)`; `BasketRepository.updatePrice(id, price: Double?, unavailable: Boolean, at)`; `sealed interface PriceLookup { Price(eur); Gone; Unknown }`; `interface PriceFetcher { fun supports(source): Boolean; suspend fun lookup(product): PriceLookup }`; `class PriceChecker(basket, fetchers, now) { suspend fun check(): List<PriceAlert> }`; `sealed interface PriceAlert { Dropped(product, from, to); Gone(product) }`; `WallapopPriceFetcher`, `AliExpressPriceFetcher`.
- [ ] Tests: migración 1→2 conserva una fila; `check()` con bajada → `Dropped(30→24)` y guarda 24; subida → sin alerta pero guarda; `Gone` → alerta una sola vez; Vinted no se consulta; parser del artículo real → 20.0.
- [ ] Implementar. Commit `feat: basket price checks`.

### Task 4: Alertas (worker + UI)
Files: `data/pool/PriceCheckWorker.kt`, `ui/Notifications.kt`, ajustes `priceAlertsEnabled`, `BasketScreen`, `ProfileScreen`, manifest; tests.
- [ ] Test: `SettingsRepository` guarda `priceAlertsEnabled`; `BasketViewModel.priceLabel(item)` → "Antes 30 € · ahora 24 €" / "Ya no está disponible" / "Sin aviso de precio" (Vinted).
- [ ] Implementar `PriceCheckWorker` (CoroutineWorker → `PriceChecker.check()` → notificaciones), `PriceAlerts.schedule/cancel(context)` (12 h, red conectada), aviso en Cesta y interruptor en Perfil con permiso `POST_NOTIFICATIONS`. Commit `feat: price drop alerts`.

### Task 5: IA en el móvil
Files: `tools/clip/embed_tags.py`, `assets/clip/tag_embeddings.json`, `data/style/ClipPreprocessor.kt`, `ClipZeroShot.kt`, `ClipEmbedder.kt`, `ClipStyleAnalyzer.kt`, `FallbackStyleAnalyzer.kt`, `StyleAnalyzer.kt` (`MODEL_UNAVAILABLE`, `Success.onDevice`), `.gitignore`, `build.gradle.kts` (`noCompress "onnx"`), workflow (descarga + caché); tests.
- Produces: `object ClipPreprocessor { fun normalize(pixels: IntArray): FloatArray }` (224×224 ARGB → NCHW); `class TagEmbeddings(map: Map<String, FloatArray>) { companion fun parse(json): TagEmbeddings }`; `object ClipZeroShot { fun tags(image: FloatArray, tags: TagEmbeddings): List<String>; fun cosine(a, b): Double }`; `interface ImageEmbedder { suspend fun embed(bitmap): FloatArray?; val available: Boolean }`; `class OnnxClipEmbedder(context)`; `class ClipStyleAnalyzer(embedder, tags)`; `class FallbackStyleAnalyzer(primary, fallback)`.
- [ ] Tests: normalización de un píxel blanco y uno negro; NCHW; zero-shot elige 1 por grupo con vectores sintéticos; embeddings del asset cubren el vocabulario y tienen dimensión 512; fallback (Gemini falla → CLIP; Gemini ok → Gemini; CLIP no disponible → error de Gemini).
- [ ] Commit `feat: on-device CLIP analyzer`.

### Task 6: Parecido visual en Buscar
Files: `data/style/VisualRanker.kt`, `ui/search/*`; tests.
- Produces: `class VisualRanker(embedder: ImageEmbedder, loadImage: suspend (String) -> Bitmap?) { suspend fun rank(photo: Bitmap, products: List<Product>, limit: Int = 24): List<Product>? }` (null si no hay modelo).
- [ ] Tests: reordena por coseno con embedder falso; null sin modelo; imágenes que fallan van al final. `SearchViewModel` pasa a `Results(visual = true)`.
- [ ] Commit `feat: visual similarity ranking`.

### Task 7: README, CI, revisión final
- [ ] Workflow: caché + descarga del modelo; secretos AliExpress. README: AliExpress, alertas, IA en el móvil. Build release local OK, push, CI verde.
