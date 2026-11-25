package com.shopassistant.model

import java.math.BigDecimal

/**
 * One packaging level. Levels are ordered from the largest package to the smallest unit.
 * [quantityPerParent] is how many of this level fit in the previous (larger) level.
 * For the first (largest) level it is always 1.
 */
data class PackagingLevel(
    val name: String,
    val quantityPerParent: BigDecimal
)

data class Product(
    val id: Long,
    val name: String,
    /** Purchase price of one unit of the largest package. */
    val purchasePrice: BigDecimal,
    val levels: List<PackagingLevel>,
    /** The user's own selling price per smallest unit; null when not set. */
    val sellingPrice: BigDecimal?
)
