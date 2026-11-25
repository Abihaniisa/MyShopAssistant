package com.shopassistant.ui.form

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopassistant.data.ProductRepository
import com.shopassistant.domain.CostBreakdown
import com.shopassistant.domain.CostCalculator
import com.shopassistant.model.PackagingLevel
import com.shopassistant.model.Product
import com.shopassistant.util.Formatters
import com.shopassistant.util.parseDecimal
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** Live calculation shown while the form is being filled in. */
class Preview(val breakdown: CostBreakdown, val complete: Boolean)

class ProductFormViewModel(
    private val repository: ProductRepository,
    private val productId: Long
) : ViewModel() {

    class LevelInput(val key: Long, name: String = "", quantity: String = "") {
        var name by mutableStateOf(name)
        var quantity by mutableStateOf(quantity)
    }

    private var nextKey = 0L

    val isEditing: Boolean = productId != 0L

    var name by mutableStateOf("")
    var price by mutableStateOf("")
    var sellingPrice by mutableStateOf("")
    val levels = mutableStateListOf<LevelInput>()

    var showErrors by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(productId != 0L)
        private set
    var isSaving by mutableStateOf(false)
        private set

    private fun newLevel(name: String = "", quantity: String = ""): LevelInput =
        LevelInput(nextKey++, name, quantity)

    // ---- Live calculation -------------------------------------------------

    /**
     * Costs calculated from as many valid levels as the user has filled in so far.
     * [Preview.complete] is true only when every level is valid.
     */
    val preview: Preview? by derivedStateOf {
        val purchase = parseDecimal(price)?.takeIf { it.signum() > 0 }
            ?: return@derivedStateOf null
        val valid = mutableListOf<PackagingLevel>()
        for ((index, level) in levels.withIndex()) {
            val quantity = if (index == 0) {
                BigDecimal.ONE
            } else {
                parseDecimal(level.quantity)?.takeIf { it.signum() > 0 } ?: break
            }
            valid.add(PackagingLevel(level.name.trim().ifBlank { "Level ${index + 1}" }, quantity))
        }
        if (valid.isEmpty()) null
        else Preview(CostCalculator.calculate(purchase, valid), valid.size == levels.size)
    }

    val parsedSellingPrice: BigDecimal?
        get() = parseDecimal(sellingPrice)?.takeIf { it.signum() > 0 }

    // ---- Level editing ----------------------------------------------------

    fun addLevel() {
        levels.add(newLevel())
    }

    fun removeLevel(index: Int) {
        if (index in 1 until levels.size) levels.removeAt(index)
    }

    fun levelLabel(index: Int): String =
        levels.getOrNull(index)?.name?.trim()?.ifBlank { null }
            ?: if (index == 0) "largest package" else "level ${index + 1}"

    fun useSuggestedPrice(value: BigDecimal) {
        sellingPrice = Formatters.plain(value)
    }

    // ---- Validation -------------------------------------------------------

    val nameError: String?
        get() = if (name.isBlank()) "Enter a product name" else null

    val priceError: String?
        get() {
            if (price.isBlank()) return "Enter the purchase price"
            val value = parseDecimal(price)
            return if (value == null || value.signum() <= 0) "Enter an amount above 0" else null
        }

    fun levelNameError(index: Int): String? {
        val current = levels.getOrNull(index)?.name?.trim() ?: return null
        if (current.isEmpty()) return "Name this level"
        val duplicate = (0 until index).any {
            levels[it].name.trim().equals(current, ignoreCase = true)
        }
        return if (duplicate) "Names must be different" else null
    }

    fun levelQuantityError(index: Int): String? {
        if (index == 0) return null
        val text = levels.getOrNull(index)?.quantity ?: return null
        if (text.isBlank()) return "Required"
        val value = parseDecimal(text)
        return if (value == null || value.signum() <= 0) "Must be above 0" else null
    }

    val sellingPriceError: String?
        get() {
            if (sellingPrice.isBlank()) return null
            val value = parseDecimal(sellingPrice)
            return if (value == null || value.signum() <= 0) "Enter a valid amount" else null
        }

    val hasErrors: Boolean
        get() = nameError != null ||
            priceError != null ||
            sellingPriceError != null ||
            levels.indices.any { levelNameError(it) != null || levelQuantityError(it) != null }

    // ---- Save -------------------------------------------------------------

    fun save(onSaved: () -> Unit) {
        showErrors = true
        if (hasErrors || isSaving) return
        val purchase = parseDecimal(price) ?: return

        val product = Product(
            id = productId,
            name = name.trim(),
            purchasePrice = purchase,
            levels = levels.mapIndexed { index, level ->
                PackagingLevel(
                    name = level.name.trim(),
                    quantityPerParent = if (index == 0) BigDecimal.ONE
                    else parseDecimal(level.quantity) ?: BigDecimal.ONE
                )
            },
            sellingPrice = parseDecimal(sellingPrice)
        )

        isSaving = true
        viewModelScope.launch {
            repository.save(product)
            isSaving = false
            onSaved()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val product = repository.get(productId)
            if (product != null) {
                name = product.name
                price = Formatters.plain(product.purchasePrice)
                sellingPrice = product.sellingPrice?.let { Formatters.plain(it) } ?: ""
                levels.clear()
                product.levels.forEachIndexed { index, level ->
                    levels.add(
                        newLevel(
                            level.name,
                            if (index == 0) "" else Formatters.plain(level.quantityPerParent)
                        )
                    )
                }
            }
            isLoading = false
        }
    }

    init {
        if (isEditing) {
            load()
        } else {
            levels.add(newLevel())
            levels.add(newLevel())
        }
    }
}
