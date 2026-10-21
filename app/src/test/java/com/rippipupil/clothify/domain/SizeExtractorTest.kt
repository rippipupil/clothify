package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SizeExtractorTest {
    @Test fun tallaPrefix() = assertEquals("M", SizeExtractor.fromText("Sudadera talla M"))
    @Test fun tallaWithDash() = assertEquals("XL", SizeExtractor.fromText("Camiseta - Talla XL"))
    @Test fun abbreviatedTalla() = assertEquals("L", SizeExtractor.fromText("Camisa T. L"))
    @Test fun shoeNumber() = assertEquals("42", SizeExtractor.fromText("Zapatillas 42"))
    @Test fun vintedSizeTitle() = assertEquals("M", SizeExtractor.fromText("M / 38 / 10"))
    @Test fun twoXlIsXxl() = assertEquals("XXL", SizeExtractor.fromText("Polo talla 2XL"))
    @Test fun noSize() = assertNull(SizeExtractor.fromText("Sudadera Nike"))
    @Test fun ignoresPrices() = assertNull(SizeExtractor.fromText("Chaqueta 40€"))
    @Test fun ignoresApostropheS() = assertNull(SizeExtractor.fromText("Vaqueros Levi's"))
}
