package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Shield
import com.example.data.GroundedSpeciesIntelligence
import com.example.data.MaintenanceAction
import com.example.data.OpenMeteoWeatherService
import com.example.data.StressCalculationOutput
import com.example.data.TreeGuardEngines
import com.example.data.TreeRecord
import com.example.data.UrbanForestAiInsights
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestAbyssBg
import com.example.ui.theme.ForestBorderSubtle
import com.example.ui.theme.ForestCardBg
import com.example.ui.theme.ForestCardElevated
import com.example.ui.theme.PriorityCriticalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun IntelligenceScreen(
    uiState: TreeGuardUiState,
    groundedSearchState: AsyncTaskState<GroundedSpeciesIntelligence>,
    forestInsightsState: AsyncTaskState<UrbanForestAiInsights>,
    onFetchForestInsights: () -> Unit,
    onSetBudgetScenario: (Double) -> Unit,
    onToggleMaintenanceStatus: (MaintenanceAction) -> Unit,
    onAddMaintenanceAction: (TreeRecord, String, Double, Double) -> Unit,
    onRecordStressSnapshot: (TreeRecord, StressCalculationOutput) -> Unit,
    onFetchWeatherForCoordinates: (lat: Double, lon: Double, label: String, forceRefresh: Boolean) -> Unit,
    onPropagateWeatherToEngines: (TreeRecord?) -> Unit,
    onRunGroundedSearch: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("AI Forest Insights", "Weather Intel", "Budget Optimizer", "Stress Debt", "Species Grounding")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ForestAbyssBg)
            .testTag("intelligence_screen")
    ) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = ForestCardBg,
            contentColor = EmeraldPrimary
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeSubTab == idx,
                    onClick = { activeSubTab = idx },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeSubTab == idx) EmeraldPrimary else TextSecondary,
                            fontWeight = if (activeSubTab == idx) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("intelligence_subtab_$idx")
                )
            }
        }

        when (activeSubTab) {
            0 -> UrbanForestAiInsightsSubScreen(
                uiState = uiState,
                insightsState = forestInsightsState,
                onFetchInsights = onFetchForestInsights,
                onSpeakText = onSpeakText
            )
            1 -> WeatherIntelligenceSubScreen(
                uiState = uiState,
                onFetchWeatherForCoordinates = onFetchWeatherForCoordinates,
                onPropagateWeatherToEngines = onPropagateWeatherToEngines,
                onSpeakText = onSpeakText
            )
            2 -> WhatIfMaintenanceOptimizerSubScreen(
                uiState = uiState,
                onSetBudget = onSetBudgetScenario,
                onToggleStatus = onToggleMaintenanceStatus,
                onAddProposal = onAddMaintenanceAction,
                onSpeakText = onSpeakText
            )
            3 -> StressDebtEngineSubScreen(
                uiState = uiState,
                onRecordStressSnapshot = onRecordStressSnapshot,
                onSpeakText = onSpeakText
            )
            4 -> SearchGroundedIntelligenceSubScreen(
                groundedSearchState = groundedSearchState,
                onRunGroundedSearch = onRunGroundedSearch,
                onSpeakText = onSpeakText
            )
        }
    }
}

@Composable
private fun WeatherIntelligenceSubScreen(
    uiState: TreeGuardUiState,
    onFetchWeatherForCoordinates: (lat: Double, lon: Double, label: String, forceRefresh: Boolean) -> Unit,
    onPropagateWeatherToEngines: (TreeRecord?) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val w = uiState.weather
    var latInput by remember(w) {
        mutableStateOf(String.format(Locale.US, "%.4f", w?.latitude ?: OpenMeteoWeatherService.VIJAYAWADA_LAT))
    }
    var lonInput by remember(w) {
        mutableStateOf(String.format(Locale.US, "%.4f", w?.longitude ?: OpenMeteoWeatherService.VIJAYAWADA_LON))
    }
    var labelInput by remember(w) {
        mutableStateOf(w?.locationLabel ?: OpenMeteoWeatherService.VIJAYAWADA_LABEL)
    }
    var selectedTargetTree by remember(uiState.trees) { mutableStateOf<TreeRecord?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Coordinate Selector & Open-Meteo Cache Controller
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OPEN-METEO WEATHER INTELLIGENCE MODULE",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Retrieves temperature, humidity, precipitation, wind speed, wind gusts & soil moisture with 15-min TTL caching",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (w != null) {
                            IconButton(
                                onClick = {
                                    onSpeakText(
                                        "Weather Intelligence for ${w.locationLabel}. " +
                                            "Temperature ${w.temperature} degrees Celsius, relative humidity ${w.humidity.toInt()} percent, " +
                                            "wind speed ${w.windSpeed.toInt()} kilometers per hour with gusts up to ${w.windGusts.toInt()}. " +
                                            "Derived Heat Stress Indicator is ${w.derivedHeatIndicator}, Dryness Indicator is ${w.derivedDrynessIndicator}, " +
                                            "and Inspection Priority Weather Factor W is ${w.derivedWeatherPriorityScore} out of 100."
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Speak Weather Intelligence Summary",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }

                    Text(
                        text = "Quick Select Coordinate Target (Default Vijayawada or Registered Tree GPS):",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTargetTree == null,
                                onClick = {
                                    selectedTargetTree = null
                                    latInput = String.format(Locale.US, "%.4f", OpenMeteoWeatherService.VIJAYAWADA_LAT)
                                    lonInput = String.format(Locale.US, "%.4f", OpenMeteoWeatherService.VIJAYAWADA_LON)
                                    labelInput = OpenMeteoWeatherService.VIJAYAWADA_LABEL
                                    onFetchWeatherForCoordinates(
                                        OpenMeteoWeatherService.VIJAYAWADA_LAT,
                                        OpenMeteoWeatherService.VIJAYAWADA_LON,
                                        OpenMeteoWeatherService.VIJAYAWADA_LABEL,
                                        false
                                    )
                                },
                                label = { Text("Vijayawada Default (16.5062, 80.6480)") }
                            )
                        }
                        items(uiState.trees) { tree ->
                            FilterChip(
                                selected = selectedTargetTree?.id == tree.id,
                                onClick = {
                                    selectedTargetTree = tree
                                    latInput = String.format(Locale.US, "%.4f", tree.latitude)
                                    lonInput = String.format(Locale.US, "%.4f", tree.longitude)
                                    labelInput = "${tree.treeCode} • ${tree.zone}"
                                    onFetchWeatherForCoordinates(
                                        tree.latitude,
                                        tree.longitude,
                                        "${tree.treeCode} • ${tree.zone}",
                                        false
                                    )
                                },
                                label = { Text("${tree.treeCode} (${String.format(Locale.US, "%.3f, %.3f", tree.latitude, tree.longitude)})") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lonInput,
                            onValueChange = { lonInput = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val lat = latInput.toDoubleOrNull() ?: OpenMeteoWeatherService.VIJAYAWADA_LAT
                                val lon = lonInput.toDoubleOrNull() ?: OpenMeteoWeatherService.VIJAYAWADA_LON
                                onFetchWeatherForCoordinates(lat, lon, labelInput, false)
                            },
                            enabled = !uiState.isWeatherLoading,
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weather_fetch_cached_button")
                        ) {
                            Text("Query (Use Cache)", color = EmeraldPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {
                                val lat = latInput.toDoubleOrNull() ?: OpenMeteoWeatherService.VIJAYAWADA_LAT
                                val lon = lonInput.toDoubleOrNull() ?: OpenMeteoWeatherService.VIJAYAWADA_LON
                                onFetchWeatherForCoordinates(lat, lon, labelInput, true)
                            },
                            enabled = !uiState.isWeatherLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ForestAbyssBg),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weather_force_refresh_button")
                        ) {
                            if (uiState.isWeatherLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = ForestAbyssBg,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetching...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Force Live Refresh", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (w != null) {
                        Surface(
                            color = ForestAbyssBg,
                            border = BorderStroke(1.dp, ForestBorderSubtle),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cache Status: ${w.cacheStatus} • Cached Grids: ${w.cachedLocationsCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (w.isLiveApi) EmeraldPrimary else WarningAmber
                                    )
                                    Text(
                                        text = w.fetchedAtLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    if (w.lastErrorMessage != null) {
                                        Text(
                                            text = "Graceful Fallback: ${w.lastErrorMessage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarningAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Current 6-Metric Telemetry & Engine Input Integration Card
        if (w != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "CURRENT METEOROLOGICAL & SOIL TELEMETRY (${w.locationLabel})",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeatherTelemetryStat("Temperature", "${w.temperature}°C", Icons.Default.Thermostat)
                            WeatherTelemetryStat("Humidity", "${w.humidity.toInt()}%", Icons.Default.WaterDrop)
                            WeatherTelemetryStat("Precipitation", "${w.precipitation} mm", Icons.Default.WaterDrop)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeatherTelemetryStat("Wind Speed", "${w.windSpeed} km/h", Icons.Default.Air)
                            WeatherTelemetryStat("Wind Gusts", "${w.windGusts} km/h", Icons.Default.Air)
                            WeatherTelemetryStat(
                                "Soil Moisture",
                                if (w.soilMoisture != null) "${String.format(Locale.US, "%.2f", w.soilMoisture)} m³/m³" else "Proxy (N/A)",
                                Icons.Default.Forest
                            )
                        }

                        // Engine Input Integration Box
                        Surface(
                            color = ForestAbyssBg,
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "INTEGRATED ENGINE INPUTS (0–100 NORMALIZED SCALE)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Heat H_t (0.45)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text("${w.derivedHeatIndicator}/100", style = MaterialTheme.typography.titleMedium, color = WarningAmber)
                                    }
                                    Column {
                                        Text("Dryness D_t (0.35)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text("${w.derivedDrynessIndicator}/100", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                                    }
                                    Column {
                                        Text("Priority W (0.10)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text("${w.derivedWeatherPriorityScore}/100", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
                                    }
                                }
                                Text(
                                    text = "Feeds directly into Tree Stress Debt Engine (S_t = 0.45H_t + 0.35D_t + 0.20O_t) and Explainable Priority Engine (R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Button(
                                    onClick = { onPropagateWeatherToEngines(selectedTargetTree) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ForestAbyssBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("propagate_weather_to_engines_button")
                                ) {
                                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedTargetTree != null) {
                                            "Apply Weather to ${selectedTargetTree!!.treeCode} Engines"
                                        } else {
                                            "Propagate Weather to All ${uiState.trees.size} Registered Trees"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. 7-Day Regional Forecast & Projected Canopy Stress
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "7-DAY OPEN-METEO FORECAST & PROJECTED STRESS INDEX",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Daily temperature range, precipitation sum, peak wind gusts, and estimated Heat (H_t) / Priority (W) impact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        w.forecast7Day.forEach { day ->
                            Surface(
                                color = ForestAbyssBg,
                                border = BorderStroke(1.dp, ForestBorderSubtle),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.1f)) {
                                        Text(
                                            text = day.date,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Max ${day.maxTemp}°C / Min ${day.minTemp}°C",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Rain: ${day.precipitationSum} mm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Gusts: ${day.maxWindGusts} km/h",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (day.maxWindGusts >= 35.0) WarningAmber else TextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Est H_t: ${day.estimatedDailyHeatStress.roundToInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WarningAmber
                                        )
                                        Text(
                                            text = "Est W: ${day.estimatedDailyWeatherRisk.roundToInt()}/100",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Important Limitation: Open-Meteo provides regional meteorological context for the GPS grid cell, not a direct root-zone measurement of an individual street tree.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherTelemetryStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun WhatIfMaintenanceOptimizerSubScreen(
    uiState: TreeGuardUiState,
    onSetBudget: (Double) -> Unit,
    onToggleStatus: (MaintenanceAction) -> Unit,
    onAddProposal: (TreeRecord, String, Double, Double) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val opt = uiState.optimizationResult
    var showAddDialog by remember { mutableStateOf(false) }
    var customBudgetInput by remember(uiState.budgetInr) { mutableStateOf(uiState.budgetInr.toInt().toString()) }

    if (showAddDialog && uiState.trees.isNotEmpty()) {
        var selectedTree by remember { mutableStateOf(uiState.trees.first()) }
        var actionText by remember { mutableStateOf("Certified arborist crown safety assessment & deadwood clearance") }
        var costText by remember { mutableStateOf("2200") }
        var benefitScore by remember { mutableDoubleStateOf(80.0) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = ForestCardBg,
            title = { Text("Propose Maintenance Action", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select target tree (${selectedTree.treeCode}) and synthetic estimated cost:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(uiState.trees) { t ->
                            FilterChip(
                                selected = selectedTree.id == t.id,
                                onClick = { selectedTree = t },
                                label = { Text(t.treeCode) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = actionText,
                        onValueChange = { actionText = it },
                        label = { Text("Proposed Action") }
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Estimated Cost (₹ INR)") },
                        singleLine = true
                    )
                    Text("Expected Benefit Score: ${benefitScore.roundToInt()}/100", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = benefitScore.toFloat(),
                        onValueChange = { benefitScore = it.toDouble() },
                        valueRange = 10f..100f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedCost = costText.toDoubleOrNull() ?: 2000.0
                        onAddProposal(selectedTree, actionText, parsedCost, benefitScore)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ForestAbyssBg)
                ) {
                    Text("Add Proposal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WHAT-IF MAINTENANCE OPTIMIZER",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Greedy Benefit-to-Cost Ranking (Efficiency = Benefit / Cost)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                onSpeakText(
                                    "What-If Maintenance Optimizer for demo budget ${opt.budgetInr.toInt()} rupees. " +
                                        "${opt.selectedActions.size} actions funded across ${opt.treesReceivingAttentionCount} trees. " +
                                        "Total expenditure ${opt.totalEstimatedExpenditure.toInt()} rupees, remaining budget ${opt.remainingBudget.toInt()} rupees, " +
                                        "expected aggregate benefit ${opt.expectedAggregateBenefit.toInt()} points."
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Speak Optimizer Summary", tint = ElectricCyan)
                        }
                    }

                    Text(
                        text = "Select a Demo Budget Scenario (Synthetic estimated costs):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5000.0, 10000.0, 25000.0).forEach { presetBudget ->
                            val isSelected = uiState.budgetInr.roundToInt() == presetBudget.roundToInt()
                            Button(
                                onClick = {
                                    customBudgetInput = presetBudget.toInt().toString()
                                    onSetBudget(presetBudget)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) EmeraldPrimary else ForestCardElevated,
                                    contentColor = if (isSelected) ForestAbyssBg else TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("budget_preset_${presetBudget.toInt()}")
                            ) {
                                Text("₹${presetBudget.toInt()}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customBudgetInput,
                            onValueChange = {
                                customBudgetInput = it
                                it.toDoubleOrNull()?.let(onSetBudget)
                            },
                            label = { Text("Custom Demo Budget (₹ INR)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            border = BorderStroke(1.dp, ElectricCyan)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Action", color = ElectricCyan)
                        }
                    }

                    // Summary KPIs
                    Surface(
                        color = ForestAbyssBg,
                        border = BorderStroke(1.dp, ForestBorderSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Expenditure", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("₹${opt.totalEstimatedExpenditure.toInt()}", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
                            }
                            Column {
                                Text("Remaining", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("₹${opt.remainingBudget.toInt()}", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                            }
                            Column {
                                Text("Benefit Score", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("+${opt.expectedAggregateBenefit.toInt()} pts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            Column {
                                Text("Trees Funded", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${opt.treesReceivingAttentionCount}", style = MaterialTheme.typography.titleMedium, color = WarningAmber)
                            }
                        }
                    }

                    Text(
                        text = "Note: Cost and benefit values are synthetic demonstration assumptions, not verified municipal quotations. A greedy benefit-to-cost heuristic is a fast decision aid and does not guarantee a mathematically global optimum in every discrete knapsack case.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Side-by-Side Budget Scenario Comparison Matrix (₹5,000 vs ₹10,000 vs ₹25,000)
        item {
            val scenarioResults = remember(uiState.maintenanceActions) {
                listOf(5000.0, 10000.0, 25000.0).map { b ->
                    TreeGuardEngines.optimizeMaintenanceBudget(uiState.maintenanceActions, b)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SCENARIO COMPARISON MATRIX (₹5,000 vs ₹10,000 vs ₹25,000)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scenarioResults.forEach { res ->
                            val isCurrent = uiState.budgetInr.roundToInt() == res.budgetInr.roundToInt()
                            Surface(
                                color = if (isCurrent) EmeraldPrimary.copy(alpha = 0.14f) else ForestAbyssBg,
                                border = BorderStroke(1.dp, if (isCurrent) EmeraldPrimary else ForestBorderSubtle),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        customBudgetInput = res.budgetInr.toInt().toString()
                                        onSetBudget(res.budgetInr)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "₹${res.budgetInr.toInt()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCurrent) EmeraldPrimary else ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${res.selectedActions.size} Actions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${res.treesReceivingAttentionCount} Trees",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarningAmber
                                    )
                                    Text(
                                        text = "Spent: ₹${res.totalEstimatedExpenditure.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Benefit: +${res.expectedAggregateBenefit.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected (Funded) Actions
        item {
            Text(
                text = "SELECTED / FUNDED ACTIONS (${opt.selectedActions.size})",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary
            )
        }
        items(opt.selectedActions, key = { "funded_${it.id}_${it.action}" }) { act ->
            MaintenanceActionCard(action = act, isFunded = true, onToggleStatus = { onToggleStatus(act) })
        }

        // Unfunded Actions
        if (opt.unfundedActions.isNotEmpty()) {
            item {
                Text(
                    text = "DEFERRED / UNFUNDED UNDER CURRENT BUDGET (${opt.unfundedActions.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarningAmber
                )
            }
            items(opt.unfundedActions, key = { "unfunded_${it.id}_${it.action}" }) { act ->
                MaintenanceActionCard(action = act, isFunded = false, onToggleStatus = { onToggleStatus(act) })
            }
        }
    }
}

@Composable
private fun MaintenanceActionCard(
    action: MaintenanceAction,
    isFunded: Boolean,
    onToggleStatus: () -> Unit
) {
    val accent = if (isFunded) EmeraldPrimary else WarningAmber
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestCardBg),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${action.treeCode} • Priority ${action.priorityScore.toInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onToggleStatus() }
                ) {
                    Text(
                        text = if (isFunded) "FUNDED (${action.status})" else "UNFUNDED",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = action.action, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Est. Cost: ₹${action.estimatedCost.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = "Benefit: ${action.expectedBenefit.toInt()} • Efficiency: ${String.format(Locale.US, "%.1f", action.efficiency)} pts/₹1k",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun StressDebtEngineSubScreen(
    uiState: TreeGuardUiState,
    onRecordStressSnapshot: (TreeRecord, StressCalculationOutput) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val trees = uiState.trees
    var targetTree by remember(trees) { mutableStateOf(trees.firstOrNull()) }

    val weatherPair = remember(uiState.weather) {
        uiState.weather?.let { TreeGuardEngines.deriveWeatherStressIndicators(it) }
    }

    var heatH by remember(weatherPair) { mutableDoubleStateOf(weatherPair?.first ?: 68.0) }
    var drynessD by remember(weatherPair) { mutableDoubleStateOf(weatherPair?.second ?: 58.0) }
    var obsO by remember(targetTree) { mutableDoubleStateOf(targetTree?.observationContribution ?: 55.0) }
    var deltaDays by remember { mutableDoubleStateOf(1.0) }
    var halfLifeDays by remember { mutableDoubleStateOf(30.0) }
    var useWeatherSource by remember { mutableStateOf(uiState.weather?.isLiveApi == true) }

    val prevDebt = targetTree?.stressDebt ?: 48.0
    val calc = remember(prevDebt, deltaDays, heatH, drynessD, obsO, halfLifeDays, useWeatherSource) {
        TreeGuardEngines.calculateStressDebt(
            previousStressDebt = prevDebt,
            deltaDays = deltaDays,
            heatIndicator = heatH,
            drynessIndicator = drynessD,
            observationIndicator = obsO,
            halfLifeDays = halfLifeDays,
            dataSourceLabel = if (useWeatherSource) "OPEN_METEO_REGIONAL" else "ESTIMATED_SIMULATED"
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TREE STRESS DEBT ENGINE (EXPERIMENTAL)",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "S_t = 0.45H_t + 0.35D_t + 0.20O_t • TD_t = TD_(t-1)·e^(-λΔt) + S_t",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                onSpeakText(
                                    "Tree Stress Debt Engine for ${targetTree?.treeCode ?: "selected tree"}. " +
                                        "Daily stress S_t is ${calc.dailyStress}. " +
                                        "Previous Stress Debt ${calc.previousDebt} decays to ${calc.decayedPreviousDebt} over ${calc.deltaDays.toInt()} days, " +
                                        "yielding new accumulated Stress Debt ${calc.newAccumulatedDebt}. " +
                                        "Note: Stress Debt is an experimental environmental indicator, not a direct medical measure of tree health."
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Speak Stress Debt Output", tint = ElectricCyan)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(trees) { t ->
                            FilterChip(
                                selected = targetTree?.id == t.id,
                                onClick = {
                                    targetTree = t
                                    obsO = t.observationContribution
                                },
                                label = { Text("${t.treeCode} (TD=${t.stressDebt})") }
                            )
                        }
                    }

                    Surface(
                        color = ForestAbyssBg,
                        border = BorderStroke(1.dp, ForestBorderSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Previous TD_(t-1)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${calc.previousDebt}", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            }
                            Column {
                                Text("Daily Stress S_t", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("+${calc.dailyStress}", style = MaterialTheme.typography.titleMedium, color = WarningAmber)
                            }
                            Column {
                                Text("New Stress Debt TD_t", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${calc.newAccumulatedDebt}", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                            }
                        }
                    }

                    StressSliderRow("Heat Indicator H_t (Weight 0.45 → +${calc.heatWeighted})", heatH) {
                        heatH = it
                        useWeatherSource = false
                    }
                    StressSliderRow("Dryness Indicator D_t (Weight 0.35 → +${calc.drynessWeighted})", drynessD) {
                        drynessD = it
                        useWeatherSource = false
                    }
                    StressSliderRow("Observation Stress O_t (Weight 0.20 → +${calc.observationWeighted})", obsO) {
                        obsO = it
                    }
                    StressSliderRow("Elapsed Days Δt (${deltaDays.roundToInt()} days)", deltaDays, 1f..30f) {
                        deltaDays = it
                    }
                    StressSliderRow("Experimental Half-Life (${halfLifeDays.roundToInt()}d • λ=${String.format(Locale.US, "%.4f", calc.decayConstantLambda)})", halfLifeDays, 7f..90f) {
                        halfLifeDays = it
                    }

                    Text(
                        text = "Data Source: ${calc.dataSourceLabel} • Experimental modelling assumption: 30-day half-life exponential decay is an environmental telemetry proxy, not a clinically validated biological decay rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    if (targetTree != null) {
                        Button(
                            onClick = { onRecordStressSnapshot(targetTree!!, calc) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ForestAbyssBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("record_stress_snapshot_button")
                        ) {
                            Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Commit Dated Stress Snapshot for ${targetTree!!.treeCode}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACCUMULATION & DECAY HISTORY",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    StressDebtLineChart(records = uiState.stressRecords)
                }
            }
        }
    }
}

@Composable
private fun SearchGroundedIntelligenceSubScreen(
    groundedSearchState: AsyncTaskState<GroundedSpeciesIntelligence>,
    onRunGroundedSearch: (String) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val context = LocalContext.current
    var queryInput by remember {
        mutableStateOf("Azadirachta indica Neem and Ficus benghalensis urban tree care and monsoon cyclone pruning guidelines in Andhra Pradesh")
    }

    val quickQueries = listOf(
        "Neem & Banyan urban cyclone resilience in Andhra Pradesh",
        "Identifying Ganoderma root rot & bark fissures in Indian street trees",
        "Vijayawada urban heat island mitigation with native canopy species"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "GOOGLE SEARCH GROUNDED FORESTRY INTELLIGENCE",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Powered by gemini-3.8-flash with live Google Search Grounding",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        label = { Text("Botanical, Pest, or Municipal Forestry Query") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_grounding_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickQueries) { q ->
                            FilterChip(
                                selected = queryInput == q,
                                onClick = {
                                    queryInput = q
                                    onRunGroundedSearch(q)
                                },
                                label = { Text(q, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ForestCardElevated
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { onRunGroundedSearch(queryInput) },
                        enabled = groundedSearchState !is AsyncTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ForestAbyssBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_search_grounding_button")
                    ) {
                        if (groundedSearchState is AsyncTaskState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ForestAbyssBg,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching Google & Synthesizing Briefing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Grounded Forestry Research", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            when (groundedSearchState) {
                is AsyncTaskState.Idle -> {}
                is AsyncTaskState.Loading -> {}
                is AsyncTaskState.Error -> {
                    Surface(
                        color = PriorityCriticalRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PriorityCriticalRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Grounded Search Error: ${groundedSearchState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PriorityCriticalRed,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is AsyncTaskState.Success -> {
                    val data = groundedSearchState.data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GROUNDED ARBORICULTURE BRIEFING",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                                IconButton(onClick = { onSpeakText(data.summaryMarkdown) }) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Read Grounded Briefing Aloud",
                                        tint = ElectricCyan
                                    )
                                }
                            }
                            Text(
                                text = data.summaryMarkdown,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            if (data.sourceTitlesAndUrls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Google Search Grounding Citations:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                data.sourceTitlesAndUrls.forEach { (title, url) ->
                                    Surface(
                                        color = ForestAbyssBg,
                                        border = BorderStroke(1.dp, ForestBorderSubtle),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                } catch (_: Exception) {}
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ElectricCyan,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Open source link",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StressSliderRow(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    onValueChange: (Double) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("${value.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = range
        )
    }
}

/**
 * AI-Driven Urban Forest Intelligence Sub-Screen.
 * Utilizes the integrated Gemini 3.8 Flash API to interpret the registered tree inventory,
 * regional Open-Meteo weather telemetry, and maintenance budget constraints.
 */
@Composable
private fun UrbanForestAiInsightsSubScreen(
    uiState: TreeGuardUiState,
    insightsState: AsyncTaskState<UrbanForestAiInsights>,
    onFetchInsights: () -> Unit,
    onSpeakText: (String) -> Unit
) {
    val totalTrees = uiState.trees.size
    val criticalCount = uiState.trees.count { it.currentPriority.equals("Critical", true) }
    val needsInspectionCount = uiState.trees.count { it.visualCondition.equals("Needs inspection", true) }
    val avgStressDebt = if (uiState.trees.isNotEmpty()) uiState.trees.map { it.stressDebt }.average() else 0.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().testTag("urban_forest_ai_insights_subscreen")
    ) {
        // 1. Executive AI Intelligence Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("ai_insights_hero_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI URBAN FOREST INTELLIGENCE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                            }
                            Text(
                                text = "Gemini 3.8 Flash Data Synthesis & Interpretation",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (insightsState is AsyncTaskState.Success) {
                            IconButton(
                                onClick = {
                                    val ins = insightsState.data
                                    val speech = "${ins.title}. Canopy Assessment: ${ins.canopyHealthAssessment}. " +
                                        "Key Risks: ${ins.keyRiskObservations.joinToString(". ")}. " +
                                        "Climatic Stress: ${ins.climaticStressInterpretation}. " +
                                        "Prioritization Advice: ${ins.maintenancePrioritizationAdvice}."
                                    onSpeakText(speech)
                                },
                                modifier = Modifier.testTag("speak_forest_insights_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Read AI Insights Aloud",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }

                    Text(
                        text = "Synthesizes $totalTrees registered trees in Vijayawada with live Open-Meteo weather telemetry (Temp ${uiState.weather?.temperature ?: 32.5}°C, Gusts ${uiState.weather?.windGusts ?: 28.0} km/h), longitudinal Stress Debt, and municipal budget constraints.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Button(
                        onClick = onFetchInsights,
                        enabled = insightsState !is AsyncTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = ForestAbyssBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("fetch_ai_insights_button")
                    ) {
                        if (insightsState is AsyncTaskState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ForestAbyssBg,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini is Interpreting Forest Data...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (insightsState is AsyncTaskState.Success) "Regenerate AI Insights" else "Fetch AI-Driven Insights (Gemini)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Idle State - Explanatory Overview
        if (insightsState is AsyncTaskState.Idle) {
            item {
                Surface(
                    color = ForestCardElevated,
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DATA SOURCES INTERPRETED BY GEMINI 3.8 FLASH",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = "• Registered Tree Inventory: $totalTrees trees, $criticalCount critical escalations, $needsInspectionCount trees requiring inspection.\n" +
                                "• Climatic Context: Open-Meteo live observations (${uiState.weather?.locationLabel ?: "Vijayawada Sector"}).\n" +
                                "• Longitudinal Stress Debt: Average accumulated debt of ${String.format(Locale.US, "%.1f", avgStressDebt)} pts across a 30-day half-life decay model.\n" +
                                "• Municipal Resource Optimization: ₹${uiState.budgetInr.toInt()} demonstration scenario budget.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Fetch AI-Driven Insights' above to trigger the live multimodal reasoning pipeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Error State
        if (insightsState is AsyncTaskState.Error) {
            item {
                Surface(
                    color = PriorityCriticalRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PriorityCriticalRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AI Interpretation Error", style = MaterialTheme.typography.titleSmall, color = PriorityCriticalRed)
                        Text(insightsState.message, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        TextButton(onClick = onFetchInsights) {
                            Text("Retry Analysis", color = PriorityCriticalRed)
                        }
                    }
                }
            }
        }

        // 4. Success State - Structured AI Findings
        if (insightsState is AsyncTaskState.Success) {
            val data = insightsState.data

            // Executive Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardElevated),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_canopy_health_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CANOPY HEALTH ASSESSMENT",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary
                            )
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, EmeraldPrimary)
                            ) {
                                Text(
                                    text = "SYNTHESIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = data.canopyHealthAssessment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Key Risk Observations Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_key_risks_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRIORITY TREE RISK OBSERVATIONS (${data.keyRiskObservations.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = WarningAmber
                        )
                        data.keyRiskObservations.forEachIndexed { idx, obs ->
                            Surface(
                                color = ForestAbyssBg,
                                border = BorderStroke(1.dp, ForestBorderSubtle),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = WarningAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = obs,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Climatic Stress & Weather Interpretation Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_climatic_stress_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CLIMATIC STRESS & WEATHER INTERPRETATION",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = data.climaticStressInterpretation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Maintenance Prioritization Advice Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_maintenance_advice_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "RESOURCE ALLOCATION & BUDGET ADVICE (₹${uiState.budgetInr.toInt()})",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = data.maintenancePrioritizationAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Proactive Recommendations Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_recommendations_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PROACTIVE MUNICIPAL FORESTRY RECOMMENDATIONS",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        data.preventativeRecommendations.forEach { rec ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Certified Arborist & Limitations Disclaimer
            item {
                Surface(
                    color = ForestAbyssBg,
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ARBORIST SAFETY DISCLAIMER",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = data.limitationsDisclaimer,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

