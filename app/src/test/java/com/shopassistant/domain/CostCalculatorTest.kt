package com.shopassistant.domain

import com.shopassistant.model.PackagingLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CostCalculatorTest {

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals(
            "expected $expected but was ${actual.toPlainString()}",
            0,
            BigDecimal(expected).compareTo(actual)
        )
    }

    private fun suggest(cost: String) =
        CostCalculator.suggestSellingPrice(BigDecimal(cost), BigDecimal.ONE)

    @Test
    fun suggestedPriceFollowsNextStrictlyHigherMultipleOf50() {
        assertDecimal("50", suggest("45"))
        assertDecimal("250", suggest("200"))
        assertDecimal("350", suggest("325"))
        assertDecimal("400", suggest("350"))
        assertDecimal("450", suggest("425"))
        assertDecimal("550", suggest("525"))
        assertDecimal("700", suggest("675"))
    }

    @Test
    fun fractionalQuantitiesAreSupported() {
        // Viva: carton 15,000; 12.5 strips per carton; 4 sachets per strip
        val levels = listOf(
            PackagingLevel("Carton", BigDecimal.ONE),
            PackagingLevel("Strip", BigDecimal("12.5")),
            PackagingLevel("Sachet", BigDecimal("4"))
        )
        val result = CostCalculator.calculate(BigDecimal("15000"), levels)
        assertDecimal("15000", result.levels[0].cost)
        assertDecimal("1200", result.levels[1].cost)
        assertDecimal("300", result.levels[2].cost)
        assertDecimal("50", result.smallestUnitsPerLargest)
        assertDecimal("350", result.suggestedSellingPrice)
    }

    @Test
    fun threeLevelsWithNonTerminatingCost() {
        // Pampers: big pack 10,000; 10 small packs; 9 diapers per small pack
        val levels = listOf(
            PackagingLevel("Big pack", BigDecimal.ONE),
            PackagingLevel("Small pack", BigDecimal("10")),
            PackagingLevel("Diaper", BigDecimal("9"))
        )
        val result = CostCalculator.calculate(BigDecimal("10000"), levels)
        assertDecimal("1000", result.levels[1].cost)
        assertEquals("111.1111111111", result.levels[2].cost.toPlainString())
        assertDecimal("150", result.suggestedSellingPrice)
    }

    @Test
    fun singleLevelProduct() {
        val result = CostCalculator.calculate(
            BigDecimal("200"),
            listOf(PackagingLevel("Bottle", BigDecimal.ONE))
        )
        assertDecimal("200", result.smallest.cost)
        assertDecimal("250", result.suggestedSellingPrice)
    }

    @Test
    fun marginIsCalculatedOnSellingPrice() {
        assertDecimal("25", CostCalculator.marginPercent(BigDecimal("400"), BigDecimal("300"))!!)
        assertDecimal("-50", CostCalculator.profit(BigDecimal("250"), BigDecimal("300")))
    }
}
