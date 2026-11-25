package com.shopassistant.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopassistant.data.ProductRepository
import com.shopassistant.model.Product
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val product: Product) : DetailState
    data object Missing : DetailState
}

class ProductDetailViewModel(
    private val repository: ProductRepository,
    private val productId: Long
) : ViewModel() {

    var state by mutableStateOf<DetailState>(DetailState.Loading)
        private set

    init {
        viewModelScope.launch {
            repository.changes.collect {
                val product = repository.get(productId)
                state = if (product == null) DetailState.Missing else DetailState.Loaded(product)
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(productId)
            onDeleted()
        }
    }
}
