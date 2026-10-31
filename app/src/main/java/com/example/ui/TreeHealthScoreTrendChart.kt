package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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

data class DailyHealthPoint(
    val dayNumber: Int,
    val dayLabel: String,
    val healthScore: Float,
    val weatherEvent: String,
    val note: String
)

enum class HealthChartFilter(val label: String) {
    FOREST_AVG("Forest Average"),
    NEEM_TG01("TG-0001 (Neem)"),
    BANYAN_TG02("TG-0002 (Banyan)"),
    PEEPAL_TG04("TG-0004 (Peepal)")
}

/**
 * 30-Day Tree Health Score Trend Chart
 * Native Jetpack Compose Canvas implementation delivering an intuitive, interactive D3/Recharts-style
 * longitudinal line & area chart with threshold bands and scrubbable tooltips.
 */
@Composable
fun TreeHealthScoreTrend30DaysChart(
    trees: List<TreeRecord>,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(HealthChartFilter.FOREST_AVG) }

    // Generate 30 days of data points reflecting Vijayawada microclimate and tree conditions
    val dataPoints = remember(activeFilter, trees) {
        generate30DayHealthTrajectory(activeFilter, trees)
    }

    // Interactive scrub index (default to the latest day: 29, representing "Today")
    var selectedIndex by remember(activeFilter) { mutableIntStateOf(29) }
    val currentPoint = dataPoints.getOrElse(selectedIndex) { dataPoints.last() }

    val minScore = remember(dataPoints) { dataPoints.minOf { it.healthScore } }
    val maxScore = remember(dataPoints) { dataPoints.maxOf { it.healthScore } }
    val latestScore = dataPoints.last().healthScore
    val firstScore = dataPoints.first().healthScore
    val netChange = latestScore - firstScore

    Card(
        colors = CardDefaults.cardColors(containerColor = ForestCardBg),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("health_score_trend_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 1. Header with title and easy subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "30-DAY TREE HEALTH SCORE TREND",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Canopy Vigor & Physiological Resilience Index (0–100)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = healthScoreColor(latestScore).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, healthScoreColor(latestScore)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", latestScore)} / 100",
                        style = MaterialTheme.typography.titleSmall,
                        color = healthScoreColor(latestScore),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. High-Level KPI Summary Strip (makes it immediately understandable to non-technical users)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryKpiPill(
                    label = "Current",
                    value = "${latestScore.roundToInt()}/100",
                    color = healthScoreColor(latestScore),
                    modifier = Modifier.weight(1f)
                )
                SummaryKpiPill(
                    label = "30d Low",
                    value = "${minScore.roundToInt()}/100",
                    color = PriorityCriticalRed,
                    modifier = Modifier.weight(1f)
                )
                SummaryKpiPill(
                    label = "30d High",
                    value = "${maxScore.roundToInt()}/100",
                    color = PriorityLowGreen,
                    modifier = Modifier.weight(1f)
                )
                SummaryKpiPill(
                    label = "30d Trajectory",
                    value = "${if (netChange >= 0) "+" else ""}${String.format(Locale.US, "%.1f", netChange)}",
                    color = if (netChange >= 0) EmeraldPrimary else WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Filter Chips: Forest Average or Specific Key Trees
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HealthChartFilter.entries.forEach { filter ->
                    val isSelected = activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeFilter = filter
                            selectedIndex = 29
                        },
                        label = {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                            selectedLabelColor = EmeraldPrimary,
                            containerColor = ForestCardElevated,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else ForestBorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interactive Canvas Chart (Canvas with threshold zones, line curve, touch scrubber)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ForestAbyssBg)
                    .testTag("health_score_canvas_box")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .pointerInput(dataPoints) {
                            detectTapGestures { offset ->
                                val pointSpacing = size.width / (dataPoints.size - 1)
                                val tappedIndex = ((offset.x + pointSpacing / 2) / pointSpacing).toInt()
                                selectedIndex = tappedIndex.coerceIn(0, dataPoints.size - 1)
                            }
                        }
                        .pointerInput(dataPoints) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val pointSpacing = size.width / (dataPoints.size - 1)
                                val draggedIndex = ((change.position.x + pointSpacing / 2) / pointSpacing).toInt()
                                selectedIndex = draggedIndex.coerceIn(0, dataPoints.size - 1)
                            }
                        }
                ) {
                    drawHealthChart(
                        points = dataPoints,
                        selectedIndex = selectedIndex
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Interactive Inspection Tooltip / Callout (Changes dynamically on scrub/tap)
            Surface(
                color = ForestCardElevated,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scrub_tooltip")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentPoint.dayLabel} (Day ${currentPoint.dayNumber} of 30)",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(healthScoreColor(currentPoint.healthScore), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Health: ${String.format(Locale.US, "%.1f", currentPoint.healthScore)} / 100",
                                style = MaterialTheme.typography.titleSmall,
                                color = healthScoreColor(currentPoint.healthScore),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Meteorological Context: ${currentPoint.weatherEvent}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )

                    Text(
                        text = "Forest Observation: ${currentPoint.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Intuitive Threshold Guide (so everyone understands what 0–100 means)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendBand(color = PriorityLowGreen, label = "80-100: Thriving")
                LegendBand(color = WarningAmber, label = "60-79: Moderate Stress")
                LegendBand(color = PriorityCriticalRed, label = "<60: Needs Inspection")
            }
        }
    }
}

@Composable
private fun SummaryKpiPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ForestCardElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ForestBorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LegendBand(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private fun DrawScope.drawHealthChart(
    points: List<DailyHealthPoint>,
    selectedIndex: Int
) {
    if (points.size < 2) return

    val w = size.width
    val h = size.height

    // Score scale: 0 to 100
    fun yForScore(score: Float): Float {
        val clamped = score.coerceIn(0f, 100f)
        return h - (clamped / 100f) * h
    }

    // 1. Draw horizontal threshold guide lines (at 80, 60, 40)
    val gridColor = Color.White.copy(alpha = 0.08f)
    listOf(80f, 60f, 40f).forEach { score ->
        val y = yForScore(score)
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }

    // 2. Build line path & area path
    val stepX = w / (points.size - 1)
    val linePath = Path()
    val areaPath = Path()

    points.forEachIndexed { i, p ->
        val x = i * stepX
        val y = yForScore(p.healthScore)

        if (i == 0) {
            linePath.moveTo(x, y)
            areaPath.moveTo(x, h)
            areaPath.lineTo(x, y)
        } else {
            // Cubic bezier smoothing
            val prevX = (i - 1) * stepX
            val prevY = yForScore(points[i - 1].healthScore)
            val cx1 = prevX + (x - prevX) * 0.45f
            val cx2 = prevX + (x - prevX) * 0.55f
            linePath.cubicTo(cx1, prevY, cx2, y, x, y)
            areaPath.cubicTo(cx1, prevY, cx2, y, x, y)
        }
    }

    // Close area gradient path at the bottom
    areaPath.lineTo(w, h)
    areaPath.close()

    // Draw lush gradient area below the curve
    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                EmeraldPrimary.copy(alpha = 0.35f),
                ElectricCyan.copy(alpha = 0.12f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h
        )
    )

    // Draw the continuous trend line
    drawPath(
        path = linePath,
        color = EmeraldPrimary,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 3. Draw Scrubber Vertical Line & Highlight Point
    val scrubX = selectedIndex * stepX
    val selectedPoint = points[selectedIndex]
    val scrubY = yForScore(selectedPoint.healthScore)

    // Vertical dashed marker line at selected day
    drawLine(
        color = ElectricCyan.copy(alpha = 0.75f),
        start = Offset(scrubX, 0f),
        end = Offset(scrubX, h),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
    )

    // Outer glow circle
    drawCircle(
        color = healthScoreColor(selectedPoint.healthScore).copy(alpha = 0.35f),
        radius = 11.dp.toPx(),
        center = Offset(scrubX, scrubY)
    )

    // Inner filled point
    drawCircle(
        color = healthScoreColor(selectedPoint.healthScore),
        radius = 5.dp.toPx(),
        center = Offset(scrubX, scrubY)
    )

    drawCircle(
        color = Color.White,
        radius = 2.dp.toPx(),
        center = Offset(scrubX, scrubY)
    )
}

private fun healthScoreColor(score: Float): Color {
    return when {
        score >= 80f -> PriorityLowGreen
        score >= 60f -> WarningAmber
        else -> PriorityCriticalRed
    }
}

/**
 * Generates synthetic yet realistic 30-day health score trajectory for Vijayawada
 * representing pre-monsoon baseline, cyclonic gale dip, and arborist recovery.
 */
private fun generate30DayHealthTrajectory(
    filter: HealthChartFilter,
    trees: List<TreeRecord>
): List<DailyHealthPoint> {
    val result = ArrayList<DailyHealthPoint>(30)

    for (day in 1..30) {
        val dateLabel = when (day) {
            1 -> "30 Days Ago"
            10 -> "20 Days Ago"
            12 -> "Storm Gale"
            20 -> "10 Days Ago"
            30 -> "Today"
            else -> "Day $day"
        }

        // Simulating the 30-day trajectory:
        // Days 1-10: Baseline healthy (82-84)
        // Day 11-13: Monsoon storm gusts in Vijayawada causing limb breakage & foliar loss
        // Day 14-22: Post-storm inspection & municipal stabilization
        // Day 23-30: Canopy regrowth & stabilization to current status
        val baseScore: Float = when (filter) {
            HealthChartFilter.FOREST_AVG -> {
                when {
                    day <= 10 -> 82.5f + ((day % 3) * 0.4f)
                    day == 11 -> 72.0f
                    day == 12 -> 62.5f // Major Storm Event
                    day == 13 -> 63.8f
                    day in 14..18 -> 67.0f + ((day - 14) * 0.9f)
                    day in 19..25 -> 71.5f + ((day - 19) * 0.5f)
                    else -> 74.8f + ((day - 26) * 0.3f)
                }
            }
            HealthChartFilter.NEEM_TG01 -> {
                // Neem experienced the direct snapped limb over bus bay on Day 12
                when {
                    day <= 10 -> 80.0f
                    day == 11 -> 65.0f
                    day == 12 -> 48.0f // Critical branch damage
                    day in 13..18 -> 51.0f + ((day - 13) * 0.6f)
                    day in 19..25 -> 54.0f + ((day - 19) * 0.5f)
                    else -> 57.5f // Needs inspection
                }
            }
            HealthChartFilter.BANYAN_TG02 -> {
                // Banyan suffered compaction & chlorosis
                when {
                    day <= 10 -> 76.0f
                    day in 11..13 -> 66.0f
                    day in 14..22 -> 67.5f + ((day - 14) * 0.3f)
                    else -> 69.8f
                }
            }
            HealthChartFilter.PEEPAL_TG04 -> {
                // Peepal remained sturdy and resilient
                when {
                    day <= 10 -> 91.0f
                    day in 11..13 -> 85.0f
                    day in 14..22 -> 87.5f
                    else -> 90.2f
                }
            }
        }

        val weather = when {
            day == 12 -> "Monsoon Gale (Wind 48 km/h • Rain 62 mm)"
            day == 13 -> "Post-Storm Saturated Soil (Humidity 92%)"
            day in 1..10 -> "Pre-Monsoon High Heat (35°C • Moderate Wind)"
            day in 20..25 -> "Pleasant Sunlight & Recovery (31°C • Light Breeze)"
            else -> "Seasonal Average (32°C • Humidity 68%)"
        }

        val note = when {
            day == 12 -> "Peak storm stress: branch snapped on TG-VJA-0001 over Benz Circle bus bay."
            day == 13 -> "Municipal arborist alert generated; emergency barrier placed."
            day == 17 -> "Initial crown thinning and clearance completed."
            day == 24 -> "Foliar recovery flush observed across urban canopy."
            day == 30 -> "Active monitoring state; average forest resilience holding steady."
            else -> "Routine longitudinal canopy telemetry monitoring."
        }

        result.add(
            DailyHealthPoint(
                dayNumber = day,
                dayLabel = dateLabel,
                healthScore = baseScore,
                weatherEvent = weather,
                note = note
            )
        )
    }

    return result
}
