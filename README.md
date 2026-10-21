# Clothify

App Android **personal** para encontrar ropa de hombre. Aprende tu estilo a partir de tus fotos, los
estilos que eliges y tus swipes, y te enseña prendas reales con precio real de **Wallapop**,
**Vinted**, **AliExpress** y **Google Shopping** (Amazon, Shein, Zalando…).

- **Descubrir:** desliza a la derecha (♥ me gusta) o a la izquierda (✕ menos de esto). 🛒 la guarda en la cesta y al tocar la tarjeta se abre la ficha real.
- **Buscar:** saca o elige una foto de una prenda y Clothify busca prendas parecidas y las ordena por parecido visual. Si Gemini no está disponible, la foto se analiza en el propio móvil, sin internet.
- **Cesta:** botón "Comprar" a la ficha real, enlaces "Buscar parecidos" en Amazon, AliExpress, Shein, Vinted y Wallapop, y **aviso si baja de precio o se vende** (Wallapop y AliExpress, revisión cada 12 h).
- **Perfil:** lo que la app ha aprendido de ti, tus tallas, tu precio máximo, la ubicación y las búsquedas gratuitas que te quedan.

Diseño: [Fase 1](docs/superpowers/specs/2026-10-21-clothify-fase1-design.md) · [Fases 2 y 3](docs/superpowers/specs/2026-10-21-clothify-fases2-3-design.md).

---

## ⚠️ Antes de nada: pon el repositorio en privado

El APK lleva dentro tus claves de Gemini y SerpApi. Si el repositorio es público, cualquiera puede
descargar el APK de *Releases*, sacar las claves y gastar tu cuota gratuita.

GitHub → **Settings → General → Danger Zone → Change visibility → Make private**.

## 1. Consigue las claves gratuitas (todas opcionales)

| Secreto | Para qué | Dónde |
|---|---|---|
| `GEMINI_API_KEY` | Analizar tus fotos de ropa con más precisión | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) → *Create API key* |
| `SERPAPI_KEY` | Prendas nuevas de Google Shopping (250 búsquedas/mes gratis; la app usa como mucho 8 al día) | [serpapi.com](https://serpapi.com/users/sign_up) → plan *Free* → *Your Private API Key* |
| `ALIEXPRESS_APP_KEY`, `ALIEXPRESS_APP_SECRET`, `ALIEXPRESS_TRACKING_ID` | Productos y precios oficiales de AliExpress, y avisos de precio | [portals.aliexpress.com](https://portals.aliexpress.com) → alta como afiliado → *Tools → API* (pide una app de tipo *Affiliates API*; la aprobación puede tardar unos días). El *tracking ID* está en *Tracking ID*. |

Sin claves la app también funciona: Wallapop y Vinted no necesitan clave, y las fotos se analizan
en el propio móvil.

## 2. Añádelas como secretos de GitHub

GitHub → **Settings → Secrets and variables → Actions → New repository secret**, una vez por cada
clave, con los nombres exactos de la tabla.

## 3. Descarga e instala el APK

Cada vez que se suben cambios, GitHub compila la app sola (pestaña **Actions**). Cuando los cambios
llegan a la rama principal, el APK se publica en **Releases** como `clothify-vN.apk`.

1. En el móvil, abre la página **Releases** del repositorio y descarga el último `clothify-vN.apk`.
2. Ábrelo. Android te pedirá permitir "instalar apps desconocidas" para tu navegador: acéptalo.
3. Para actualizar, instala el APK nuevo encima: tus gustos y tu cesta se conservan.

> Después de añadir los secretos, vuelve a lanzar la compilación (Actions → *Build APK* → *Run workflow*)
> para que el APK lleve las claves.

### Firma de los APK (para poder actualizar sin perder datos)

Android solo deja instalar una versión encima de otra si las dos van firmadas con la misma clave.
Créala una sola vez, desde el navegador:

1. Con el repositorio ya en **privado**, ve a **Actions → Generate signing key → Run workflow**.
2. Cuando termine, abre la ejecución y descarga el artefacto `signing-secrets`. Dentro hay un texto
   con cuatro valores.
3. Crea los secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD` con esos
   valores. Después borra el artefacto (caduca solo en 1 día).
4. Vuelve a lanzar *Build APK*.

Sin estos secretos el APK se firma con una clave temporal distinta en cada compilación. Se puede
instalar, pero para actualizar tendrás que desinstalar antes, y perderás tus gustos y tu cesta.

## Avisos

- **Vinted y Wallapop** se consultan mediante sus APIs internas, que no son oficiales. Va contra
  sus condiciones de uso, puede dejar de funcionar si cambian su web y podrían bloquearte. La app
  hace muy pocas peticiones (como mucho una cada 3 s y solo al rellenar tarjetas o buscar). Si una
  fuente falla, verás un aviso y la app seguirá con las demás.
- **Gemini, plan gratuito:** Google puede usar las fotos que envías para mejorar sus productos.
- **Ubicación:** es opcional y aproximada (~1 km). Solo se usa para ordenar Wallapop por cercanía.
- **Avisos de precio:** se activan en la Cesta o en Perfil y piden permiso de notificaciones. Vinted
  y Google Shopping no se pueden vigilar (lo indica la cesta).
- **IA en el móvil:** el APK pesa unos 135 MB porque incluye el modelo CLIP (ViT-B/32, de OpenAI,
  versión cuantizada de [Xenova](https://huggingface.co/Xenova/clip-vit-base-patch32)). Solo
  funciona en móviles ARM (la gran mayoría).

## Desarrollo

Requisitos: JDK 17+ y el SDK de Android (API 36). Las claves en local van en `local.properties`
(no se sube al repositorio):

```properties
sdk.dir=/ruta/al/android-sdk
GEMINI_API_KEY=...
SERPAPI_KEY=...
```

```bash
./gradlew testDebugUnitTest   # pruebas (JUnit + Robolectric)
./gradlew assembleDebug       # APK de desarrollo
```

Para el análisis en el móvil, descarga el modelo (no está en git) a
`app/src/main/assets/clip/vision_model_quantized.onnx` desde
`https://huggingface.co/Xenova/clip-vit-base-patch32/resolve/main/onnx/vision_model_quantized.onnx`.
Los vectores de las etiquetas (`assets/clip/tag_embeddings.json`) se regeneran con
`tools/clip/embed_tags.py` si cambias el vocabulario.

Estructura: `domain/` (lógica pura: vocabulario de etiquetas, recomendador, planificador de
búsquedas, enlaces), `data/` (fuentes Wallapop/Vinted/SerpApi/AliExpress, Gemini, CLIP, avisos de
precio, Room, DataStore) y `ui/`
(pantallas Compose).
