# Clothify — Fase 1: diseño

**Fecha:** 2026-10-21
**Estado:** pendiente de revisión

## 1. Propósito y contexto

Clothify es una app Android que aprende el estilo de ropa del usuario a partir de
sus fotos, estilos elegidos y swipes, le muestra prendas parecidas y le lleva a
comprarlas en Amazon, AliExpress, Shein, Vinted o Wallapop.

- **Para quién:** proyecto personal del autor, para uso propio y para **aprender a
  programar** (nivel de partida: ninguno o muy poco).
- **Restricciones:** solo Android, Kotlin + Jetpack Compose, **coste cero**
  (solo servicios gratuitos), sin cuentas de usuario.
- **Prioridad de la Fase 1:** que la experiencia completa funcione (fotos → gustos
  → swipe → cesta → tiendas), aunque los productos vengan de un catálogo de prueba.

### Criterios de éxito

1. La app se instala y ejecuta en un móvil Android o en el emulador.
2. El usuario sube una foto de ropa y recibe tarjetas de prendas parecidas.
3. Las recomendaciones cambian de forma visible al hacer swipe (se ve en Perfil).
4. Desde la cesta se abre la búsqueda de la prenda en cada tienda disponible en su
   país, y en Wallapop ordenada por cercanía si dio permiso de ubicación.
5. La lógica principal tiene pruebas automáticas que pasan.

### Fuera de alcance en la Fase 1

- Productos y precios reales de las tiendas (APIs de afiliados) → Fase 2.
- Similitud visual con un modelo propio en el móvil (CLIP) → Fase 3.
- Login, sincronización entre dispositivos, iOS, tiendas físicas, gastos de envío
  por prenda, comparador de precios entre tiendas, swipe hacia arriba y "deshacer".

## 2. Pantallas

Navegación inferior con 4 pestañas: **Descubrir · Buscar · Cesta · Perfil**.
La bienvenida aparece solo la primera vez, antes de las pestañas.

### 2.1 Bienvenida (3 pasos)

1. **País:** detectado del idioma/región del móvil; editable con una lista.
2. **Ubicación (opcional):** se explica para qué sirve (segunda mano cerca) y se
   pide el permiso de ubicación aproximada. Se puede saltar.
3. **Tu estilo:** cuadrícula de estilos predefinidos (multi-selección) y botón
   opcional "Subir fotos de mi ropa" (hasta 5 fotos). Botón "Empezar".

### 2.2 Descubrir

- Pila de tarjetas. Cada tarjeta muestra: **foto, nombre, precio en la moneda del
  usuario, tienda (logo/nombre), etiquetas de estilo** y la marca **"Ejemplo"**
  (catálogo de prueba).
- **Deslizar a la derecha** = me gusta. **Deslizar a la izquierda** = menos de esto.
  Botones ✕ / ♥ equivalentes para accesibilidad.
- **Botón 🛒** en la tarjeta: añade la prenda a la cesta (no cuenta como swipe; la
  tarjeta se queda).
- Si no quedan prendas por ver: "¡Lo has visto todo!" + botón "Volver a empezar"
  (borra el historial de vistas, no las puntuaciones).

### 2.3 Buscar con foto

1. Cámara o galería.
2. Se analiza la foto y se muestran las **etiquetas detectadas como chips
   editables** (quitar / añadir desde el vocabulario).
3. Resultados: cuadrícula de tarjetas (foto, precio, tienda, etiquetas) de las
   prendas del catálogo ordenadas por parecido, cada una con botón 🛒.
4. Botón **"Buscar esto en las tiendas"** que muestra directamente los enlaces de
   tiendas para esas etiquetas.

Buscar con foto **no** modifica las puntuaciones del perfil.

### 2.4 Cesta

- Lista de prendas guardadas (foto, nombre, precio, etiquetas) con opción de quitar.
- Cada prenda tiene un botón por tienda disponible en el país del usuario; cada uno
  abre la búsqueda equivalente en esa tienda.

### 2.5 Perfil

- **Tus estilos:** etiquetas con su puntuación (barras), ordenadas de mayor a menor.
  Sirve para ver cómo aprende la app.
- País y ubicación (cambiar país; activar/desactivar ubicación).
- Botón "Añadir fotos de mi estilo" (igual que en la bienvenida).
- "Borrar mis datos": vacía puntuaciones, cesta, historial y vuelve a la bienvenida.

## 3. Arquitectura

Patrón **MVVM** recomendado por Google, con inyección de dependencias manual
mediante un `AppContainer` (sin Hilt, para que sea más fácil de entender).

```
ui/            Pantallas Compose + un ViewModel por pantalla
  onboarding/  discover/  search/  basket/  profile/  components/
domain/        Lógica pura en Kotlin (sin Android) → fácil de probar
  Recommender, ProfileScorer, StoreLinkBuilder, CurrencyConverter, TagVocabulary
data/
  catalog/     CatalogRepository (lee assets/catalog.json)
  local/       Room: TagScore, BasketItem, SeenProduct; DataStore: ajustes
  style/       StyleAnalyzer (interfaz) + GeminiStyleAnalyzer
  location/    LocationProvider
AppContainer   Crea y conecta todas las piezas
```

Regla: `domain/` no depende de Android ni de `data/`; recibe datos y devuelve
resultados. Así la lógica se prueba con JUnit sin emulador.

### Librerías (todas gratuitas)

Kotlin, Jetpack Compose + Material 3, Navigation Compose, Room, DataStore
(Preferences), Coil (imágenes), kotlinx.serialization (JSON), Firebase AI Logic
(Gemini Developer API, plan gratuito Spark), servicios de ubicación de Google Play
(`FusedLocationProviderClient`), JUnit para pruebas.

## 4. Modelo de datos

### 4.1 Vocabulario de etiquetas (`TagVocabulary`)

Lista cerrada de ~80 etiquetas, cada una con `id` estable, grupo, nombre en español
y en inglés. Grupos:

| Grupo | Ejemplos |
|---|---|
| `CATEGORY` (tipo de prenda) | camiseta, camisa, sudadera, jersey, chaqueta, abrigo, vaqueros, pantalón, falda, vestido, zapatillas, botas, bolso, gorra |
| `STYLE` | streetwear, minimalista, vintage, y2k, old money, deportivo, boho, formal, casual, grunge, preppy |
| `COLOR` | negro, blanco, gris, beige, marrón, tonos tierra, azul marino, azul claro, verde, rojo, rosa, estampado |
| `FIT_MATERIAL` | oversize, slim, cropped, ancho, denim, punto, cuero, lino, algodón |

Vive en el código (`domain/`) como una lista Kotlin; es la única fuente de verdad.

### 4.2 Producto (catálogo de prueba)

```kotlin
data class Product(
    val id: String,
    val title: String,          // en español
    val imageUrl: String,       // Unsplash/Pexels (licencia libre)
    val priceEur: Double,       // precio de ejemplo en euros
    val store: Store,           // AMAZON, ALIEXPRESS, SHEIN, VINTED, WALLAPOP
    val secondHand: Boolean,
    val tagIds: List<String>,   // ids del vocabulario, 3–7 por prenda
)
```

- `assets/catalog.json` con **~150 prendas**, repartidas entre estilos y tiendas.
- Las fotos se cargan por URL desde Unsplash/Pexels (licencias que permiten uso
  gratuito); se guarda el crédito del autor en el JSON.
- Al cargar, se descartan las etiquetas que no existan en el vocabulario.

### 4.3 Base de datos local (Room)

| Tabla | Campos | Uso |
|---|---|---|
| `tag_score` | `tagId` (PK), `score: Double` | Perfil de gustos |
| `basket_item` | `productId` (PK), `addedAt` | Cesta |
| `seen_product` | `productId` (PK), `liked: Boolean`, `seenAt` | No repetir tarjetas |

### 4.4 Ajustes (DataStore)

`countryCode`, `onboardingDone`, `locationEnabled`, `lastLatitude`,
`lastLongitude` (redondeadas a 2 decimales ≈ 1 km).

## 5. Lógica de recomendación

### 5.1 Puntuaciones (`ProfileScorer`)

| Evento | Cambio en cada etiqueta implicada |
|---|---|
| Elegir un estilo en la bienvenida | +3 |
| Etiqueta detectada en una foto propia (bienvenida/Perfil) | +2 |
| Swipe derecha | +1 |
| Swipe izquierda | −0,5 |

Puntuaciones limitadas a **[−10, +10]**. Un estilo predefinido de la bienvenida se
traduce a sus etiquetas (ej. "Streetwear" → `streetwear`, `oversize`, `sudadera`,
`zapatillas`).

### 5.2 Siguiente tarjeta (`Recommender`)

1. Candidatas = catálogo − prendas ya vistas.
2. Nota de una prenda = suma de las puntuaciones de sus etiquetas ÷ √(nº etiquetas)
   (para no favorecer prendas con muchas etiquetas).
3. Con probabilidad **0,8** se elige la candidata de mayor nota (desempate
   aleatorio); con **0,2** una candidata al azar (exploración).
4. Sin candidatas → estado "lo has visto todo".

El generador aleatorio se inyecta en `Recommender` para que las pruebas sean
deterministas. La pila de Descubrir mantiene 3 tarjetas precargadas.

### 5.3 Parecido para "Buscar con foto"

Parecido = nº de etiquetas compartidas ÷ nº de etiquetas de la unión (Jaccard).
Se muestran las 20 prendas con mayor parecido > 0, ordenadas de mayor a menor.

## 6. Análisis de fotos (`StyleAnalyzer`)

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

- **`GeminiStyleAnalyzer`** (Fase 1): usa Firebase AI Logic con un modelo Gemini
  de la capa gratuita. La imagen se reduce a máx. 1024 px antes de enviarla.
  El prompt pide **salida JSON estructurada** con un esquema cuyo campo `tags` es
  una lista de valores de un `enum` = ids del vocabulario.
- La respuesta se valida en código (`GeminiResponseParser`, lógica pura): se
  descartan ids desconocidos y duplicados; si no queda ninguno → `NO_TAGS`.
- **Fase 3** añadirá `ClipStyleAnalyzer` con la misma interfaz; el resto de la app
  no cambia.
- Aviso al usuario (bienvenida y pantalla de búsqueda): las fotos se envían a
  Google para analizarlas; en el plan gratuito Google puede usarlas para mejorar sus
  productos.

## 7. Enlaces a tiendas (`StoreLinkBuilder`)

Entrada: etiquetas de la prenda, país, ubicación opcional.
Salida: lista de `(Store, Uri)` solo para tiendas disponibles en ese país.

1. **Texto de búsqueda:** categoría + hasta 3 etiquetas más (estilo, color,
   corte/material), en español si el país es hispanohablante y en inglés en el
   resto. Ej.: "chaqueta vaquera oversize azul claro".
2. **Plantillas por tienda** (dominio según el país):

| Tienda | Plantilla (ejemplo España) |
|---|---|
| Amazon | `https://www.amazon.es/s?k={q}` |
| AliExpress | `https://es.aliexpress.com/w/wholesale-{q-con-guiones}.html` |
| Shein | `https://es.shein.com/pdsearch/{q}/` |
| Vinted | `https://www.vinted.es/catalog?search_text={q}` |
| Wallapop | `https://es.wallapop.com/app/search?keywords={q}&latitude={lat}&longitude={lon}` |

3. **Países de la Fase 1:** España, Portugal, Francia, Italia, Alemania, Reino
   Unido, Estados Unidos y México. Una tabla indica, por país, la moneda, el idioma
   de búsqueda y el dominio de cada tienda (o "no disponible"; p. ej. Wallapop solo
   en ES, IT y PT).
4. Sin ubicación, Wallapop se abre sin `latitude`/`longitude`.
5. Los formatos exactos de URL se **comprueban a mano durante la implementación**
   (abriéndolos en el navegador) y se fijan en pruebas.
6. Se abren con `Intent.ACTION_VIEW`: Android usa la app de la tienda si está
   instalada; si no, el navegador.

### Moneda (`CurrencyConverter`)

Tabla fija de tipos de cambio EUR → USD, GBP, MXN dentro de la app (sin internet).
Los precios se muestran formateados según el país.

## 8. Ubicación

- Permiso **aproximado** (`ACCESS_COARSE_LOCATION`) solicitado solo si el usuario
  activa la opción.
- Se obtiene la última ubicación conocida al abrir la app y se guarda redondeada.
- Solo se usa para construir el enlace de Wallapop (y Vinted si en la
  implementación se confirma un parámetro de cercanía).

## 9. Errores

| Situación | Comportamiento |
|---|---|
| Sin internet / fallo de Gemini | Mensaje claro + "Reintentar" + "Elegir etiquetas a mano" |
| Cuota gratuita agotada | "Vuelve mañana o elige las etiquetas a mano" |
| Gemini sin etiquetas válidas | Selector manual de etiquetas |
| Permiso de ubicación denegado | Todo funciona; Wallapop sin ubicación; se puede activar en Perfil |
| Imagen de producto no carga | Imagen genérica (placeholder) |
| `catalog.json` dañado | Se descartan las prendas no válidas; si no queda ninguna, mensaje de error |
| Ninguna app/navegador para el enlace | Mensaje "No se pudo abrir el enlace" |

## 10. Pruebas

- **Unitarias (JUnit, `domain/` y parser):** `ProfileScorer` (sumas y límites),
  `Recommender` (exclusión de vistas, 80/20 con aleatorio fijo, catálogo agotado),
  parecido Jaccard, `StoreLinkBuilder` (cada tienda × varios países, con y sin
  ubicación, codificación de caracteres), `CurrencyConverter`,
  `GeminiResponseParser`, carga y validación del catálogo.
- **Interfaz (Compose UI test):** swipe derecha/izquierda en Descubrir actualiza el
  estado; botón 🛒 añade a la cesta.
- **Manual:** lista de comprobación en el emulador/móvil para cada pantalla y cada
  caso de error de la sección 9.

## 11. Forma de trabajo

- El código se escribe en este repositorio con comentarios didácticos y se sube a
  GitHub; el usuario lo abre y ejecuta en **Android Studio**.
- Requisitos del usuario: Android Studio, un proyecto gratuito de Firebase (plan
  Spark) con AI Logic activado y su archivo `google-services.json` (no se sube al
  repositorio; se documentan los pasos en el README).
- Cada paso del plan de implementación incluirá qué se aprende en él.

## 12. Fases siguientes (fuera de esta especificación)

- **Fase 2 — Productos reales:** API de afiliados de AliExpress; Amazon cuando se
  cumplan los requisitos de su programa; probablemente un pequeño backend gratuito
  que refresque el catálogo.
- **Fase 3 — IA en el móvil:** `ClipStyleAnalyzer` con un modelo tipo CLIP para
  similitud visual sin internet.
