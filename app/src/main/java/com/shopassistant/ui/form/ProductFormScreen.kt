package com.shopassistant.ui.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.shopassistant.domain.CostCalculator
import com.shopassistant.ui.components.AppCard
import com.shopassistant.ui.components.DecimalField
import com.shopassistant.ui.components.FieldShape
import com.shopassistant.ui.components.SectionLabel
import com.shopassistant.ui.components.appFieldColors
import com.shopassistant.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    viewModel: ProductFormViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditing) "Edit product" else "New product",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (viewModel.showErrors && viewModel.hasErrors) {
                            Text(
                                "Please fix the highlighted fields.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = { viewModel.save(onSaved) },
                            enabled = !viewModel.isSaving && !viewModel.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                if (viewModel.isEditing) "Save changes" else "Save product",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { inner ->
        if (viewModel.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            FormContent(viewModel = viewModel, modifier = Modifier.padding(inner))
        }
    }
}

@Composable
private fun FormContent(viewModel: ProductFormViewModel, modifier: Modifier = Modifier) {
    val show = viewModel.showErrors
    val preview = viewModel.preview
    val completeBreakdown = preview?.takeIf { it.complete }?.breakdown

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Name
        OutlinedTextField(
            value = viewModel.name,
            onValueChange = { viewModel.name = it.take(60) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Product name") },
            singleLine = true,
            isError = show && viewModel.nameError != null,
            supportingText = if (show) viewModel.nameError?.let { { Text(it) } } else null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            shape = FieldShape,
            colors = appFieldColors()
        )

        // Purchase price
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Purchase")
            DecimalField(
                value = viewModel.price,
                onValueChange = { viewModel.price = it },
                label = "Price of 1 ${viewModel.levelLabel(0)}",
                modifier = Modifier.fillMaxWidth(),
                prefix = "₦",
                maxFraction = 2,
                error = if (show) viewModel.priceError else null
            )
        }

        // Packaging
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Packaging")
            Text(
                "Start with the largest package, then add each smaller level inside it. Fractions such as 12.5 are fine.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    viewModel.levels.forEachIndexed { index, level ->
                        key(level.key) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (index == 0) {
                                    LevelNameField(
                                        value = level.name,
                                        onValueChange = { level.name = it.take(30) },
                                        label = "Largest package",
                                        error = if (show) viewModel.levelNameError(index) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        "1 ${viewModel.levelLabel(index - 1)} contains",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        DecimalField(
                                            value = level.quantity,
                                            onValueChange = { level.quantity = it },
                                            label = "Quantity",
                                            modifier = Modifier.weight(0.85f),
                                            error = if (show) viewModel.levelQuantityError(index) else null
                                        )
                                        LevelNameField(
                                            value = level.name,
                                            onValueChange = { level.name = it.take(30) },
                                            label = "Level name",
                                            error = if (show) viewModel.levelNameError(index) else null,
                                            modifier = Modifier.weight(1.15f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeLevel(index) },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove level"
                                            )
                                        }
                                    }
                                }

                                preview?.breakdown?.levels?.getOrNull(index)?.let { cost ->
                                    Text(
                                        "Costs ${Formatters.money(cost.cost)} each",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::addLevel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = FieldShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add level")
                    }
                }
            }
        }

        // Pricing
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Pricing")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (completeBreakdown == null) {
                        Text(
                            "Enter the price and every quantity to see the cost per unit and a suggested selling price.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val smallest = completeBreakdown.smallest
                        val suggested = completeBreakdown.suggestedSellingPrice

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Cost per ${smallest.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                Formatters.money(smallest.cost),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Suggested price",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Next ₦50 above cost",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                Formatters.money(suggested),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { viewModel.useSuggestedPrice(suggested) }) {
                                Text("Use")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        DecimalField(
                            value = viewModel.sellingPrice,
                            onValueChange = { viewModel.sellingPrice = it },
                            label = "Your selling price per ${smallest.name} (optional)",
                            modifier = Modifier.fillMaxWidth(),
                            prefix = "₦",
                            maxFraction = 2,
                            imeAction = ImeAction.Done,
                            error = if (show) viewModel.sellingPriceError else null
                        )

                        viewModel.parsedSellingPrice?.let { selling ->
                            val profit = CostCalculator.profit(selling, smallest.cost)
                            val margin = CostCalculator.marginPercent(selling, smallest.cost)
                            val color = if (profit.signum() < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Profit per ${smallest.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    Formatters.money(profit) +
                                        (margin?.let { "  (${Formatters.percent(it)})" } ?: ""),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelNameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        ),
        shape = FieldShape,
        colors = appFieldColors()
    )
}
