package com.shopassistant.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopassistant.data.ProductRepository
import com.shopassistant.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListState(
    val loaded: Boolean = false,
    val total: Int = 0,
    val items: List<Product> = emptyList()
)

class ProductListViewModel(private val repository: ProductRepository) : ViewModel() {

    private val all = MutableStateFlow<List<Product>?>(null)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val state: StateFlow<ListState> = combine(all, _query) { list, query ->
        if (list == null) {
            ListState()
        } else {
            val q = query.trim()
            val filtered = if (q.isEmpty()) list else list.filter { product ->
                product.name.contains(q, ignoreCase = true) ||
                    product.levels.any { it.name.contains(q, ignoreCase = true) }
            }
            ListState(loaded = true, total = list.size, items = filtered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListState())

    init {
        viewModelScope.launch {
            repository.changes.collect { all.value = repository.getAll() }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }
}
