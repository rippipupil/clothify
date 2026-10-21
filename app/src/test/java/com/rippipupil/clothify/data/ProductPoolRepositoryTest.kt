package com.rippipupil.clothify.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.local.toEntity
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.pool.SettingsBudgetGate
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.sources.ProductSource
import com.rippipupil.clothify.data.sources.SourceFailure
import com.rippipupil.clothify.data.sources.SourceResult
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.QueryPlanner
import com.rippipupil.clothify.domain.SearchBudget
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProductPoolRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: ClothifyDatabase
    private lateinit var settings: SettingsRepository
    private var now = 10 * DAY

    private inner class FakeSource(override val source: Source, val perQuery: Int, val fail: SourceFailure? = null) : ProductSource {
        val queries = mutableListOf<SearchQuery>()
        override suspend fun search(query: SearchQuery): SourceResult {
            queries += query
            if (fail != null) return SourceResult.Failed(fail)
            return SourceResult.Ok(List(perQuery) { product("${source.name}:${query.text}:$it", query.tagIds, source, fetchedAt = now) })
        }
    }

    @Before
    fun setUp() {
        db = ClothifyDatabase.inMemory(ApplicationProvider.getApplicationContext())
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) { tmp.newFile("s.preferences_pb") },
        )
    }

    @After
    fun tearDown() = db.close()

    private fun pool(vararg sources: ProductSource) =
        ProductPoolRepository(db, sources.toList(), QueryPlanner(Random(1)), ProfileRepository(db), settings) { now }

    @Test
    fun refillCollectsFromWorkingSourcesAndReportsFailures() = runTest {
        ProfileRepository(db).apply(ProfileEvent.PresetChosen(setOf("sudadera", "streetwear")))
        val pool = pool(FakeSource(Source.WALLAPOP, 5), FakeSource(Source.VINTED, 0, SourceFailure.BLOCKED))
        val report = pool.refill()
        assertEquals(15, report.added)
        assertEquals(mapOf(Source.VINTED to SourceFailure.BLOCKED), report.failures)
        assertEquals(15, pool.candidates().size)
    }

    @Test
    fun refillDoesNotRepeatQueriesWithin24Hours() = runTest {
        val wallapop = FakeSource(Source.WALLAPOP, 1)
        val pool = pool(wallapop)
        pool.refill()
        pool.refill()
        val texts = wallapop.queries.map { it.text }
        assertEquals(texts.size, texts.toSet().size)
    }

    @Test
    fun seenProductsAreNotCandidatesAgain() = runTest {
        val pool = pool(FakeSource(Source.WALLAPOP, 2))
        pool.refill()
        val first = pool.candidates().first()
        pool.markSeen(first, liked = true)
        assertFalse(pool.candidates().any { it.id == first.id })
        pool.refill()
        assertFalse(pool.candidates().any { it.id == first.id })
    }

    @Test
    fun needsRefillBelowFifteen() = runTest {
        val pool = pool(FakeSource(Source.WALLAPOP, 5))
        assertTrue(pool.needsRefill())
        pool.refill()
        assertFalse(pool.needsRefill())
    }

    @Test
    fun purgeExpiredRemovesProductsOlderThanThreeDays() = runTest {
        db.productDao().upsertAll(listOf(product("old", fetchedAt = now - 4 * DAY), product("new", fetchedAt = now - DAY)).map { it.toEntity() })
        val pool = pool()
        assertEquals(listOf("new"), pool.candidates().map { it.id })
        pool.purgeExpired()
        assertEquals(listOf("new"), db.productDao().all().map { it.id })
    }

    @Test
    fun searchAllCombinesSourcesWithoutSaving() = runTest {
        val pool = pool(FakeSource(Source.WALLAPOP, 2), FakeSource(Source.GOOGLE_SHOPPING, 0, SourceFailure.BUDGET_EXHAUSTED))
        val (products, failures) = pool.searchAll(
            SearchQuery("sudadera", setOf("sudadera"), "sudadera", 60.0, com.rippipupil.clothify.domain.UserSizes(), null),
        )
        assertEquals(2, products.size)
        assertEquals(mapOf(Source.GOOGLE_SHOPPING to SourceFailure.BUDGET_EXHAUSTED), failures)
        assertTrue(db.productDao().all().isEmpty())
    }

    @Test
    fun budgetGateConsumesUntilDailyLimit() = runTest {
        val gate = SettingsBudgetGate(settings) { LocalDate.of(2026, 10, 21) }
        repeat(SearchBudget.DAILY) { assertTrue(gate.tryConsume()) }
        assertFalse(gate.tryConsume())
        assertEquals(SearchBudget.DAILY, settings.current().budget.dayCount)
    }

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
