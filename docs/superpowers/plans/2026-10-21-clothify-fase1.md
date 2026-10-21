# Clothify Fase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android personal que aprende el estilo del usuario y le muestra en
tarjetas swipe prendas reales de hombre de Wallapop, Vinted y Google Shopping.

**Architecture:** Un único módulo `:app`. `domain/` es Kotlin puro probado con
JUnit; `data/` contiene fuentes HTTP (OkHttp), Room, DataStore y Gemini; `ui/` son
pantallas Compose con un ViewModel cada una, conectadas por un `AppContainer`
manual. GitHub Actions ejecuta pruebas y compila un APK release firmado.

**Tech Stack:** AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21, KSP 2.2.21-2.0.4,
compileSdk/targetSdk 36, minSdk 26, Compose BOM 2025.10.01, Material 3,
Navigation Compose 2.9.5, Room 2.8.3, DataStore 1.1.7, Coil 3.3.0, OkHttp 4.12.0,
kotlinx.serialization 1.9.0, coroutines 1.10.2, play-services-location 21.3.0,
JUnit 4.13.2, Robolectric 4.16 (pruebas Compose en JVM).

**Spec:** `docs/superpowers/specs/2026-10-21-clothify-fase1-design.md`

## Global Constraints

- Paquete `com.rippipupil.clothify`. Textos de la interfaz en español.
- País fijo España, euros, sección hombre.
- `domain/` no importa nada de `android.*` ni de `data/`.
- Puntuaciones: estilo bienvenida +3, foto propia +2, like +1, dislike −0,5; límites [−10, +10].
- Recomendador: 0,8 mejor nota / 0,2 azar; nota = suma ÷ √(nº etiquetas); máx. 3 seguidas de la misma fuente.
- Rellenar cuando < 15 prendas sin ver; 3 búsquedas (2 explotación + 1 exploración); no repetir texto en 24 h.
- Caducidad de la reserva: 3 días. La cesta no caduca.
- SerpApi: máx. 8/día y 240/mes. Fuentes no oficiales: ≥ 3 s entre peticiones por fuente.
- Vinted bloqueado tras renovar cookie → fuente desactivada 1 h.
- Precio máximo por defecto 60 €, rango 10–300 €.
- Claves `GEMINI_API_KEY`, `SERPAPI_KEY` vía `BuildConfig` desde variables de entorno o `local.properties`; nunca en el repositorio.

## Review Focus

1. Respuesta de una fuente con campos ausentes o con otro formato (precio como texto, sin imágenes) → se descarta esa prenda, no la búsqueda; JSON no válido → `BAD_RESPONSE`, nunca un crash. (Tareas 8–10)
2. Títulos con tildes, mayúsculas y signos ("SUDADERA Oversize, talla-M") → `TitleTagger` y el extractor de talla los reconocen. (Tareas 2, 9)
3. Claves de API vacías (APK compilado sin secretos) → Google Shopping devuelve `BUDGET_EXHAUSTED`-equivalente `DISABLED` sin llamar, Gemini devuelve `Failure(UNKNOWN)` con mensaje "Falta la clave de Gemini"; la app no se cae. (Tareas 8, 12)
4. Reserva vacía y todas las fuentes fallando en el primer arranque → estado "No hay más prendas por ahora", nunca un spinner infinito. (Tareas 11, 15)
5. Cambio de día/mes a medianoche para el presupuesto de SerpApi → el contador diario se reinicia, el mensual no hasta cambiar de mes. (Tarea 5)

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew*
app/build.gradle.kts, app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/rippipupil/clothify/
  ClothifyApp.kt              Application; crea AppContainer
  AppContainer.kt             Conecta todas las piezas
  MainActivity.kt             setContent { ClothifyRoot() }
  domain/
    Tag.kt                    Tag, TagGroup
    TagVocabulary.kt          lista de etiquetas, byId, normalize()
    TitleTagger.kt            etiquetas desde título
    SizeExtractor.kt          talla desde título / size_title
    StylePresets.kt           estilos predefinidos → etiquetas
    ProfileScorer.kt          aplica eventos a puntuaciones
    Product.kt                Product, Source, UserSizes, SearchQuery, LatLng
    Recommender.kt            siguiente tarjeta
    Similarity.kt             Jaccard
    QueryPlanner.kt           genera SearchQuery
    SearchBudget.kt           presupuesto SerpApi
    StoreLinkBuilder.kt       enlaces "buscar parecidos"
    Geo.kt                    distancia haversine
  data/
    local/ ClothifyDatabase.kt, entities.kt, daos.kt, Converters.kt
    settings/ SettingsRepository.kt (DataStore) , UserSettings.kt
    sources/ ProductSource.kt, Http.kt (OkHttp + throttle),
             WallapopSource.kt, WallapopParser.kt,
             VintedSource.kt, VintedParser.kt, VintedCookieProvider.kt,
             GoogleShoppingSource.kt, GoogleShoppingParser.kt
    pool/ ProductPoolRepository.kt, ProfileRepository.kt, BasketRepository.kt
    style/ StyleAnalyzer.kt, GeminiStyleAnalyzer.kt, GeminiResponseParser.kt
    location/ LocationProvider.kt
  ui/
    theme/ Theme.kt
    ClothifyRoot.kt           navegación + barra inferior
    components/ ProductCard.kt, SwipeableCard.kt, TagChips.kt, ProductImage.kt
    onboarding/ OnboardingScreen.kt, OnboardingViewModel.kt
    discover/ DiscoverScreen.kt, DiscoverViewModel.kt
    search/ SearchScreen.kt, SearchViewModel.kt
    basket/ BasketScreen.kt, BasketViewModel.kt
    profile/ ProfileScreen.kt, ProfileViewModel.kt
    Links.kt                  openUrl(context, url)
app/src/test/java/com/rippipupil/clothify/... (espejo de lo anterior)
app/src/test/resources/fixtures/ wallapop_search.json, vinted_search.json, serpapi_shopping.json
.github/workflows/build.yml
README.md
```

---

### Task 1: Esqueleto del proyecto que compila

**Files:** raíz Gradle, `gradle/libs.versions.toml`, `app/build.gradle.kts`,
`AndroidManifest.xml`, `ClothifyApp.kt`, `MainActivity.kt`, `ui/theme/Theme.kt`,
`.gitignore`, wrapper Gradle.

**Interfaces:**
- Produces: `BuildConfig.GEMINI_API_KEY: String`, `BuildConfig.SERPAPI_KEY: String`,
  `BuildConfig.GEMINI_MODEL = "gemini-flash-latest"`; `ClothifyTheme { }`.

- [ ] **Step 1:** Generar wrapper: `gradle wrapper --gradle-version 8.14.3`.
- [ ] **Step 2:** Catálogo de versiones con las del Tech Stack; plugins
  `android-application`, `kotlin-android`, `kotlin-compose`, `kotlin-serialization`, `ksp`.
- [ ] **Step 3:** `app/build.gradle.kts`: `buildConfig = true`, `compose = true`;
  claves leídas de `System.getenv(name) ?: localProperties[name] ?: ""`.
  `signingConfigs.release` desde env `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`; si falta `KEYSTORE_FILE`, release usa la firma debug.
  `isMinifyEnabled = false` (evita problemas de R8 con serialización).
  `testOptions.unitTests.isIncludeAndroidResources = true`.
- [ ] **Step 4:** Manifest: permisos `INTERNET`, `ACCESS_COARSE_LOCATION`,
  `<queries>` con intent VIEW https. `MainActivity` muestra "Clothify".
- [ ] **Step 5:** Verificar: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
- [ ] **Step 6:** Commit `chore: scaffold Android project`.

### Task 2: Vocabulario, etiquetado de títulos y talla

**Files:** `domain/Tag.kt`, `TagVocabulary.kt`, `TitleTagger.kt`, `SizeExtractor.kt`; tests.

**Interfaces:**
- Produces:
  - `enum class TagGroup { CATEGORY, STYLE, COLOR, FIT_MATERIAL }`
  - `data class Tag(val id: String, val group: TagGroup, val label: String, val synonyms: List<String>, val sizeKind: SizeKind? = null)`
  - `enum class SizeKind { TOP, WAIST, SHOE }` (solo en categorías)
  - `object TagVocabulary { val all: List<Tag>; fun byId(id: String): Tag?; fun isValid(id: String): Boolean; fun normalize(text: String): String }`
  - `object TitleTagger { fun tag(title: String): Set<String> }`
  - `object SizeExtractor { fun fromText(text: String): String? }` → "S","M","L","XL","XXL","XS", número de cintura "28".."40" o EU "38".."47"

- [ ] **Step 1: Tests**
  - `TagVocabularyTest`: ids únicos; ≥ 70 etiquetas; cada grupo ≥ 8; `normalize("Sudadera ÓVERSIZE, Marrón!") == "sudadera oversize marron"`.
  - `TitleTaggerTest`: `tag("SUDADERA Oversize negra Nike")` contiene `sudadera`, `oversize`, `negro`; `tag("Hoodie black")` contiene `sudadera`, `negro`; `tag("Pantalón vaquero recto")` contiene `vaqueros`, `recto`, `denim`; `tag("cartera")` no contiene `camiseta` (palabra completa).
  - `SizeExtractorTest`: `"Sudadera talla M" → "M"`, `"Camiseta - Talla XL" → "XL"`, `"T. L"→"L"`, `"Zapatillas 42" → "42"`, `"M / 38 / 10" → "M"`, `"Sudadera Nike" → null`.
- [ ] **Step 2:** Ejecutar `./gradlew testDebugUnitTest --tests '*domain*'` → FAIL.
- [ ] **Step 3:** Implementar. Sinónimos en minúsculas sin tildes; coincidencia por palabra completa o frase (regex `\b`). Vaqueros añade sinónimos `vaquero, vaqueros, jeans` e implica `denim` mediante sus sinónimos compartidos.
- [ ] **Step 4:** Tests → PASS. Commit `feat: tag vocabulary, title tagger, size extractor`.

### Task 3: Estilos predefinidos y puntuaciones

**Files:** `domain/StylePresets.kt`, `ProfileScorer.kt`; tests.

**Interfaces:**
- Produces:
  - `data class StylePreset(val id: String, val label: String, val tagIds: Set<String>)`; `object StylePresets { val all: List<StylePreset> }` (≥ 8: Streetwear, Minimalista, Vintage, Old money, Deportivo, Workwear, Formal, Y2K, Techwear, Casual)
  - `sealed interface ProfileEvent { PresetChosen(tagIds), OwnPhoto(tagIds), Liked(tagIds), Disliked(tagIds) }`
  - `object ProfileScorer { fun apply(scores: Map<String, Double>, event: ProfileEvent): Map<String, Double> }`

- [ ] **Step 1: Tests** `ProfileScorerTest`: PresetChosen → +3 por etiqueta; OwnPhoto +2; Liked +1; Disliked −0,5; 5×PresetChosen → 10.0 (tope); 30×Disliked → −10.0; etiquetas ausentes del evento no cambian. `StylePresetsTest`: todos los `tagIds` son válidos.
- [ ] **Step 2–4:** fallar, implementar, pasar. Commit `feat: style presets and profile scoring`.

### Task 4: Modelo de producto, recomendador y parecido

**Files:** `domain/Product.kt`, `Recommender.kt`, `Similarity.kt`, `Geo.kt`; tests.

**Interfaces:**
- Produces:
  - `enum class Source(val label: String) { WALLAPOP("Wallapop"), VINTED("Vinted"), GOOGLE_SHOPPING("Google Shopping") }`
  - `data class Product(id, source, title, imageUrl, priceEur: Double, sellerName, productUrl, size: String?, distanceKm: Double?, tagIds: Set<String>, fetchedAtMillis: Long)`
  - `data class LatLng(val lat: Double, val lon: Double)`; `data class UserSizes(val top: String?, val waist: String?, val shoe: String?)`
  - `data class SearchQuery(val text: String, val tagIds: Set<String>, val categoryTag: String?, val maxPriceEur: Double, val sizes: UserSizes, val location: LatLng?)`
  - `class Recommender(private val random: Random) { fun score(p: Product, scores: Map<String, Double>): Double; fun next(candidates: List<Product>, scores: Map<String, Double>, recentSources: List<Source>): Product? }`
  - `object Similarity { fun jaccard(a: Set<String>, b: Set<String>): Double; fun rank(query: Set<String>, products: List<Product>, limit: Int = 60): List<Product> }`
  - `object Geo { fun distanceKm(a: LatLng, b: LatLng): Double }`
- [ ] **Step 1: Tests** `RecommenderTest` con `Random` falso: candidatas vacías → null; con random ≥ 0,2 (rama mejor nota) elige la de mayor `score`; `score` de {a:4,b:0} con 4 etiquetas = 2.0; si `recentSources` son 3× WALLAPOP y hay otra fuente, no devuelve WALLAPOP; con random < 0,2 elige índice aleatorio. `SimilarityTest`: jaccard({a,b},{b,c}) = 1/3; rank ordena desc. y excluye 0. `GeoTest`: Madrid–Barcelona ≈ 505 km ± 5.
- [ ] **Step 2–4:** fallar, implementar, pasar. Commit `feat: recommender and similarity`.

### Task 5: Planificador de búsquedas y presupuesto SerpApi

**Files:** `domain/QueryPlanner.kt`, `SearchBudget.kt`; tests.

**Interfaces:**
- Produces:
  - `class QueryPlanner(private val random: Random) { fun plan(scores: Map<String, Double>, recentTexts: Set<String>, maxPriceEur: Double, sizes: UserSizes, location: LatLng?): List<SearchQuery> }` → 3 consultas, `text` = etiquetas en español separadas por espacio (sin "hombre": lo añade cada fuente).
  - `data class BudgetState(val day: String, val dayCount: Int, val month: String, val monthCount: Int)` (day "2026-10-21", month "2026-10")
  - `object SearchBudget { const val DAILY = 8; const val MONTHLY = 240; fun canSearch(s: BudgetState, today: LocalDate): Boolean; fun record(s: BudgetState, today: LocalDate): BudgetState; fun remaining(s, today): Pair<Int, Int> }`
- [ ] **Step 1: Tests** QueryPlanner: devuelve 3; cada una tiene exactamente 1 etiqueta CATEGORY y ≤ 3 en total; con perfil vacío sigue devolviendo 3 (todo exploración); no devuelve textos en `recentTexts` (reintenta hasta 20 veces, luego acepta). SearchBudget: 8 registros el mismo día → `canSearch` false; al día siguiente true y `dayCount` = 0; 240 en el mes → false aunque sea otro día; cambio de mes reinicia ambos.
- [ ] **Step 2–4:** fallar, implementar, pasar. Commit `feat: query planner and search budget`.

### Task 6: Enlaces a tiendas

**Files:** `domain/StoreLinkBuilder.kt`; test.

**Interfaces:**
- Produces: `enum class Store(val label: String) { AMAZON, ALIEXPRESS, SHEIN, VINTED, WALLAPOP }`; `object StoreLinkBuilder { fun queryText(tagIds: Set<String>): String; fun links(tagIds: Set<String>, location: LatLng?): List<Pair<Store, String>> }`
- [ ] **Step 1: Tests** con etiquetas {chaqueta, denim, oversize, azul_claro}: texto = "chaqueta denim oversize azul claro hombre" (categoría primero, luego STYLE, COLOR, FIT_MATERIAL, máx. 3 extra); Amazon `https://www.amazon.es/s?k=chaqueta+denim+oversize+azul+claro+hombre`; AliExpress `https://es.aliexpress.com/w/wholesale-chaqueta-denim-oversize-azul-claro-hombre.html`; Shein `https://es.shein.com/pdsearch/chaqueta%20denim%20oversize%20azul%20claro%20hombre/`; Vinted `https://www.vinted.es/catalog?search_text=chaqueta+denim+oversize+azul+claro+hombre`; Wallapop sin ubicación `https://es.wallapop.com/app/search?keywords=chaqueta+denim+oversize+azul+claro+hombre`, con ubicación añade `&latitude=40.42&longitude=-3.7`.
- [ ] **Step 2–4:** fallar, implementar, pasar. Commit `feat: store link builder`.

### Task 7: Persistencia local

**Files:** `data/local/*`, `data/settings/*`, `data/pool/ProfileRepository.kt`, `BasketRepository.kt`; tests Robolectric.

**Interfaces:**
- Produces:
  - Entidades: `ProductEntity` (campos de Product, `tagIds` como texto separado por comas), `TagScoreEntity(tagId, score)`, `BasketItemEntity(productId, product campos..., addedAtMillis)`, `SeenProductEntity(productId, liked, seenAtMillis)`, `QueryHistoryEntity(text, atMillis)`.
  - `ProductDao`: `upsertAll`, `unseenFresh(minFetchedAt): List<ProductEntity>`, `deleteOlderThan(millis)`, `countUnseenFresh(minFetchedAt): Int`, `deleteAll`.
  - `ClothifyDatabase.create(context)`; `ClothifyDatabase.inMemory(context)` para pruebas.
  - `data class UserSettings(onboardingDone, sizes: UserSizes, maxPriceEur = 60.0, locationEnabled, lastLocation: LatLng?, budget: BudgetState)`; `class SettingsRepository(dataStore) { val settings: Flow<UserSettings>; suspend fun update(transform: (UserSettings) -> UserSettings) }`
  - `class ProfileRepository(db) { val scores: Flow<Map<String, Double>>; suspend fun apply(event: ProfileEvent); suspend fun snapshot(): Map<String, Double>; suspend fun clear() }`
  - `class BasketRepository(db) { val items: Flow<List<BasketItem>>; suspend fun add(p: Product); suspend fun remove(id: String); suspend fun clear() }`; `data class BasketItem(val product: Product, val addedAtMillis: Long)`
- [ ] **Step 1: Tests** (Robolectric, BD en memoria): ProfileRepository aplica Liked y `snapshot` refleja +1; Basket add/remove; `unseenFresh` excluye vistos y caducados; SettingsRepository guarda y lee `maxPriceEur` y `budget`.
- [ ] **Step 2–4:** fallar, implementar, pasar. Commit `feat: local persistence`.

### Task 8: Fuente Google Shopping (SerpApi)

**Files:** `data/sources/ProductSource.kt`, `Http.kt`, `GoogleShoppingParser.kt`, `GoogleShoppingSource.kt`; fixture `serpapi_shopping.json`; test.

**Interfaces:**
- Produces:
  - `interface ProductSource { val source: Source; suspend fun search(query: SearchQuery): SourceResult }`
  - `sealed interface SourceResult { data class Ok(val products: List<Product>); data class Failed(val reason: SourceFailure) }`
  - `enum class SourceFailure { NO_INTERNET, BLOCKED, BUDGET_EXHAUSTED, DISABLED, BAD_RESPONSE }`
  - `class Throttle(minIntervalMs: Long, clock)`: `suspend fun await()`
  - `object GoogleShoppingParser { fun parse(json: String, query: SearchQuery, nowMillis: Long): List<Product>? }` (null = JSON no válido)
  - `class GoogleShoppingSource(client: OkHttpClient, apiKey: String, budget: BudgetGate)`; `interface BudgetGate { suspend fun tryConsume(): Boolean }`
- URL: `https://serpapi.com/search.json?engine=google_shopping&q={text}+hombre&gl=es&hl=es&location=Spain&api_key=…`. Producto: `title`, `extracted_price`, `source`→sellerName, `thumbnail`, `link ?: product_link`, id `gs:{product_id ?: hash(link)}`. Precio > máximo → descartado. tagIds = query.tagIds ∪ TitleTagger.tag(title).
- [ ] **Step 1: Tests** parser con fixture de 3 resultados (uno sin `extracted_price` → descartado, uno con precio 80 y máximo 60 → descartado); JSON "basura" → null; source con apiKey "" → `Failed(DISABLED)` sin petición; `BudgetGate` falso → `Failed(BUDGET_EXHAUSTED)`.
- [ ] **Step 2–4:** fallar, implementar, pasar. Commit `feat: google shopping source`.

### Task 9: Fuente Wallapop

**Files:** `WallapopParser.kt`, `WallapopSource.kt`; fixture real `wallapop_search.json`; test.

**Interfaces:**
- Produces: `object WallapopParser { fun parse(json: String, query: SearchQuery, nowMillis: Long): List<Product>? }`; `class WallapopSource(client, throttle)`.
- Petición: `GET https://api.wallapop.com/api/v3/search?keywords={text}&latitude={lat}&longitude={lon}&source=search_box&category_id=12465&subcategory_ids=11003&max_sale_price={max}&order_by={closest|most_relevance}` con cabeceras `X-DeviceOS: 0` y User-Agent móvil. Sin ubicación: Madrid (40.4168, −3.7038) y `most_relevance`, `distanceKm = null`.
- Item → Product: `data.section.payload.items[]`; id `wp:{id}`; imagen `images[0].urls.medium`; precio `price.amount`; url `https://es.wallapop.com/item/{web_slug}`; descartar `reserved.flag == true` y los que no tengan "Men" (id 11003) en `taxonomy` si `taxonomy` existe; talla = `SizeExtractor.fromText(title + " " + description)`; si la categoría de la consulta tiene `sizeKind` y el usuario tiene esa talla y la talla extraída existe y es distinta → descartar.
- [ ] **Step 1: Tests** fixture → 3 productos con url `https://es.wallapop.com/item/sudadera-…`; el de "Talla M" tiene size "M" y se descarta si el usuario tiene talla top "L"; distancia calculada con ubicación; JSON sin `data` → null.
- [ ] **Step 2–4.** Commit `feat: wallapop source`.

### Task 10: Fuente Vinted

**Files:** `VintedParser.kt`, `VintedSource.kt`, `VintedCookieProvider.kt`; fixture `vinted_search.json`; test.

**Interfaces:**
- Produces: `interface CookieProvider { suspend fun cookies(forceRefresh: Boolean): String? }`; `class WebViewVintedCookieProvider(context)` (carga `https://www.vinted.es/` en WebView invisible en el hilo principal, espera `onPageFinished` + 2 s, máx. 20 s, lee `CookieManager.getCookie`); `object VintedParser { fun parse(json, query, nowMillis): List<Product>? }`; `class VintedSource(client, cookies: CookieProvider, throttle, clock)`.
- Petición: `GET https://www.vinted.es/api/v2/catalog/items?search_text={text}&catalog_ids=5&price_to={max}&currency=EUR&order=relevance&per_page=40` con `Cookie`. 401/403 → renovar cookie una vez y reintentar; si vuelve a fallar → `Failed(BLOCKED)` y desactivada 1 h.
- Item → Product: `items[]`; id `vt:{id}`; precio `price` como objeto `{amount}` o texto; imagen `photo.url`; url `url`; talla `SizeExtractor.fromText(size_title)`; filtro de talla igual que Wallapop.
- [ ] **Step 1: Tests** parser con fixture (precio objeto y precio texto, uno sin foto → descartado); fuente con cookie provider falso y servidor falso (OkHttp `MockWebServer` 4.12.0): 403→403 → BLOCKED; 403→200 → Ok; segunda llamada en 1 h tras BLOCKED → BLOCKED sin petición.
- [ ] **Step 2–4.** Commit `feat: vinted source`.

### Task 11: Reserva de prendas

**Files:** `data/pool/ProductPoolRepository.kt`; test.

**Interfaces:**
- Consumes: `ProductSource` (T8–10), `QueryPlanner`, `ProductDao`, `ProfileRepository`, `SettingsRepository`.
- Produces:
  - `data class RefillReport(val added: Int, val failures: Map<Source, SourceFailure>)`
  - `class ProductPoolRepository(...) { suspend fun candidates(): List<Product>; suspend fun needsRefill(): Boolean; suspend fun refill(): RefillReport; suspend fun searchAll(query: SearchQuery): Pair<List<Product>, Map<Source, SourceFailure>>; suspend fun markSeen(p: Product, liked: Boolean); suspend fun purgeExpired(); suspend fun resetSeen() }`
- `refill`: plan → para cada consulta, cada fuente secuencialmente → upsert sin vistos/duplicados; guarda textos en `QueryHistoryEntity`. `searchAll` no guarda en la reserva.
- [ ] **Step 1: Tests** con fuentes falsas: una falla y otra devuelve 5 → `added` = 5 × 3 consultas sin duplicados, `failures` contiene la que falló; productos vistos no vuelven a `candidates`; `purgeExpired` borra los de > 3 días.
- [ ] **Step 2–4.** Commit `feat: product pool`.

### Task 12: Análisis de fotos con Gemini

**Files:** `data/style/StyleAnalyzer.kt`, `GeminiResponseParser.kt`, `GeminiStyleAnalyzer.kt`; test.

**Interfaces:**
- Produces: `interface StyleAnalyzer { suspend fun analyze(image: Bitmap): StyleResult }`; `StyleResult`, `FailureReason { NO_INTERNET, QUOTA_EXCEEDED, NO_TAGS, MISSING_KEY, UNKNOWN }`; `object GeminiResponseParser { fun parse(json: String): List<String> }`; `object GeminiRequest { fun body(base64Jpeg: String): String }`.
- REST: `POST https://generativelanguage.googleapis.com/v1beta/models/{BuildConfig.GEMINI_MODEL}:generateContent`, cabecera `x-goog-api-key`. `generationConfig.responseMimeType = "application/json"`, `responseSchema = {type: OBJECT, properties: {tags: {type: ARRAY, items: {type: STRING, enum: [ids]}}}}`. Prompt: "Analiza la prenda principal de la foto. Devuelve entre 3 y 7 etiquetas…". Imagen reducida a 1024 px, JPEG 85. HTTP 429 → QUOTA_EXCEEDED; IOException → NO_INTERNET.
- [ ] **Step 1: Tests** parser: `candidates[0].content.parts[0].text = "{\"tags\":[\"sudadera\",\"inventada\",\"sudadera\",\"negro\"]}"` → `[sudadera, negro]`; JSON roto → `[]`. Request body contiene `"responseMimeType":"application/json"` y todos los ids. Analyzer con clave "" → `Failure(MISSING_KEY)`.
- [ ] **Step 2–4.** Commit `feat: gemini style analyzer`.

### Task 13: Ubicación, AppContainer y navegación

**Files:** `data/location/LocationProvider.kt`, `AppContainer.kt`, `ClothifyApp.kt`, `ui/ClothifyRoot.kt`, `ui/Links.kt`.

**Interfaces:**
- Produces: `class LocationProvider(context) { suspend fun lastKnown(): LatLng? }` (null sin permiso; redondeo 2 decimales); `class AppContainer(context)` con propiedades `settings, profile, basket, pool, styleAnalyzer, location, recommender`; `fun openUrl(context, url): Boolean`; `ClothifyRoot(container)`: si `!onboardingDone` → Onboarding, si no NavHost con 4 pestañas (rutas `discover`, `search`, `basket`, `profile`).
- [ ] **Step 1:** Implementar; `MainActivity` usa `ClothifyRoot((application as ClothifyApp).container)`. Pantallas provisionales con su título.
- [ ] **Step 2:** `./gradlew assembleDebug` → OK. Commit `feat: app container and navigation`.

### Task 14: Bienvenida

**Files:** `ui/onboarding/*`, `ui/components/TagChips.kt`; test Robolectric Compose.

- Pasos de la spec §2.1. Tallas con `FilterChip`s (XS–XXL; 28–40 pares; 38–47). Precio `Slider` 10–300 paso 5. Ubicación: `rememberLauncherForActivityResult(RequestPermission)`. Estilos: cuadrícula de `StylePresets.all`, botón Empezar deshabilitado sin ninguno. Fotos: `PickMultipleVisualMedia(5)`; cada foto → `styleAnalyzer` → `ProfileEvent.OwnPhoto`; los fallos se ignoran con un aviso.
- Al terminar: `PresetChosen` por preset, guarda ajustes, `onboardingDone = true`, lanza `pool.refill()` en segundo plano.
- [ ] **Step 1: Test** `OnboardingViewModelTest`: elegir "Streetwear" y terminar → `streetwear` = 3.0 y `onboardingDone`.
- [ ] **Step 2–4.** Commit `feat: onboarding`.

### Task 15: Descubrir (swipe)

**Files:** `ui/discover/*`, `ui/components/SwipeableCard.kt`, `ProductCard.kt`, `ProductImage.kt`; tests.

**Interfaces:**
- `DiscoverUiState { Loading; Cards(stack: List<Product>, sourceWarning: String?); Empty(sourceWarning: String?) }`
- `DiscoverViewModel(pool, profile, basket, recommender)`: `onSwipe(product, liked)`, `onAddToBasket(product)`, `onRetry()`. Mantiene 3 cartas; tras cada swipe `markSeen` + `profile.apply(Liked/Disliked(tagIds))`; si `needsRefill()` → `refill()` (un solo refill a la vez). Si no hay candidatas tras refill → `Empty`.
- `SwipeableCard`: `Animatable` offset X con `detectDragGestures`; umbral 30 % del ancho → sale animada y llama `onSwiped(liked)`; rotación = offset/20 grados.
- `ProductCard`: imagen (Coil, placeholder), título, precio "27 €", vendedor, talla, "a 3 km", chips de etiquetas, botones ✕ ♥ 🛒; tocar abre `productUrl`.
- [ ] **Step 1: Tests** `DiscoverViewModelTest` (fakes): swipe derecha sube la puntuación y retira la carta; 🛒 añade a cesta sin retirar; pool vacío con fuentes fallando → `Empty`. Compose (Robolectric): pulsar ♥ llama `onSwipe(liked=true)`.
- [ ] **Step 2–4.** Commit `feat: discover screen with swipe`.

### Task 16: Buscar con foto

**Files:** `ui/search/*`; test.

- Estados: `Idle`, `Analyzing`, `Tags(tagIds, error: String?)`, `Searching`, `Results(products, warning)`. Botones Cámara (`TakePicturePreview`) y Galería (`PickVisualMedia`). Error de análisis → mensaje según `FailureReason` + "Reintentar" + selector manual (todas las etiquetas agrupadas). Buscar → `QueryPlanner`-independiente: `SearchQuery(text = StoreLinkBuilder.queryText sin "hombre"`, …) → `pool.searchAll` → `Similarity.rank`. Muestra "Usa 1 búsqueda de Google Shopping (quedan N hoy)". Botón "Buscar esto en las tiendas" → hoja con `StoreLinkBuilder.links`.
- [ ] **Step 1: Test** ViewModel: analyzer falso con `Success([sudadera, negro])` → `Tags`; buscar → `Results` ordenado por parecido; `Failure(QUOTA_EXCEEDED)` → `Tags([], error="Has agotado el análisis gratuito de hoy. Vuelve mañana o elige las etiquetas a mano.")`.
- [ ] **Step 2–4.** Commit `feat: photo search`.

### Task 17: Cesta y Perfil

**Files:** `ui/basket/*`, `ui/profile/*`; tests.

- Cesta: lista; "Comprar" → `openUrl(productUrl)`; fila de tiendas con `StoreLinkBuilder.links(tagIds, location)`; aviso "Puede que ya no esté disponible" si `addedAt` > 3 días; "No se pudo abrir el enlace" si `openUrl` devuelve false.
- Perfil: barras de puntuación (ordenadas desc., solo ≠ 0); tallas, precio, interruptor ubicación; "Búsquedas de Google Shopping: N hoy · M este mes"; "Añadir fotos de mi estilo"; "Volver a empezar las vistas"; "Borrar mis datos" (confirmación) → borra BD y ajustes.
- [ ] **Step 1: Tests** BasketViewModel quita productos; ProfileViewModel `clearAll` deja puntuaciones vacías y `onboardingDone = false`.
- [ ] **Step 2–4.** Commit `feat: basket and profile screens`.

### Task 18: CI, firma y README

**Files:** `.github/workflows/build.yml`, `README.md`.

- Workflow: `on: push` (todas las ramas) + `workflow_dispatch`. Ubuntu, JDK 17 (temurin), `android-actions/setup-android@v3`, `gradle/actions/setup-gradle@v4`. Pasos: decodificar `KEYSTORE_BASE64` si existe a `$RUNNER_TEMP/release.jks`; `./gradlew testDebugUnitTest assembleRelease` con env `GEMINI_API_KEY`, `SERPAPI_KEY`, `KEYSTORE_*`, `VERSION_CODE=${{ github.run_number }}`; subir `app-release.apk` como artefacto; en `main` crear release `v${{ github.run_number }}` con `softprops/action-gh-release@v2` (`permissions: contents: write`).
- `versionCode` desde env `VERSION_CODE` (por defecto 1), `versionName = "1.0.$versionCode"`.
- README en español: qué es; ⚠️ repo privado; obtener clave Gemini (aistudio.google.com) y SerpApi (serpapi.com); crear keystore (`keytool -genkeypair …`) y `base64`; añadir los 6 secretos; descargar e instalar el APK; aviso sobre Vinted/Wallapop no oficiales.
- [ ] **Step 1:** Verificación local: `./gradlew testDebugUnitTest assembleRelease` → OK; validar YAML con `python3 -c "import yaml…"`.
- [ ] **Step 2:** Commit `ci: build and release signed APK`.

### Task 19: Revisión final

- [ ] Ejecutar todas las pruebas y `assembleRelease`; repasar la spec sección por sección; push.
