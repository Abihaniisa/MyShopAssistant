package com.shopassistant.domain

import com.shopassistant.model.PackagingLevel
import java.math.BigDecimal
import java.math.RoundingMode

data class LevelCost(
    val name: String,
    val parentName: String?,
    val quantityPerParent: BigDecimal,
    /** Purchase cost of one unit of this level. */
    val cost: BigDecimal,
    /** How many smallest units one unit of this level holds. */
    val smallestUnits: BigDecimal
)

data class CostBreakdown(
    val levels: List<LevelCost>,
    val smallestUnitsPerLargest: BigDecimal,
    val suggestedSellingPrice: BigDecimal
) {
    val largest: LevelCost get() = levels.first()
    val smallest: LevelCost get() = levels.last()
}

object CostCalculator {
    private const val SCALE = 10
    private val STEP = BigDecimal(50)

    /**
     * Calculates the cost at every level.
     * The cost of a level is purchasePrice / (product of all quantities down to that level),
     * computed once from the exact product so no intermediate rounding accumulates.
     */
    fun calculate(purchasePrice: BigDecimal, levels: List<PackagingLevel>): CostBreakdown {
        require(levels.isNotEmpty()) { "At least one packaging level is required" }

        val cumulative = ArrayList<BigDecimal>(levels.size)
        var running = BigDecimal.ONE
        levels.forEachIndexed { index, level ->
            if (index > 0) running = running.multiply(level.quantityPerParent)
            cumulative.add(running)
        }
        val totalUnits = running

        val costs = levels.mapIndexed { index, level ->
            val smallestUnits = levels.drop(index + 1)
                .fold(BigDecimal.ONE) { acc, l -> acc.multiply(l.quantityPerParent) }
            LevelCost(
                name = level.name,
                parentName = levels.getOrNull(index - 1)?.name,
                quantityPerParent = level.quantityPerParent,
                cost = purchasePrice.divide(cumulative[index], SCALE, RoundingMode.HALF_UP),
                smallestUnits = smallestUnits
            )
        }

        return CostBreakdown(
            levels = costs,
            smallestUnitsPerLargest = totalUnits,
            suggestedSellingPrice = suggestSellingPrice(purchasePrice, totalUnits)
        )
    }

    /**
     * Next strictly higher multiple of 50 above the smallest-unit cost.
     * Cost = purchasePrice / unitsPerLargest. The floor is taken on the exact quotient
     * (purchasePrice / (unitsPerLargest * 50)), so boundary values behave exactly:
     * 350 -> 400, 45 -> 50, 200 -> 250.
     */
    fun suggestSellingPrice(purchasePrice: BigDecimal, unitsPerLargest: BigDecimal): BigDecimal {
        val divisor = unitsPerLargest.multiply(STEP)
        val steps = purchasePrice.divide(divisor, 0, RoundingMode.FLOOR)
        return steps.add(BigDecimal.ONE).multiply(STEP)
    }

    fun profit(sellingPrice: BigDecimal, unitCost: BigDecimal): BigDecimal =
        sellingPrice.subtract(unitCost)

    /** Profit as a percentage of the selling price, one decimal place. */
    fun marginPercent(sellingPrice: BigDecimal, unitCost: BigDecimal): BigDecimal? {
        if (sellingPrice.signum() <= 0) return null
        return sellingPrice.subtract(unitCost)
            .multiply(BigDecimal(100))
            .divide(sellingPrice, 1, RoundingMode.HALF_UP)
    }
}
