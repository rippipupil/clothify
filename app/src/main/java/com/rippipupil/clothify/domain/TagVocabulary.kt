package com.rippipupil.clothify.domain

import java.text.Normalizer

/** Lista cerrada de etiquetas: la única fuente de verdad para perfil, fuentes y Gemini. */
object TagVocabulary {

    val all: List<Tag> = buildList {
        fun cat(id: String, label: String, size: SizeKind?, vararg syn: String) =
            add(Tag(id, TagGroup.CATEGORY, label, listOf(*syn), size))
        fun style(id: String, label: String, vararg syn: String) = add(Tag(id, TagGroup.STYLE, label, listOf(*syn)))
        fun color(id: String, label: String, vararg syn: String) = add(Tag(id, TagGroup.COLOR, label, listOf(*syn)))
        fun fit(id: String, label: String, vararg syn: String) = add(Tag(id, TagGroup.FIT_MATERIAL, label, listOf(*syn)))

        val top = SizeKind.TOP
        val waist = SizeKind.WAIST
        val shoe = SizeKind.SHOE
        cat("camiseta", "camiseta", top, "camiseta", "camisetas", "t shirt", "tshirt", "tee")
        cat("tirantes", "camiseta de tirantes", top, "tirantes", "tank top", "sin mangas")
        cat("camisa", "camisa", top, "camisa", "camisas", "shirt", "overshirt", "sobrecamisa")
        cat("polo", "polo", top, "polo", "polos")
        cat("sudadera", "sudadera", top, "sudadera", "sudaderas", "hoodie", "sweatshirt", "crewneck")
        cat("jersey", "jersey", top, "jersey", "jerseis", "jerseys", "sueter", "sweater", "cardigan", "rebeca")
        cat("chaqueta", "chaqueta", top, "chaqueta", "chaquetas", "jacket", "blazer", "americana")
        cat("cazadora", "cazadora", top, "cazadora", "bomber", "varsity", "harrington", "biker")
        cat("abrigo", "abrigo", top, "abrigo", "coat", "gabardina", "trench", "parka", "plumifero", "anorak")
        cat("chaleco", "chaleco", top, "chaleco", "gilet", "vest")
        cat("vaqueros", "vaqueros", waist, "vaqueros", "vaquero", "jeans", "jean", "tejanos", "tejano")
        cat("pantalon", "pantalón", waist, "pantalon", "pantalones", "trousers", "pants")
        cat("chino", "chino", waist, "chino", "chinos")
        cat("cargo", "cargo", waist, "cargo", "cargos")
        cat("shorts", "shorts", waist, "shorts", "short", "bermuda", "bermudas", "pantalon corto", "pantalones cortos")
        cat("chandal", "chándal", waist, "chandal", "joggers", "jogger", "tracksuit")
        cat("banador", "bañador", waist, "banador", "swim shorts", "boardshorts")
        cat("traje", "traje", top, "traje", "suit")
        cat("zapatillas", "zapatillas", shoe, "zapatillas", "zapatilla", "sneakers", "sneaker", "deportivas", "tenis", "trainers")
        cat("botas", "botas", shoe, "botas", "bota", "boots", "botines", "botin")
        cat("zapatos", "zapatos", shoe, "zapatos", "zapato", "mocasines", "mocasin", "loafers", "shoes", "oxford", "derby")
        cat("sandalias", "sandalias", shoe, "sandalias", "sandalia", "chanclas", "sandals")
        cat("gorra", "gorra", null, "gorra", "gorras", "cap", "gorro", "beanie", "sombrero", "bucket hat")
        cat("mochila", "mochila / bolso", null, "mochila", "bandolera", "rinonera", "bolso", "backpack")

        style("streetwear", "streetwear", "streetwear", "street", "urbano", "urbana", "hip hop")
        style("skate", "skate", "skate", "skater", "skateboard")
        style("minimalista", "minimalista", "minimalista", "minimal", "basico", "basica", "liso", "lisa")
        style("vintage", "vintage", "vintage", "retro", "90s", "80s", "70s", "anos 90", "anos 80")
        style("y2k", "Y2K", "y2k", "2000s")
        style("old_money", "old money", "old money", "elegante", "quiet luxury")
        style("deportivo", "deportivo", "deportivo", "deportiva", "sport", "running", "gym", "training", "futbol")
        style("formal", "formal", "formal", "vestir", "corbata", "blazer", "americana", "traje")
        style("casual", "casual", "casual", "informal", "diario")
        style("grunge", "grunge", "grunge", "rock", "punk", "metal", "heavy")
        style("preppy", "preppy", "preppy", "college", "universitario", "rugby", "ivy")
        style("workwear", "workwear", "workwear", "carhartt", "dickies", "trabajo", "canvas", "carpenter")
        style("techwear", "techwear / outdoor", "techwear", "gorpcore", "outdoor", "impermeable", "cortavientos", "goretex", "gore tex", "softshell", "trekking")
        style("militar", "militar", "militar", "military", "army", "camuflaje")
        style("surf", "surf", "surf", "surfero", "playa", "hawaiana")

        color("negro", "negro", "negro", "negra", "negros", "negras", "black")
        color("blanco", "blanco", "blanco", "blanca", "blancos", "blancas", "white")
        color("gris", "gris", "gris", "grises", "grey", "gray", "jaspeado")
        color("beige", "beige", "beige", "crema", "arena", "camel", "crudo", "hueso", "cream", "sand")
        color("marron", "marrón", "marron", "marrones", "brown", "chocolate", "cafe")
        color("tonos_tierra", "tonos tierra", "tierra", "terracota", "mostaza", "caqui", "khaki", "oliva", "olive")
        color("azul_marino", "azul marino", "azul marino", "marino", "navy")
        color("azul_claro", "azul claro", "azul claro", "celeste", "light blue", "azul cielo")
        color("azul", "azul", "azul", "azules", "blue")
        color("verde", "verde", "verde", "verdes", "green")
        color("rojo", "rojo", "rojo", "roja", "rojos", "red", "granate", "burdeos")
        color("rosa", "rosa", "rosa", "pink")
        color("amarillo", "amarillo", "amarillo", "amarilla", "yellow")
        color("naranja", "naranja", "naranja", "orange")
        color("morado", "morado", "morado", "morada", "lila", "purple")
        color("estampado", "estampado", "estampado", "estampada", "print", "rayas", "cuadros", "flores", "tie dye")

        fit("oversize", "oversize", "oversize", "oversized", "holgado", "holgada", "boxy")
        fit("slim", "slim", "slim", "ajustado", "ajustada", "skinny", "entallado")
        fit("recto", "recto", "recto", "recta", "straight", "regular fit")
        fit("ancho", "ancho", "ancho", "ancha", "anchos", "baggy", "wide", "wide leg", "relaxed")
        fit("cropped", "cropped", "cropped", "crop")
        fit("denim", "denim", "denim", "vaquero", "vaquera", "vaqueros", "vaqueras", "jeans", "tejano")
        fit("punto", "punto", "punto", "knit", "tricot", "lana", "wool", "merino", "cashmere")
        fit("cuero", "cuero / piel", "cuero", "piel", "leather", "polipiel", "ante", "suede")
        fit("lino", "lino", "lino", "linen")
        fit("pana", "pana", "pana", "corduroy")
        fit("algodon", "algodón", "algodon", "cotton")
        fit("polar", "polar", "polar", "fleece", "borreguito", "sherpa")
        fit("acolchado", "acolchado", "acolchado", "acolchada", "puffer", "plumas")
        fit("desgastado", "desgastado", "desgastado", "lavado", "washed", "faded", "rotos", "ripped")
        fit("seda", "seda / satén", "seda", "satin", "saten")
    }

    private val byId: Map<String, Tag> = all.associateBy { it.id }

    fun byId(id: String): Tag? = byId[id]

    fun isValid(id: String): Boolean = id in byId

    private val diacritics = Regex("\\p{Mn}+")
    private val nonAlphanumeric = Regex("[^a-z0-9]+")

    /** Minúsculas, sin tildes, signos convertidos en espacios simples. */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(diacritics, "")
            .replace(nonAlphanumeric, " ")
            .trim()
}
