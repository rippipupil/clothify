package com.rippipupil.clothify.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.PriceAlert
import com.rippipupil.clothify.data.pool.PriceChecker
import com.rippipupil.clothify.data.pool.PriceFetcher
import com.rippipupil.clothify.data.pool.PriceLookup
import com.rippipupil.clothify.data.sources.WallapopItemParser
import com.rippipupil.clothify.data.sources.fixture
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PriceCheckerTest {
    @get:Rule
    val migrations = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ClothifyDatabase::class.java)

    private lateinit var db: ClothifyDatabase
    private lateinit var basket: BasketRepository

    private class Fake(private val lookups: Map<String, PriceLookup>) : PriceFetcher {
        val asked = mutableListOf<String>()
        override fun supports(source: Source) = source == Source.WALLAPOP
        override suspend fun lookup(product: Product): PriceLookup = lookups.getValue(product.id).also { asked += product.id }
    }

    @Before
    fun setUp() {
        db = ClothifyDatabase.inMemory(ApplicationProvider.getApplicationContext())
        basket = BasketRepository(db) { 1L }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun item(id: String) = basket.items.first().single { it.product.id == id }

    @Test
    fun priceDropRaisesAlertAndIsStored() = runTest {
        basket.add(product("wp:a", price = 30.0))
        val alerts = PriceChecker(basket, listOf(Fake(mapOf("wp:a" to PriceLookup.Price(24.0))))) { 5L }.check()
        assertEquals(listOf(PriceAlert.Dropped(item("wp:a").product, 30.0, 24.0)), alerts)
        assertEquals(24.0, item("wp:a").currentPriceEur!!, 1e-9)
    }

    @Test
    fun secondDropComparesWithLastKnownPrice() = runTest {
        basket.add(product("wp:a", price = 30.0))
        val fake = Fake(mapOf("wp:a" to PriceLookup.Price(24.0)))
        PriceChecker(basket, listOf(fake)) { 5L }.check()
        assertTrue(PriceChecker(basket, listOf(fake)) { 6L }.check().isEmpty())
    }

    @Test
    fun priceRiseIsStoredWithoutAlert() = runTest {
        basket.add(product("wp:a", price = 30.0))
        assertTrue(PriceChecker(basket, listOf(Fake(mapOf("wp:a" to PriceLookup.Price(35.0))))) { 5L }.check().isEmpty())
        assertEquals(35.0, item("wp:a").currentPriceEur!!, 1e-9)
    }

    @Test
    fun goneAlertsOnlyOnce() = runTest {
        basket.add(product("wp:a"))
        val fake = Fake(mapOf("wp:a" to PriceLookup.Gone))
        assertEquals(1, PriceChecker(basket, listOf(fake)) { 5L }.check().filterIsInstance<PriceAlert.Gone>().size)
        assertTrue(item("wp:a").unavailable)
        assertTrue(PriceChecker(basket, listOf(fake)) { 6L }.check().isEmpty())
    }

    @Test
    fun unknownChangesNothing() = runTest {
        basket.add(product("wp:a", price = 30.0))
        assertTrue(PriceChecker(basket, listOf(Fake(mapOf("wp:a" to PriceLookup.Unknown)))) { 5L }.check().isEmpty())
        assertEquals(null, item("wp:a").currentPriceEur)
    }

    @Test
    fun unsupportedSourcesAreSkipped() = runTest {
        basket.add(product("vt:a", source = Source.VINTED))
        val fake = Fake(emptyMap())
        assertTrue(PriceChecker(basket, listOf(fake)) { 5L }.check().isEmpty())
        assertTrue(fake.asked.isEmpty())
    }

    @Test
    fun wallapopItemPriceIsParsed() = assertEquals(20.0, WallapopItemParser.price(fixture("wallapop_item.json"))!!, 1e-9)

    @Test
    fun migrationOneToTwoKeepsTheBasket() {
        migrations.createDatabase("migration-test", 1).apply {
            execSQL(
                "INSERT INTO basket_item (productId, source, title, imageUrl, priceEur, sellerName, productUrl, size, distanceKm, tagIds, fetchedAtMillis, addedAtMillis) " +
                    "VALUES ('wp:a', 'WALLAPOP', 'Sudadera', 'https://i', 20.0, 'Wallapop', 'https://u', NULL, NULL, 'sudadera', 1, 2)",
            )
            close()
        }
        val migrated = migrations.runMigrationsAndValidate("migration-test", 2, true, ClothifyDatabase.MIGRATION_1_2)
        migrated.query("SELECT title, unavailable, currentPriceEur FROM basket_item").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Sudadera", c.getString(0))
            assertEquals(0, c.getInt(1))
            assertTrue(c.isNull(2))
        }
    }
}
