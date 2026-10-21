package com.rippipupil.clothify.ui

import androidx.test.core.app.ApplicationProvider
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.data.sources.ProductSource
import com.rippipupil.clothify.data.sources.SourceResult
import com.rippipupil.clothify.data.style.FailureReason
import com.rippipupil.clothify.data.style.StyleResult
import com.rippipupil.clothify.domain.QueryPlanner
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.product
import com.rippipupil.clothify.ui.search.SearchUiState
import com.rippipupil.clothify.ui.search.SearchViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SearchViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: ClothifyDatabase
    private val queries = mutableListOf<SearchQuery>()

    private val source = object : ProductSource {
        override val source = Source.WALLAPOP
        override suspend fun search(query: SearchQuery): SourceResult {
            queries += query
            return SourceResult.Ok(
                listOf(
                    product("half", setOf("sudadera", "rojo")),
                    product("full", setOf("sudadera", "negro")),
                    product("none", setOf("botas")),
                ),
            )
        }
    }

    @Before
    fun setUp() {
        db = ClothifyDatabase.inMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = db.close()

    private fun vm(analyzer: FakeAnalyzer): SearchViewModel {
        val settings = testSettings(tmp)
        val pool = ProductPoolRepository(db, listOf(source), QueryPlanner(Random(1)), ProfileRepository(db), settings)
        return SearchViewModel(analyzer, pool, settings)
    }

    @Test
    fun analyzedPhotoShowsEditableTags() = runTest {
        val vm = vm(FakeAnalyzer(StyleResult.Success(listOf("sudadera", "negro"))))
        vm.onPhoto(bitmap()).join()
        assertEquals(SearchUiState.Tags(setOf("sudadera", "negro"), error = null, canRetry = true), vm.state.value)
    }

    @Test
    fun searchRanksResultsBySimilarity() = runTest {
        val vm = vm(FakeAnalyzer(StyleResult.Success(listOf("sudadera", "negro"))))
        vm.onPhoto(bitmap()).join()
        vm.search().join()
        val results = vm.state.value as SearchUiState.Results
        assertEquals(listOf("full", "half"), results.products.map { it.id })
        assertEquals("sudadera negro", queries.single().text)
        assertEquals("sudadera", queries.single().categoryTag)
    }

    @Test
    fun quotaErrorOffersManualTags() = runTest {
        val vm = vm(FakeAnalyzer(StyleResult.Failure(FailureReason.QUOTA_EXCEEDED)))
        vm.onPhoto(bitmap()).join()
        assertEquals(
            SearchUiState.Tags(
                emptySet(),
                error = "Has agotado el análisis gratuito de hoy. Vuelve mañana o elige las etiquetas a mano.",
                canRetry = true,
            ),
            vm.state.value,
        )
    }

    @Test
    fun retryAnalyzesTheSamePhotoAgain() = runTest {
        val analyzer = FakeAnalyzer(StyleResult.Failure(FailureReason.NO_INTERNET))
        val vm = vm(analyzer)
        vm.onPhoto(bitmap()).join()
        analyzer.result = StyleResult.Success(listOf("gorra"))
        vm.retry()!!.join()
        assertEquals(2, analyzer.calls)
        assertEquals(setOf("gorra"), (vm.state.value as SearchUiState.Tags).tagIds)
    }

    @Test
    fun toggleTagEditsSelection() = runTest {
        val vm = vm(FakeAnalyzer(StyleResult.Success(listOf("sudadera"))))
        vm.onPhoto(bitmap()).join()
        vm.toggleTag("negro")
        vm.toggleTag("sudadera")
        assertEquals(setOf("negro"), (vm.state.value as SearchUiState.Tags).tagIds)
    }

    @Test
    fun manualModeStartsWithNoTags() {
        val vm = vm(FakeAnalyzer(StyleResult.Success(emptyList())))
        vm.chooseManually()
        assertTrue((vm.state.value as SearchUiState.Tags).tagIds.isEmpty())
    }
}
