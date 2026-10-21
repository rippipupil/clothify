package com.rippipupil.clothify.ui.search

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.settings.SettingsRepository
import com.rippipupil.clothify.data.style.StyleAnalyzer
import com.rippipupil.clothify.data.style.StyleResult
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.SearchBudget
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Similarity
import com.rippipupil.clothify.domain.StoreLinkBuilder
import com.rippipupil.clothify.domain.TagGroup
import com.rippipupil.clothify.domain.TagVocabulary
import com.rippipupil.clothify.ui.message
import com.rippipupil.clothify.ui.sourceWarning
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Analyzing : SearchUiState
    data class Tags(val tagIds: Set<String>, val error: String?, val canRetry: Boolean) : SearchUiState
    data class Searching(val tagIds: Set<String>) : SearchUiState
    data class Results(val tagIds: Set<String>, val products: List<Product>, val warning: String?) : SearchUiState
}

class SearchViewModel(
    private val analyzer: StyleAnalyzer,
    private val pool: ProductPoolRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state
    private var lastPhoto: Bitmap? = null

    /** Búsquedas de Google Shopping que quedan hoy. */
    val shoppingSearchesLeft: StateFlow<Int> = settings.settings
        .map { SearchBudget.remaining(it.budget, LocalDate.now()).first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchBudget.DAILY)

    fun onPhoto(photo: Bitmap): Job {
        lastPhoto = photo
        return analyze(photo)
    }

    fun retry(): Job? = lastPhoto?.let(::analyze)

    fun chooseManually() {
        lastPhoto = null
        _state.value = SearchUiState.Tags(emptySet(), error = null, canRetry = false)
    }

    fun toggleTag(id: String) {
        val current = currentTags()
        val next = if (id in current) current - id else current + id
        _state.value = SearchUiState.Tags(next, error = null, canRetry = lastPhoto != null)
    }

    fun search(): Job = viewModelScope.launch {
        val tags = currentTags()
        if (tags.isEmpty()) return@launch
        _state.value = SearchUiState.Searching(tags)
        val s = settings.current()
        val query = SearchQuery(
            text = StoreLinkBuilder.queryText(tags, withSection = false),
            tagIds = tags,
            categoryTag = tags.firstOrNull { TagVocabulary.byId(it)?.group == TagGroup.CATEGORY },
            maxPriceEur = s.maxPriceEur,
            sizes = s.sizes,
            location = s.lastLocation.takeIf { s.locationEnabled },
        )
        val (products, failures) = pool.searchAll(query)
        _state.value = SearchUiState.Results(tags, Similarity.rank(tags, products), sourceWarning(failures))
    }

    fun backToTags() {
        _state.value = SearchUiState.Tags(currentTags(), error = null, canRetry = lastPhoto != null)
    }

    fun reset() {
        lastPhoto = null
        _state.value = SearchUiState.Idle
    }

    private fun analyze(photo: Bitmap): Job = viewModelScope.launch {
        _state.value = SearchUiState.Analyzing
        _state.value = when (val result = analyzer.analyze(photo)) {
            is StyleResult.Success -> SearchUiState.Tags(result.tagIds.toSet(), error = null, canRetry = true)
            is StyleResult.Failure -> SearchUiState.Tags(emptySet(), error = result.reason.message(), canRetry = true)
        }
    }

    private fun currentTags(): Set<String> = when (val s = _state.value) {
        is SearchUiState.Tags -> s.tagIds
        is SearchUiState.Searching -> s.tagIds
        is SearchUiState.Results -> s.tagIds
        else -> emptySet()
    }
}
