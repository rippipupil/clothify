# Clothify — Fases 2 y 3 + pendientes: diseño

**Fecha:** 2026-10-21
**Estado:** aprobado por el usuario ("haz ya toda la app entera"); amplía `2026-10-21-clothify-fase1-design.md`.

## 1. Alcance

1. **Fase 2a — AliExpress oficial:** nueva fuente de productos con la API de afiliados de AliExpress.
2. **Fase 2b — Alertas de precio:** comprobar a diario los precios de la cesta y avisar si bajan o si la prenda ya no está.
3. **Fase 3 — IA en el móvil:** analizador de fotos CLIP sin internet y orden por parecido visual en "Buscar con foto".
4. **Pendientes menores de la Fase 1:** bienvenida limpia tras "Borrar mis datos", foto de cámara a resolución completa, aviso "Sin conexión" sin repeticiones.

Todo sigue siendo gratis, de uso personal y sin servidor propio.

## 2. AliExpress (`AliExpressSource`)

- API de afiliados (`https://api-sg.aliexpress.com/sync`, método `aliexpress.affiliate.product.query`).
- Secretos nuevos: `ALIEXPRESS_APP_KEY`, `ALIEXPRESS_APP_SECRET`, `ALIEXPRESS_TRACKING_ID` → `BuildConfig`. Si falta la clave o el secreto: `DISABLED` sin petición (no se muestra aviso).
- Firma (`AliExpressSigner`): parámetros ordenados por nombre, concatenados `nombre+valor`, HMAC-SHA256 con el secreto, hexadecimal en mayúsculas; `sign_method=sha256`, `timestamp` en milisegundos.
- Parámetros: `keywords = "{texto} hombre"`, `target_currency=EUR`, `target_language=ES`, `ship_to_country=ES`, `page_size=40`, `max_sale_price` en céntimos, `tracking_id`.
- Producto: id `ae:{product_id}`, título `product_title`, precio `target_sale_price`, imagen `product_main_image_url`, enlace `promotion_link` (o `product_detail_url`), vendedor "AliExpress". Precio > máximo → descartado. Etiquetas = consulta ∪ título.
- Respuesta `error_response` con clave/firma inválida → `DISABLED`; otro error → `BAD_RESPONSE`.
- Ritmo: ≥ 1 s entre peticiones.
- No se ha podido probar en vivo desde el entorno de desarrollo (conexión cortada): el formato sigue la documentación pública y queda fijado en pruebas.

## 3. Alertas de precio

- **Ajuste** `priceAlertsEnabled` (por defecto desactivado). Se activa desde un aviso en la Cesta o un interruptor en Perfil; en Android 13+ pide el permiso de notificaciones.
- **Comprobación** (`PriceChecker`) para prendas de la cesta de:
  - Wallapop: `GET https://api.wallapop.com/api/v3/items/{id}` → `price.cash.amount`; 404 → ya no disponible.
  - AliExpress: `aliexpress.affiliate.productdetail.get` con `product_ids` → `target_sale_price`; sin producto → ya no disponible.
  - Vinted y Google Shopping: no se comprueban (Vinted exige la cookie del WebView, que no se puede usar en segundo plano; Google Shopping no tiene id estable). Se indica en la cesta.
- **Programación:** WorkManager, cada 12 h, solo con internet.
- **Base de datos v2:** `basket_item` añade `currentPriceEur REAL` (nulo = sin comprobar), `unavailable INTEGER NOT NULL DEFAULT 0`, `checkedAtMillis INTEGER` (nulo). Migración 1→2 sin pérdida de datos.
- **Aviso:** una notificación por prenda que baja ("Ha bajado: 30 € → 24 €", al tocar abre la ficha) y una por prenda que deja de estar disponible.
- **Cesta:** muestra "Antes 30 € · ahora 24 €" y "Ya no está disponible"; en Vinted/Google Shopping, "Sin aviso de precio".

## 4. IA en el móvil (Fase 3)

- **Modelo:** CLIP ViT-B/32 cuantizado (`Xenova/clip-vit-base-patch32`, `onnx/vision_model_quantized.onnx`, ~89 MB) con ONNX Runtime para Android.
  - No se guarda en git: GitHub Actions lo descarga en `app/src/main/assets/clip/` al compilar (con caché). Una compilación local sin el modelo funciona; el análisis local queda "no disponible".
- **Etiquetas:** `assets/clip/tag_embeddings.json` con el vector de texto CLIP (512 dimensiones, normalizado) de cada etiqueta del vocabulario, generado con `tools/clip/embed_tags.py` (indicaciones en inglés, p. ej. "a photo of a sweatshirt or hoodie"). Una prueba exige que estén todas las etiquetas.
- **Preprocesado:** escala el lado corto a 224, recorte central 224×224, RGB normalizado con media (0.48145466, 0.4578275, 0.40821073) y desviación (0.26862954, 0.26130258, 0.27577711), formato NCHW.
- **`ClipStyleAnalyzer`:** vector de la imagen → similitud coseno con cada etiqueta → la mejor `CATEGORY`, el mejor `COLOR`, el mejor `STYLE` y el mejor `FIT_MATERIAL` (4 etiquetas). Sin modelo → `Failure(MODEL_UNAVAILABLE)`.
- **`FallbackStyleAnalyzer`:** usa Gemini; si falla por cualquier motivo y CLIP está disponible, usa CLIP. Sin clave de Gemini, usa CLIP directamente. El resultado indica qué analizador respondió para mostrar "Analizado en el móvil".
- **Parecido visual en "Buscar con foto":** tras mostrar los resultados por etiquetas, si hay foto y modelo, se descargan las miniaturas de los 24 primeros, se calcula su vector y se reordenan por similitud coseno con la foto ("Ordenado por parecido visual"). Si algo falla, se queda el orden por etiquetas.

## 5. Pendientes menores

- `OnboardingViewModel` vuelve a su estado inicial al terminar, para que tras "Borrar mis datos" la bienvenida empiece limpia.
- "Hacer foto" usa `TakePicture` con un `FileProvider` (foto completa en la caché de la app) en vez de la miniatura.
- `sourceWarning` no repite "Sin conexión".

## 6. Pruebas

Unitarias: firma de AliExpress (vector conocido), parser y fuente AliExpress (MockWebServer), parser de artículo Wallapop (respuesta real guardada), `PriceChecker` (bajadas, subidas, retiradas, fuentes no comprobables), migración 1→2 (Room `MigrationTestHelper`), preprocesado CLIP, selección de etiquetas por coseno, `FallbackStyleAnalyzer`, reordenación visual, embeddings completos, reinicio de la bienvenida, aviso sin repeticiones.
