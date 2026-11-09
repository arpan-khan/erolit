package dev.erolit.reader.ui.categories

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.erolit.reader.data.repo.CatalogRepository
import dev.erolit.reader.domain.Category
import dev.erolit.reader.ui.common.UiState
import dev.erolit.reader.ui.common.UiState.Content
import dev.erolit.reader.ui.common.UiState.Empty
import dev.erolit.reader.ui.common.UiState.Error
import dev.erolit.reader.ui.common.UiState.Loading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoriesViewModel(private val catalog: CatalogRepository) : ViewModel() {

    private val _ui = MutableStateFlow(CategoriesUiState())
    val ui: StateFlow<CategoriesUiState> = _ui.asStateFlow()

    private var _query = mutableStateOf("")
    val query: String get() = _query.value
    fun setQuery(query: String) {
        _query.value = query
    }

    private var _typeFilter = mutableStateOf("all")
    val typeFilter: String get() = _typeFilter.value
    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(list = Loading) }
            try {
                val all = catalog.loadCategories(force = false)
                _ui.update { it.copy(list = if (all.isEmpty()) Empty else Content(all)) }
            } catch (e: Throwable) {
                _ui.update { it.copy(list = Error(e as? dev.erolit.reader.core.net.ApiError
                    ?: dev.erolit.reader.core.net.ApiError.Network(e))) }
            }
        }
    }

    fun filtered(): List<Category> {
        val q = query.lowercase()
        val data = when (val list = _ui.value.list) {
            is Content -> list.data
            else -> emptyList()
        }
        return data.filter { category ->
            (typeFilter == "all" || category.type == typeFilter) &&
                (q.isEmpty() || category.name.lowercase().contains(q))
        }
    }
}

data class CategoriesUiState(
    val list: UiState<List<Category>> = UiState.Loading,
)
