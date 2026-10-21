package com.rippipupil.clothify.domain

data class StylePreset(val id: String, val label: String, val tagIds: Set<String>)

/** Estilos que el usuario puede elegir en la bienvenida. */
object StylePresets {
    val all: List<StylePreset> = listOf(
        StylePreset("streetwear", "Streetwear", setOf("streetwear", "oversize", "sudadera", "zapatillas")),
        StylePreset("minimalista", "Minimalista", setOf("minimalista", "camiseta", "negro", "blanco", "recto")),
        StylePreset("vintage", "Vintage", setOf("vintage", "desgastado", "vaqueros", "cazadora")),
        StylePreset("old_money", "Old money", setOf("old_money", "polo", "jersey", "punto", "beige", "chino")),
        StylePreset("deportivo", "Deportivo", setOf("deportivo", "chandal", "zapatillas", "sudadera")),
        StylePreset("workwear", "Workwear", setOf("workwear", "cargo", "chaqueta", "tonos_tierra", "pana")),
        StylePreset("formal", "Formal", setOf("formal", "camisa", "traje", "zapatos", "azul_marino")),
        StylePreset("y2k", "Y2K", setOf("y2k", "ancho", "vaqueros", "estampado")),
        StylePreset("techwear", "Techwear / outdoor", setOf("techwear", "abrigo", "negro", "cargo")),
        StylePreset("casual", "Casual", setOf("casual", "camiseta", "vaqueros", "zapatillas")),
        StylePreset("skate", "Skate", setOf("skate", "ancho", "sudadera", "gorra")),
        StylePreset("grunge", "Grunge", setOf("grunge", "camisa", "estampado", "botas", "negro")),
    )
}
