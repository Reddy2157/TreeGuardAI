package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.StressRecord
import com.example.data.TreeRecord
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestAbyssBg
import com.example.ui.theme.ForestBorderSubtle
import com.example.ui.theme.ForestCardBg
import com.example.ui.theme.PriorityCriticalRed
import com.example.ui.theme.PriorityHighOrange
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityModerateYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

fun priorityColor(priority: String): Color = when (priority.lowercase()) {
    "critical" -> PriorityCriticalRed
    "high" -> PriorityHighOrange
    "moderate" -> PriorityModerateYellow
    else -> PriorityLowGreen
}

fun conditionColor(condition: String): Color = when (condition) {
    "Needs inspection" -> PriorityCriticalRed
    "Monitor" -> WarningAmber
    "Unclear" -> ElectricCyan
    else -> EmeraldPrimary
}

@Composable
fun PriorityBadge(priority: String, score: Double? = null, modifier: Modifier = Modifier) {
    val color = priorityColor(priority)
    Surface(
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (score != null) "${priority.uppercase()} • ${score.toInt()}" else priority.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DataOriginBadge(isDemo: Boolean, modifier: Modifier = Modifier) {
    val color = if (isDemo) WarningAmber else ElectricCyan
    val text = if (isDemo) "DEMO DATA" else "LIVE CLOUD / GPS"
    Surface(
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AiSafetyDisclaimerBanner(
    modifier: Modifier = Modifier,
    onSpeakDisclaimer: (() -> Unit)? = null
) {
    Surface(
        color = WarningAmber.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Safety Disclaimer",
                tint = WarningAmber,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "AI-generated visual screening is an initial observation tool, not a certified arborist assessment. Final tree-health and safety decisions must be made by qualified professionals.",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (onSpeakDisclaimer != null) {
                IconButton(onClick = onSpeakDisclaimer) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Read safety disclaimer aloud",
                        tint = ElectricCyan
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityDistributionChart(
    trees: List<TreeRecord>,
    modifier: Modifier = Modifier
) {
    val total = trees.size.coerceAtLeast(1)
    val lowCount = trees.count { it.currentPriority.equals("Low", true) }
    val modCount = trees.count { it.currentPriority.equals("Moderate", true) }
    val highCount = trees.count { it.currentPriority.equals("High", true) }
    val critCount = trees.count { it.currentPriority.equals("Critical", true) }

    val items = listOf(
        Triple("Low (0–24)", lowCount, PriorityLowGreen),
        Triple("Moderate (25–49)", modCount, PriorityModerateYellow),
        Triple("High (50–74)", highCount, PriorityHighOrange),
        Triple("Critical (75–100)", critCount, PriorityCriticalRed)
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, count, color) ->
            val fraction = (count.toFloat() / total.toFloat()).coerceIn(0.04f, 1f)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        text = "$count trees (${((count * 100f) / total).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = ForestAbyssBg,
                        size = size,
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = color,
                        size = Size(size.width * (if (count == 0) 0.02f else fraction), size.height),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }
    }
}

@Composable
fun StressDebtLineChart(
    records: List<StressRecord>,
    modifier: Modifier = Modifier
) {
    val points = if (records.size >= 2) {
        records.map { it.stressDebt.toFloat() }
    } else {
        listOf(32f, 44f, 58f, 64f, 74.6f)
    }
    val labels = if (records.size >= 2) {
        records.map { it.date.takeLast(5) }
    } else {
        listOf("D-30", "D-20", "D-14", "D-7", "Now")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val maxVal = (points.maxOrNull() ?: 100f).coerceAtLeast(100f)
            val stepX = size.width / (points.size - 1).coerceAtLeast(1)

            // Horizontal reference grid lines
            for (i in 0..3) {
                val y = size.height * (i / 3f)
                drawLine(
                    color = ForestBorderSubtle.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - ((value / maxVal) * (size.height * 0.85f)) - (size.height * 0.07f)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (index == points.lastIndex) {
                    fillPath.lineTo(x, size.height)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ElectricCyan.copy(alpha = 0.30f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = ElectricCyan,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - ((value / maxVal) * (size.height * 0.85f)) - (size.height * 0.07f)
                drawCircle(color = ForestAbyssBg, radius = 6.dp.toPx(), center = Offset(x, y))
                drawCircle(color = EmeraldPrimary, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { lbl ->
                Text(text = lbl, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

@Composable
fun TreePassportQrMatrix(
    treeCode: String,
    modifier: Modifier = Modifier
) {
    // Deterministic 15x15 QR-style matrix generated from treeCode hash
    val gridSize = 15
    val seed = treeCode.hashCode()

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.size(136.dp)
    ) {
        Canvas(modifier = Modifier.padding(10.dp)) {
            val cellSize = size.width / gridSize
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val isFinderTopLeft = r < 4 && c < 4
                    val isFinderTopRight = r < 4 && c >= gridSize - 4
                    val isFinderBottomLeft = r >= gridSize - 4 && c < 4
                    val isFilled = when {
                        isFinderTopLeft || isFinderTopRight || isFinderBottomLeft -> {
                            r == 0 || r == 3 || r == gridSize - 1 || r == gridSize - 4 ||
                                c == 0 || c == 3 || c == gridSize - 1 || c == gridSize - 4 ||
                                (r in 1..2 && c in 1..2) ||
                                (r in 1..2 && c in (gridSize - 3)..(gridSize - 2)) ||
                                (r in (gridSize - 3)..(gridSize - 2) && c in 1..2)
                        }
                        else -> ((seed shr ((r * gridSize + c) % 28)) xor (r * 31 + c * 17)) and 1 == 1
                    }
                    if (isFilled) {
                        drawRect(
                            color = ForestAbyssBg,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.92f, cellSize * 0.92f)
                        )
                    }
                }
            }
        }
    }
}
