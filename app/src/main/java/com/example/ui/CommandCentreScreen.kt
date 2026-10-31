package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.data.SampleTreeBitmaps
import com.example.data.TreeImageStorage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TreeRecord
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestAbyssBg
import com.example.ui.theme.ForestBorderSubtle
import com.example.ui.theme.ForestCardBg
import com.example.ui.theme.ForestCardElevated
import com.example.ui.theme.PriorityCriticalRed
import com.example.ui.theme.PriorityHighOrange
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityModerateYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CommandCentreScreen(
    uiState: TreeGuardUiState,
    isSeedingCloud: Boolean,
    onSeedCloud: () -> Unit,
    onRefreshWeather: () -> Unit,
    onOpenTreePassport: (TreeRecord) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToIntelligence: () -> Unit,
    onSpeakSummary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trees = uiState.trees
    val totalTrees = trees.size
    val needsInspectionCount = trees.count {
        it.visualCondition == "Needs inspection" || it.currentPriority.equals("Critical", true)
    }
    val highPriorityCount = trees.count {
        it.currentPriority.equals("High", true) || it.currentPriority.equals("Critical", true)
    }
    val monitorCount = trees.count { it.visualCondition == "Monitor" }
    val avgStressDebt = if (trees.isNotEmpty()) trees.map { it.stressDebt }.average() else 0.0
    val opt = uiState.optimizationResult
    val budgetUtilPercent = if (opt.budgetInr > 0) ((opt.totalEstimatedExpenditure / opt.budgetInr) * 100.0).roundToInt() else 0
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ForestAbyssBg)
            .testTag("command_centre_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Command Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ForestCardElevated, ForestAbyssBg)
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = EmeraldPrimary.copy(alpha = 0.08f),
                            radius = size.minDimension * 0.55f,
                            center = Offset(size.width * 0.88f, size.height * 0.22f)
                        )
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.06f),
                            radius = size.minDimension * 0.35f,
                            center = Offset(size.width * 0.78f, size.height * 0.75f)
                        )
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DataOriginBadge(isDemo = uiState.isUsingDemoFallback)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val briefing = "TreeGuard AI Command Centre for Vijayawada. " +
                                            "Monitoring $totalTrees registered trees. " +
                                            "$needsInspectionCount trees require professional arborist inspection. " +
                                            "Average experimental Stress Debt is ${String.format(Locale.US, "%.1f", avgStressDebt)}. " +
                                            "Maintenance budget utilization is $budgetUtilPercent percent."
                                        onSpeakSummary(briefing)
                                    },
                                    modifier = Modifier.testTag("tts_command_briefing_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Speak Command Centre Briefing (TTS)",
                                        tint = ElectricCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "URBAN FOREST COMMAND CENTRE",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sector: Vijayawada, Andhra Pradesh • Longitudinal Telemetry & Risk Prioritization",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        if (uiState.isUsingDemoFallback) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = WarningAmber.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Displaying Labelled Demo Inventory",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = WarningAmber
                                        )
                                        Text(
                                            text = "Tap to persist the Vijayawada seed records into your live Cloud Firestore database.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = onSeedCloud,
                                        enabled = !isSeedingCloud,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = ForestAbyssBg
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("seed_demo_cloud_button")
                                    ) {
                                        if (isSeedingCloud) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = ForestAbyssBg,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CloudSync,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sync Demo", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Prominent Alert Section for Trees Requiring Professional Inspection
        val criticalTrees = trees.filter {
            it.visualCondition == "Needs inspection" || it.currentPriority.equals("Critical", true)
        }
        if (criticalTrees.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PriorityCriticalRed.copy(alpha = 0.14f)),
                    border = BorderStroke(1.5.dp, PriorityCriticalRed.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Critical Inspection Alert",
                                tint = PriorityCriticalRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROFESSIONAL ARBORIST ESCALATION ALERT (${criticalTrees.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = PriorityCriticalRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        criticalTrees.forEach { alertTree ->
                            Surface(
                                color = ForestAbyssBg.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onOpenTreePassport(alertTree) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val alertBmp = TreeImageStorage.getTreeImage(context, alertTree.treeCode)
                                        ?: SampleTreeBitmaps.getTreeBitmap(
                                            treeCode = alertTree.treeCode,
                                            species = alertTree.species,
                                            visualCondition = alertTree.visualCondition
                                        )
                                    Image(
                                        bitmap = alertBmp.asImageBitmap(),
                                        contentDescription = "Visual inspection photo of ${alertTree.species}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, PriorityCriticalRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${alertTree.treeCode} • ${alertTree.species}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = alertTree.zone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = alertTree.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarningAmber,
                                            maxLines = 2
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    PriorityBadge(
                                        priority = alertTree.currentPriority,
                                        score = alertTree.currentRiskScore
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Key Telemetry Summary Cards ('Total Trees', 'High-Priority Trees', and Operational KPIs)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Total Trees",
                        value = "$totalTrees",
                        subtitle = if (uiState.isUsingDemoFallback) "Demo Seed Active" else "Firestore Synced",
                        accentColor = EmeraldPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_total_trees")
                    )
                    MetricStatCard(
                        title = "High-Priority Trees",
                        value = "$highPriorityCount",
                        subtitle = "Priority Index R ≥ 50",
                        accentColor = PriorityCriticalRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_high_priority_trees")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Needs Inspection",
                        value = "$needsInspectionCount",
                        subtitle = "Arborist Escalation",
                        accentColor = PriorityHighOrange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_needs_inspection")
                    )
                    MetricStatCard(
                        title = "Monitor Status",
                        value = "$monitorCount",
                        subtitle = "Scheduled Checks",
                        accentColor = WarningAmber,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_monitor_status")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Stress Debt (Avg)",
                        value = String.format(Locale.US, "%.1f", avgStressDebt),
                        subtitle = "30d Half-Life Model",
                        accentColor = ElectricCyan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_stress_debt")
                    )
                    MetricStatCard(
                        title = "Budget Utilized",
                        value = "$budgetUtilPercent%",
                        subtitle = "₹${opt.totalEstimatedExpenditure.toInt()} / ₹${opt.budgetInr.toInt()}",
                        accentColor = EmeraldPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summary_card_budget_utilized")
                    )
                }
            }
        }

        // 4. Regional Environmental Conditions (Open-Meteo Live Telemetry & 7-Day Forecast)
        item {
            val w = uiState.weather
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WEATHER INTELLIGENCE MODULE (OPEN-METEO)",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = w?.locationLabel ?: "Vijayawada, Andhra Pradesh (16.5062, 80.6480)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = onRefreshWeather,
                            modifier = Modifier.testTag("refresh_weather_button")
                        ) {
                            if (uiState.isWeatherLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ElectricCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Open-Meteo Weather",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (w != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeatherChip(
                                icon = Icons.Default.Thermostat,
                                label = "Temp",
                                value = "${w.temperature}°C"
                            )
                            WeatherChip(
                                icon = Icons.Default.WaterDrop,
                                label = "Hum / Rain",
                                value = "${w.humidity.toInt()}% / ${w.precipitation}mm"
                            )
                            WeatherChip(
                                icon = Icons.Default.Air,
                                label = "Wind / Gust",
                                value = "${w.windSpeed.toInt()}/${w.windGusts.toInt()} km/h"
                            )
                            WeatherChip(
                                icon = Icons.Default.Forest,
                                label = "Soil Moist",
                                value = if (w.soilMoisture != null) "${String.format(Locale.US, "%.2f", w.soilMoisture)} m³/m³" else "Proxy"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ForestAbyssBg,
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Engine Inputs: H_t=${w.derivedHeatIndicator} • D_t=${w.derivedDrynessIndicator} • W=${w.derivedWeatherPriorityScore}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = w.cacheStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                        }

                        if (w.forecast7Day.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "7-Day Regional Forecast & Projected Canopy Stress (Open-Meteo)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(w.forecast7Day) { day ->
                                    Surface(
                                        color = ForestAbyssBg,
                                        border = BorderStroke(1.dp, ForestBorderSubtle),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = day.date.takeLast(5),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricCyan
                                            )
                                            Text(
                                                text = "${day.maxTemp.toInt()}°/${day.minTemp.toInt()}°",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${day.precipitationSum}mm • ${day.maxWindGusts.toInt()}km/h",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "W=${day.estimatedDailyWeatherRisk.roundToInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${w.fetchedAtLabel} • Note: Regional meteorological context, not a direct measurement of an individual tree's microclimate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // 5. 30-Day Tree Health Score Trend Chart (Intuitive D3/Recharts-style visual telemetry)
        item {
            TreeHealthScoreTrend30DaysChart(trees = trees)
        }

        // 6. Registered Urban Tree Photo Gallery (Resolves missing tree images on Command Centre)
        item {
            UrbanTreeVisualGalleryCard(
                trees = trees,
                onOpenTreePassport = onOpenTreePassport,
                onNavigateToScan = onNavigateToScan
            )
        }

        // 7. Tree Inspection-Priority Distribution & Stress Debt Visualization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INSPECTION-PRIORITY DISTRIBUTION (R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T)",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PriorityDistributionChart(trees = trees)
                }
            }
        }

        item {
            var showPlaceholderMode by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stress_debt_visualization_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stress Debt",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Longitudinal TD_t = λTD_(t-1) + S_t • 30-day half-life decay",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                        }
                        OutlinedButton(
                            onClick = onNavigateToIntelligence,
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                        ) {
                            Text("Simulator", color = ElectricCyan, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showPlaceholderMode,
                            onClick = { showPlaceholderMode = false },
                            label = { Text("Live Telemetry Curve", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = showPlaceholderMode,
                            onClick = { showPlaceholderMode = true },
                            label = { Text("Visualization Placeholder", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (showPlaceholderMode || uiState.stressRecords.isEmpty()) {
                        StressDebtVisualizationPlaceholder(
                            averageStressDebt = avgStressDebt,
                            treeCount = totalTrees
                        )
                    } else {
                        StressDebtLineChart(records = uiState.stressRecords)
                    }
                }
            }
        }

        // 6. Maintenance Budget Allocation Preview & Quick Module Navigation Cards
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WHAT-IF MAINTENANCE ALLOCATION",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "${opt.selectedActions.size} Funded Actions • ${opt.treesReceivingAttentionCount} Trees • Benefit Score ${opt.expectedAggregateBenefit.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = onNavigateToIntelligence) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Maintenance Optimizer",
                                tint = EmeraldPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (budgetUtilPercent / 100f).coerceIn(0f, 1f) },
                        color = EmeraldPrimary,
                        trackColor = ForestAbyssBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Demo Budget: ₹${opt.budgetInr.toInt()} • Allocated: ₹${opt.totalEstimatedExpenditure.toInt()} • Remaining: ₹${opt.remainingBudget.toInt()} (Synthetic demo estimates)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // 7. Interactive Map Preview & Quick Action Launchers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardElevated),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToMap() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("GIS Tree Map", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "Inspect $totalTrees Vijayawada markers",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardElevated),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToScan() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI Tree Doctor", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "Scan canopy & bark with Gemini",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 8. Recent AI Screenings & Activity Timeline
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECENT OBSERVATIONS & SCREENING TIMELINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    uiState.observations.take(4).forEach { obs ->
                        Surface(
                            color = ForestAbyssBg,
                            border = BorderStroke(1.dp, ForestBorderSubtle),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    trees.firstOrNull { it.id == obs.treeId || it.treeCode == obs.treeCode }
                                        ?.let(onOpenTreePassport)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val obsBmp = TreeImageStorage.getTreeImage(context, obs.treeCode)
                                    ?: SampleTreeBitmaps.getTreeBitmap(
                                        treeCode = obs.treeCode,
                                        species = obs.possibleSpecies,
                                        visualCondition = obs.visualCondition
                                    )
                                Image(
                                    bitmap = obsBmp.asImageBitmap(),
                                    contentDescription = "Observation plate of ${obs.treeCode}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, ForestBorderSubtle, RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${obs.treeCode} • ${obs.source}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = obs.visualCondition,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = conditionColor(obs.visualCondition),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = obs.visibleSymptoms,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Next: ${obs.recommendedAction}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AiSafetyDisclaimerBanner()
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestCardBg),
        border = BorderStroke(1.dp, ForestBorderSubtle),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeatherChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = EmeraldPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

/**
 * Midnight Forest Console visualization placeholder for Longitudinal Tree Stress Debt (TD_t),
 * displaying the exponential decay envelope, component indicators (H_t, D_t, O_t), and summary badge.
 */
@Composable
fun StressDebtVisualizationPlaceholder(
    averageStressDebt: Double,
    treeCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ForestAbyssBg,
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("stress_debt_placeholder")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STRESS DEBT TELEMETRY CANVAS",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Text(
                    text = "Avg TD_t: ${String.format(Locale.US, "%.1f", averageStressDebt)} ($treeCount trees)",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stylized dark-theme telemetry wave & grid placeholder
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val rows = 4
                for (i in 0..rows) {
                    val y = (size.height / rows) * i
                    drawLine(
                        color = ForestBorderSubtle,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
                val samplePoints = listOf(0.68f, 0.62f, 0.54f, 0.49f, 0.38f, 0.44f, 0.32f)
                val stepX = size.width / (samplePoints.size - 1).coerceAtLeast(1)
                for (i in 0 until samplePoints.size - 1) {
                    val p1 = Offset(i * stepX, size.height * samplePoints[i])
                    val p2 = Offset((i + 1) * stepX, size.height * samplePoints[i + 1])
                    drawLine(
                        color = ElectricCyan,
                        start = p1,
                        end = p2,
                        strokeWidth = 4f
                    )
                    drawCircle(
                        color = EmeraldPrimary,
                        radius = 5f,
                        center = p1
                    )
                }
                drawCircle(
                    color = EmeraldPrimary,
                    radius = 6f,
                    center = Offset((samplePoints.size - 1) * stepX, size.height * samplePoints.last())
                )
            }

            Text(
                text = "Daily Stress S_t = 0.40·H_t (Heat) + 0.35·D_t (Dryness) + 0.25·O_t (Visual Observation) • λ = 2^(-1/30)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun UrbanTreeVisualGalleryCard(
    trees: List<TreeRecord>,
    onOpenTreePassport: (TreeRecord) -> Unit,
    onNavigateToScan: () -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = ForestCardBg),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "REGISTERED URBAN TREE PHOTOGRAPHS",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Visual Canopy & Bark Plates (${trees.size} Trees Monitored)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = onNavigateToScan,
                    border = BorderStroke(1.dp, EmeraldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Tree", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(trees) { tree ->
                    val storedBmp = remember(tree.treeCode) {
                        TreeImageStorage.getTreeImage(context, tree.treeCode)
                    }
                    val photoBmp = storedBmp ?: SampleTreeBitmaps.getTreeBitmap(
                        treeCode = tree.treeCode,
                        species = tree.species,
                        visualCondition = tree.visualCondition
                    )
                    val isRealPhoto = storedBmp != null

                    Surface(
                        color = ForestCardElevated,
                        border = BorderStroke(1.dp, ForestBorderSubtle),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .width(190.dp)
                            .clickable { onOpenTreePassport(tree) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    bitmap = photoBmp.asImageBitmap(),
                                    contentDescription = "Photo of ${tree.species}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                PriorityBadge(
                                    priority = tree.currentPriority,
                                    score = tree.currentRiskScore,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                )
                                if (isRealPhoto) {
                                    Surface(
                                        color = ForestAbyssBg.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "📸 Stored Photo",
                                            color = EmeraldPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tree.treeCode,
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tree.species,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = String.format(Locale.US, "GPS: %.4f, %.4f", tree.latitude, tree.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}


