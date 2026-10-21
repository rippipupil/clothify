# Clothify — Fase 1: diseño

**Fecha:** 2026-10-21 (revisado el mismo día: uso personal con productos reales)
**Estado:** pendiente de revisión

## 1. Propósito y contexto

Clothify es una app Android **de uso personal** (un único usuario: el dueño del
repositorio) para encontrar ropa de hombre. Aprende su estilo a partir de fotos,
estilos elegidos y swipes, le muestra **prendas reales con precio real** de
Wallapop, Vinted y Google Shopping (Amazon, AliExpress, Shein, Zalando…) y le lleva
a la ficha del producto para comprarla.

- **Restricciones:** solo Android, Kotlin + Jetpack Compose, **coste cero**
  (planes gratuitos), sin cuentas, sin servidor propio. País fijo: **España**
  (euros, dominios `.es`). Sección: **hombre**.
- **Instalación:** GitHub Actions compila el APK; el usuario lo descarga e instala.
  No necesita Android Studio.
- **No es una app para publicar:** usa APIs internas no oficiales de Vinted y
  Wallapop, aceptado por el usuario para uso personal y de bajo volumen,
  sabiendo que va contra sus condiciones de uso y puede romperse o bloquearse.

### Criterios de éxito

1. El APK se compila en GitHub, se instala en el móvil del usuario y se actualiza
   encima sin perder datos.
2. Descubrir muestra prendas reales de al menos dos de las tres fuentes, filtradas
   por hombre, talla (donde se pueda) y precio máximo.
3. Las recomendaciones cambian de forma visible al hacer swipe (se ve en Perfil).
4. Una foto de ropa devuelve prendas reales parecidas.
5. Desde la cesta, "Comprar" abre la ficha real del producto.
6. Si una fuente falla, la app sigue funcionando con las demás.

### Fuera de alcance en la Fase 1

- API oficial de afiliados de AliExpress/Amazon, otros países, login, iOS,
  tiendas físicas, comparador de la misma prenda entre tiendas, swipe arriba,
  "deshacer", notificaciones de bajadas de precio.
- Similitud visual con un modelo propio en el móvil (CLIP) → Fase 3.
- Búsqueda visual exacta tipo Google Lens.

## 2. Pantallas

Navegación inferior con 4 pestañas: **Descubrir · Buscar · Cesta · Perfil**.
La bienvenida aparece solo la primera vez.

### 2.1 Bienvenida (3 pasos)

1. **Tallas y precio:** talla de parte de arriba (XS–XXL), pantalón (cintura
   28–40) y calzado (EU 38–47); precio máximo (control deslizante, 10–300 €,
   por defecto 60 €). Todo opcional salvo el precio, que tiene valor por defecto.
2. **Ubicación (opcional):** se explica que sirve para ver segunda mano cerca en
   Wallapop; se pide permiso de ubicación aproximada. Se puede saltar.
3. **Tu estilo:** cuadrícula de estilos predefinidos (multi-selección, al menos 1)
   y botón opcional "Subir fotos de mi ropa" (hasta 5). Botón "Empezar".

### 2.2 Descubrir

- Pila de tarjetas. Cada tarjeta muestra: **foto, título, precio, tienda o
  vendedor, etiquetas de estilo**, y si existen **talla** y **distancia en km**.
- **Deslizar a la derecha** = me gusta. **Deslizar a la izquierda** = menos de esto.
  Botones ✕ / ♥ equivalentes.
- **Botón 🛒**: añade a la cesta (no cuenta como swipe; la tarjeta se queda).
- **Tocar la tarjeta**: abre la ficha real del producto.
- Estado "Buscando prendas…" mientras se rellena; estado "No hay más prendas por
  ahora" con botón "Buscar más" si todas las fuentes fallan o no devuelven nada.
- Aviso discreto si una fuente no responde ("Vinted no responde ahora").

### 2.3 Buscar con foto

1. Cámara o galería.
2. Gemini analiza la foto → **etiquetas como chips editables**.
3. Botón "Buscar" (indica "usa 1 búsqueda de Google Shopping" si está disponible).
4. Resultados: cuadrícula de tarjetas de las tres fuentes, ordenadas por parecido,
   cada una con botón 🛒 y que abre la ficha al tocarla.

Buscar con foto **no** modifica las puntuaciones del perfil.

### 2.4 Cesta

- Lista de prendas guardadas (foto, título, precio, tienda, etiquetas), con quitar.
- Botón **"Comprar"** → abre la ficha real.
- Fila **"Buscar parecidos en"**: Amazon · AliExpress · Shein · Vinted · Wallapop
  (enlaces de búsqueda construidos con las etiquetas de la prenda).
- Nota "puede que ya no esté disponible" en prendas guardadas hace más de 3 días.

### 2.5 Perfil

- **Tus estilos:** etiquetas con su puntuación (barras), de mayor a menor.
- Tallas, precio máximo, ubicación (activar/desactivar).
- **Búsquedas de Google Shopping restantes** hoy y este mes.
- "Añadir fotos de mi estilo".
- "Borrar mis datos": vacía todo y vuelve a la bienvenida.

## 3. Arquitectura

Patrón **MVVM**, inyección de dependencias manual con un `AppContainer`.

```
ui/              Pantallas Compose + un ViewModel por pantalla
  onboarding/ discover/ search/ basket/ profile/ components/
domain/          Lógica pura Kotlin (sin Android), probada con JUnit
  TagVocabulary, TitleTagger, ProfileScorer, Recommender, QueryPlanner,
  SearchBudget, StoreLinkBuilder, Similarity
data/
  sources/       ProductSource (interfaz) + WallapopSource, VintedSource,
                 GoogleShoppingSource; parsers de cada respuesta
  pool/          ProductPoolRepository: rellena, guarda y caduca prendas
  local/         Room (tablas §4.3) + DataStore (ajustes §4.4)
  style/         StyleAnalyzer (interfaz) + GeminiStyleAnalyzer
  location/      LocationProvider
AppContainer     Crea y conecta todas las piezas
```

`domain/` no depende de Android ni de `data/`.

### Librerías (todas gratuitas)

Kotlin, Jetpack Compose + Material 3, Navigation Compose, Room, DataStore, Coil,
kotlinx.serialization, OkHttp (peticiones HTTP), Android WebView + `CookieManager`
(cookie de Vinted), SDK cliente de Gemini o llamada REST a la Gemini API con clave
de Google AI Studio, servicios de ubicación de Google Play, JUnit.

## 4. Modelo de datos

### 4.1 Vocabulario de etiquetas (`TagVocabulary`)

Lista cerrada de ~80 etiquetas con `id` estable, grupo, nombre en español y
**sinónimos en español e inglés** (para etiquetar títulos). Grupos:

| Grupo | Ejemplos |
|---|---|
| `CATEGORY` | camiseta, camisa, polo, sudadera, jersey, chaqueta, cazadora, abrigo, vaqueros, pantalón, chino, cargo, shorts, zapatillas, botas, gorra |
| `STYLE` | streetwear, minimalista, vintage, y2k, old money, deportivo, formal, casual, grunge, preppy, workwear, techwear |
| `COLOR` | negro, blanco, gris, beige, marrón, tonos tierra, azul marino, azul claro, verde, rojo, estampado |
| `FIT_MATERIAL` | oversize, slim, recto, ancho, cropped, denim, punto, cuero, lino, pana |

Estilos predefinidos de la bienvenida = conjuntos de etiquetas
(ej. "Streetwear" → `streetwear`, `oversize`, `sudadera`, `zapatillas`).

### 4.2 Producto

```kotlin
data class Product(
    val id: String,             // "{fuente}:{id en la fuente}"
    val source: Source,         // WALLAPOP, VINTED, GOOGLE_SHOPPING
    val title: String,
    val imageUrl: String,
    val priceEur: Double,
    val sellerName: String,     // "Wallapop", "Vinted" o la tienda de Shopping
    val productUrl: String,     // ficha real
    val size: String?,          // si la fuente lo da
    val distanceKm: Double?,    // solo Wallapop con ubicación
    val tagIds: Set<String>,
    val fetchedAt: Instant,
)
```

**Etiquetado sin gastar IA (`TitleTagger`):** `tagIds` = etiquetas de la búsqueda
que encontró la prenda ∪ etiquetas cuyos sinónimos aparecen en el título
(sin tildes, minúsculas, palabra completa).

### 4.3 Base de datos local (Room)

| Tabla | Campos clave | Uso |
|---|---|---|
| `product` | todos los de `Product` | Reserva de prendas por ver (caché) |
| `tag_score` | `tagId`, `score` | Perfil de gustos |
| `basket_item` | `productId`, copia del producto, `addedAt` | Cesta (no caduca) |
| `seen_product` | `productId`, `liked`, `seenAt` | No repetir tarjetas |
| `search_usage` | `date`, `count` | Presupuesto de SerpApi |

### 4.4 Ajustes (DataStore)

`onboardingDone`, `topSize`, `waistSize`, `shoeSize`, `maxPriceEur`,
`locationEnabled`, `lastLatitude`, `lastLongitude` (redondeadas a 2 decimales).

## 5. Fuentes de productos

```kotlin
interface ProductSource {
    val source: Source
    suspend fun search(query: SearchQuery): SourceResult
}
data class SearchQuery(
    val text: String,           // "sudadera oversize streetwear"
    val tagIds: Set<String>,
    val categoryTag: String?,   // para elegir qué talla aplicar
    val maxPriceEur: Double,
    val sizes: UserSizes,
    val location: LatLng?,
)
sealed interface SourceResult {
    data class Ok(val products: List<Product>) : SourceResult
    data class Failed(val reason: SourceFailure) : SourceResult
}
enum class SourceFailure { NO_INTERNET, BLOCKED, BUDGET_EXHAUSTED, BAD_RESPONSE }
```

### 5.1 Wallapop (`WallapopSource`)

- API interna de búsqueda de `api.wallapop.com` (v3), con palabras clave,
  categoría de moda, precio máximo, latitud/longitud y orden por distancia.
- Sin ubicación: se usa el centro de España y se omite la distancia.
- Talla: filtro si la API lo permite; si no, se usa el atributo de talla de la
  respuesta para descartar tallas distintas cuando existan.
- El formato exacto (endpoint, parámetros, cabeceras) se determina **durante la
  implementación** inspeccionando la web y se fija con una respuesta de ejemplo
  guardada en las pruebas.

### 5.2 Vinted (`VintedSource`)

- `https://www.vinted.es/api/v2/catalog/items` con `search_text`, catálogo de
  hombre, ids de talla y `price_to`.
- **Cookie de sesión:** un `WebView` invisible carga `https://www.vinted.es/`, se
  leen sus cookies con `CookieManager` y se reutilizan en OkHttp. Si la API
  responde 401/403, se renueva la cookie una vez y se reintenta; si vuelve a
  fallar → `BLOCKED`.
- Correspondencia tallas del usuario → ids de talla de Vinted: tabla en código,
  rellenada durante la implementación.

### 5.3 Google Shopping (`GoogleShoppingSource`)

- SerpApi, motor `google_shopping`, `q = "{texto} hombre"`, `gl=es`, `hl=es`,
  filtro de precio máximo.
- Resultado: título, precio, tienda, miniatura y enlace a la ficha.
- **Presupuesto (`SearchBudget`):** máx. 8 búsquedas/día y 240/mes (plan gratuito:
  250/mes, 50/hora). Agotado → `BUDGET_EXHAUSTED`, sin llamar.
- Talla: Google Shopping no filtra por talla; se acepta.

### 5.4 Cortesía con las fuentes no oficiales

- Como mucho 1 petición cada 3 s por fuente y solo al rellenar la reserva o al
  buscar con foto. Una página de resultados por búsqueda.
- User-Agent de navegador móvil normal. Sin paralelizar peticiones a la misma
  fuente.

## 6. Recomendación

### 6.1 Puntuaciones (`ProfileScorer`)

| Evento | Cambio en cada etiqueta implicada |
|---|---|
| Elegir un estilo en la bienvenida | +3 |
| Etiqueta detectada en una foto propia (bienvenida/Perfil) | +2 |
| Swipe derecha | +1 |
| Swipe izquierda | −0,5 |

Puntuaciones limitadas a **[−10, +10]**.

### 6.2 Qué buscar (`QueryPlanner`)

Cuando quedan **menos de 15** prendas sin ver y no caducadas, se preparan
**3 búsquedas**:

- 2 de **explotación**: 1 etiqueta `CATEGORY` + hasta 2 de `STYLE`/`COLOR`/
  `FIT_MATERIAL`, elegidas al azar ponderado entre las de puntuación positiva más
  alta (ej. "sudadera oversize negro").
- 1 de **exploración**: categoría al azar + estilo con puntuación ≥ 0 elegido al
  azar.
- No se repite la misma combinación de texto en 24 h.

Cada búsqueda se lanza en las tres fuentes (Google Shopping solo si hay
presupuesto). Las prendas nuevas se guardan en `product` (sin duplicados, sin
las ya vistas, sin las de precio > máximo).

### 6.3 Siguiente tarjeta (`Recommender`)

1. Candidatas = reserva − vistas − caducadas (`fetchedAt` > 3 días).
2. Nota = suma de puntuaciones de sus etiquetas ÷ √(nº etiquetas).
3. Probabilidad **0,8** → mayor nota (desempate aleatorio); **0,2** → al azar.
4. Se intenta no mostrar más de 3 seguidas de la misma fuente.

Generador aleatorio inyectado para pruebas deterministas. 3 tarjetas precargadas.

### 6.4 Parecido en "Buscar con foto" (`Similarity`)

Jaccard entre etiquetas de la foto y de la prenda; se muestran ordenadas de mayor
a menor (todas las devueltas por las fuentes, máximo 60).

### 6.5 Caducidad

Al abrir la app se borran de `product` las prendas con más de 3 días. La cesta
guarda su propia copia y no caduca.

## 7. Análisis de fotos (`StyleAnalyzer`)

```kotlin
interface StyleAnalyzer {
    suspend fun analyze(image: Bitmap): StyleResult
}
sealed interface StyleResult {
    data class Success(val tagIds: List<String>) : StyleResult
    data class Failure(val reason: FailureReason) : StyleResult
}
enum class FailureReason { NO_INTERNET, QUOTA_EXCEEDED, NO_TAGS, UNKNOWN }
```

- **`GeminiStyleAnalyzer`:** Gemini API (capa gratuita, clave de Google AI Studio).
  Imagen reducida a máx. 1024 px. Salida **JSON estructurada** con `tags` como lista
  de un `enum` = ids del vocabulario.
- `GeminiResponseParser` (lógica pura) descarta ids desconocidos y duplicados;
  ninguno válido → `NO_TAGS`.
- Fase 3: `ClipStyleAnalyzer` con la misma interfaz.

## 8. Enlaces "Buscar parecidos" (`StoreLinkBuilder`)

Texto = categoría + hasta 3 etiquetas + "hombre", codificado para URL.

| Tienda | Plantilla |
|---|---|
| Amazon | `https://www.amazon.es/s?k={q}` |
| AliExpress | `https://es.aliexpress.com/w/wholesale-{q-con-guiones}.html` |
| Shein | `https://es.shein.com/pdsearch/{q}/` |
| Vinted | `https://www.vinted.es/catalog?search_text={q}` |
| Wallapop | `https://es.wallapop.com/app/search?keywords={q}` (+ `latitude`/`longitude` si hay ubicación) |

Las plantillas se comprueban a mano en la implementación y se fijan en pruebas.
Todos los enlaces (fichas y búsquedas) se abren con `Intent.ACTION_VIEW`: app de
la tienda si está instalada, si no el navegador.

## 9. Ubicación

Permiso aproximado (`ACCESS_COARSE_LOCATION`) solo si el usuario lo activa.
Última ubicación conocida al abrir la app, guardada redondeada (~1 km). Se usa
solo para Wallapop.

## 10. Compilación, instalación y claves

- **GitHub Actions** (`.github/workflows/build.yml`): en cada push a cualquier
  rama ejecuta las pruebas y compila el APK **release** firmado (descargable como
  artefacto del workflow). En cada push a `main` además lo publica como GitHub
  Release (`clothify-vN.apk`, N = número de ejecución). Si las pruebas fallan, no
  hay APK.
- **Firma:** un keystore generado una vez, guardado como secreto en base64
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), para que
  cada actualización se instale encima sin perder datos.
- **Claves de API:** `GEMINI_API_KEY` y `SERPAPI_KEY` como secretos de GitHub,
  inyectadas en `BuildConfig` al compilar. En local se leen de `local.properties`
  (no se sube al repositorio).
- El README explica: cómo obtener ambas claves gratis, cómo añadir los secretos,
  cómo descargar e instalar el APK (permitir "instalar apps desconocidas").
- ⚠️ El APK contiene las claves: el repositorio debe ser **privado** y el APK no
  se comparte.

## 11. Errores

| Situación | Comportamiento |
|---|---|
| Una fuente falla | Se sigue con las demás; aviso "X no responde ahora" |
| Todas las fuentes fallan | Se muestran las prendas guardadas; si no hay, "No hay más prendas por ahora" + "Buscar más" |
| Vinted bloquea (401/403 tras renovar cookie) | Se marca como no disponible durante 1 h |
| Presupuesto de SerpApi agotado | Solo Wallapop y Vinted hasta el día/mes siguiente; visible en Perfil |
| Gemini falla / sin internet | Mensaje + "Reintentar" + "Elegir etiquetas a mano" |
| Cuota de Gemini agotada | "Vuelve mañana o elige las etiquetas a mano" |
| Gemini sin etiquetas válidas | Selector manual de etiquetas |
| Permiso de ubicación denegado | Wallapop sin distancia; se puede activar en Perfil |
| Imagen no carga | Imagen genérica |
| Ningún navegador/app para un enlace | "No se pudo abrir el enlace" |

## 12. Pruebas

- **Unitarias (JUnit):** `TitleTagger`, `ProfileScorer` (sumas y límites),
  `QueryPlanner` (explotación/exploración, no repetir en 24 h), `Recommender`
  (vistas, caducadas, 80/20 con aleatorio fijo, variedad de fuentes),
  `Similarity`, `SearchBudget` (día/mes), `StoreLinkBuilder` (cada tienda, con y
  sin ubicación, caracteres especiales), `GeminiResponseParser`.
- **Parsers de fuentes:** respuestas reales de ejemplo de Wallapop, Vinted y
  SerpApi guardadas en `test/resources`; las pruebas comprueban que se convierten
  en `Product` correctos y que una respuesta inesperada da `BAD_RESPONSE` sin
  romper la app.
- **Interfaz (Compose UI test):** swipe derecha/izquierda actualiza el estado;
  🛒 añade a la cesta.
- **Manual:** lista de comprobación en el móvil para cada pantalla y cada caso de
  la sección 11.
- Las pruebas unitarias se ejecutan en GitHub Actions antes de compilar el APK.

## 13. Fases siguientes (fuera de esta especificación)

- **Fase 2 — Más fuentes oficiales:** API de afiliados de AliExpress; alertas de
  precio en prendas de la cesta.
- **Fase 3 — IA en el móvil:** `ClipStyleAnalyzer` con un modelo tipo CLIP para
  similitud visual sin internet.
