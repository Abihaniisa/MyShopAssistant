package com.shopassistant.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopassistant.domain.CostCalculator
import com.shopassistant.domain.LevelCost
import com.shopassistant.model.Product
import com.shopassistant.ui.components.AppCard
import com.shopassistant.ui.components.SectionLabel
import com.shopassistant.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val state = viewModel.state
    val product = (state as? DetailState.Loaded)?.product
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(onClick = { onEdit(product.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit product")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete product")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { inner ->
        when (state) {
            DetailState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            DetailState.Missing -> Box(Modifier.fillMaxSize().padding(inner))

            is DetailState.Loaded -> DetailContent(
                product = state.product,
                modifier = Modifier.padding(inner)
            )
        }
    }

    if (confirmDelete && product != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete product?") },
            text = { Text("“${product.name}” and its packaging breakdown will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailContent(product: Product, modifier: Modifier = Modifier) {
    val breakdown = remember(product) {
        CostCalculator.calculate(product.purchasePrice, product.levels)
    }
    val largest = breakdown.largest
    val smallest = breakdown.smallest
    val multiLevel = breakdown.levels.size > 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(product.name, style = MaterialTheme.typography.headlineMedium)

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Purchase price",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    Formatters.money(product.purchasePrice),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "per ${largest.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (multiLevel) {
                    Text(
                        "1 ${largest.name} holds ${Formatters.quantity(breakdown.smallestUnitsPerLargest)} ${smallest.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Cost breakdown")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    breakdown.levels.forEachIndexed { index, level ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        LevelRow(
                            level = level,
                            isSmallest = index == breakdown.levels.lastIndex,
                            smallestName = smallest.name
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Selling price per ${smallest.name}")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InfoRow(
                        label = "Suggested",
                        value = Formatters.money(breakdown.suggestedSellingPrice),
                        valueColor = MaterialTheme.colorScheme.primary,
                        large = true
                    )
                    Text(
                        "The next ₦50 multiple above the cost of one ${smallest.name}.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    val selling = product.sellingPrice
                    if (selling == null) {
                        InfoRow(label = "Your selling price", value = "Not set")
                    } else {
                        val profit = CostCalculator.profit(selling, smallest.cost)
                        val margin = CostCalculator.marginPercent(selling, smallest.cost)
                        val profitColor = if (profit.signum() < 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        InfoRow(label = "Your selling price", value = Formatters.money(selling))
                        InfoRow(
                            label = "Profit per ${smallest.name}",
                            value = Formatters.money(profit),
                            valueColor = profitColor
                        )
                        if (margin != null) {
                            InfoRow(
                                label = "Margin",
                                value = Formatters.percent(margin),
                                valueColor = profitColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelRow(level: LevelCost, isSmallest: Boolean, smallestName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                level.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                if (level.parentName == null) {
                    "Largest package"
                } else {
                    "${Formatters.quantity(level.quantityPerParent)} per ${level.parentName}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(0.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Formatters.money(level.cost),
                style = MaterialTheme.typography.titleMedium,
                color = if (isSmallest) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                if (isSmallest) "smallest unit"
                else "holds ${Formatters.quantity(level.smallestUnits)} $smallestName",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    large: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (large) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.titleMedium,
            color = valueColor
        )
    }
}
