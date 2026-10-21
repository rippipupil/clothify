package com.rippipupil.clothify.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rippipupil.clothify.domain.product
import com.rippipupil.clothify.ui.discover.DiscoverContent
import com.rippipupil.clothify.ui.discover.DiscoverUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiscoverScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun heartButtonSwipesRight() {
        val swipes = mutableListOf<Pair<String, Boolean>>()
        val p = product("sudadera-negra", setOf("sudadera"), price = 27.0)
        compose.setContent {
            DiscoverContent(
                state = DiscoverUiState.Cards(listOf(p), warning = null),
                onSwipe = { product, liked -> swipes += product.id to liked },
                onAddToBasket = {},
                onOpen = {},
                onRetry = {},
            )
        }
        compose.onNodeWithText("27 €").assertExists()
        compose.onNodeWithContentDescription("Me gusta").performClick()
        assertEquals(listOf("sudadera-negra" to true), swipes)
    }

    @Test
    fun emptyStateOffersRetry() {
        var retries = 0
        compose.setContent {
            DiscoverContent(DiscoverUiState.Empty("Vinted no responde ahora"), { _, _ -> }, {}, {}, { retries++ })
        }
        compose.onNodeWithText("Vinted no responde ahora").assertExists()
        compose.onNodeWithText("Buscar más").performClick()
        assertEquals(1, retries)
    }
}
