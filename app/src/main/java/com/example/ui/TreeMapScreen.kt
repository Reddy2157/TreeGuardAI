package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.data.SampleTreeBitmaps
import com.example.data.TreeImageStorage
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import kotlin.math.hypot

/**
 * Available map rendering modes in [TreeMapScreen]:
 * - [GOOGLE_MAPS]: Google Maps Compose (`GoogleMap` + priority color-coded `Marker`s)
 * - [RADAR_GIS]: Zero-key Midnight Forest Console GIS Radar canvas
 * - [OSM_TILES]: OpenStreetMap Leaflet tile layer with attribution
 */
enum class TreeMapRenderMode(val label: String) {
    GOOGLE_MAPS("Google Maps"),
    RADAR_GIS("Radar GIS"),
    OSM_TILES("OSM Tiles")
}

/**
 * Maps a tree's Explainable Inspection-Priority category (`Critical`, `High`, `Moderate`, `Low`)
 * to a Google Maps [BitmapDescriptorFactory] marker hue (0f..360f).
 */
fun priorityToGoogleMarkerHue(priorityCategory: String): Float {
    return when (priorityCategory.trim().lowercase(Locale.US)) {
        "critical" -> BitmapDescriptorFactory.HUE_RED
        "high" -> BitmapDescriptorFactory.HUE_ORANGE
        "moderate" -> BitmapDescriptorFactory.HUE_YELLOW
        "low" -> BitmapDescriptorFactory.HUE_GREEN
        else -> BitmapDescriptorFactory.HUE_AZURE
    }
}

@Composable
fun TreeMapScreen(
    trees: List<TreeRecord>,
    selectedTree: TreeRecord?,
    onSelectTree: (TreeRecord) -> Unit,
    onOpenPassport: (TreeRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasConfiguredMapsApiKey = remember {
        val key = BuildConfig.MAPS_API_KEY
        key.isNotBlank() &&
            key != "DEFAULT_MAPS_API_KEY" &&
            !key.startsWith("YOUR_", ignoreCase = true)
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var renderMode by remember {
        mutableStateOf(
            if (hasConfiguredMapsApiKey) TreeMapRenderMode.GOOGLE_MAPS
            else TreeMapRenderMode.OSM_TILES
        )
    }

    val filteredTrees = remember(trees, selectedFilter) {
        if (selectedFilter == "All") trees
        else trees.filter { it.currentPriority.equals(selectedFilter, ignoreCase = true) }
    }

    val activeTree = selectedTree ?: filteredTrees.firstOrNull() ?: trees.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ForestAbyssBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("tree_map_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Layer Mode Switcher (Google Maps Compose / Radar GIS / OSM Tiles)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VIJAYAWADA URBAN FOREST GIS MAP",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Google Maps Compose & GIS priority markers (Low, Moderate, High, Critical)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TreeMapRenderMode.entries.forEach { mode ->
                    val isSelected = renderMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { renderMode = mode },
                        label = {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) ForestAbyssBg else ElectricCyan
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            containerColor = ForestCardBg
                        ),
                        modifier = Modifier.testTag("map_mode_${mode.name.lowercase(Locale.US)}")
                    )
                }
            }
        }

        // Priority Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            val categories = listOf(
                "All" to ElectricCyan,
                "Critical" to PriorityCriticalRed,
                "High" to PriorityHighOrange,
                "Moderate" to PriorityModerateYellow,
                "Low" to PriorityLowGreen
            )
            items(categories) { (label, color) ->
                val isSelected = selectedFilter == label
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = label },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) ForestAbyssBg else color
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color,
                        containerColor = ForestCardBg
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = color.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("map_priority_filter_${label.lowercase(Locale.US)}")
                )
            }
        }

        // Optional non-blocking notice when MAPS_API_KEY is still using the default placeholder
        if (renderMode == TreeMapRenderMode.GOOGLE_MAPS && !hasConfiguredMapsApiKey) {
            Surface(
                color = ForestCardElevated,
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("maps_api_key_notice")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Maps Compose active. Add MAPS_API_KEY in the AI Studio Secrets panel for live vector tiles, or tap 'Radar GIS' / 'OSM Tiles' anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Map Container (Google Maps Compose / Radar GIS / OpenStreetMap Tiles)
        Card(
            colors = CardDefaults.cardColors(containerColor = ForestCardBg),
            border = BorderStroke(1.5.dp, ForestBorderSubtle),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (renderMode) {
                    TreeMapRenderMode.GOOGLE_MAPS -> {
                        GoogleMapsComposeTreeCanopyView(
                            trees = filteredTrees,
                            selectedTree = activeTree,
                            onSelectTree = onSelectTree,
                            onOpenPassport = onOpenPassport
                        )
                    }
                    TreeMapRenderMode.RADAR_GIS -> {
                        InteractiveVijayawadaRadarMap(
                            trees = filteredTrees,
                            selectedTree = activeTree,
                            onSelectTree = onSelectTree
                        )
                    }
                    TreeMapRenderMode.OSM_TILES -> {
                        OpenStreetMapLeafletView(
                            trees = filteredTrees,
                            selectedTree = activeTree
                        )
                    }
                }

                // Map Legend & Coordinate Attribution Overlay
                Surface(
                    color = ForestAbyssBg.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot("Low (0-24)", PriorityLowGreen)
                            LegendDot("Mod (25-49)", PriorityModerateYellow)
                            LegendDot("High (50-74)", PriorityHighOrange)
                            LegendDot("Crit (75-100)", PriorityCriticalRed)
                        }
                        Text(
                            text = when (renderMode) {
                                TreeMapRenderMode.GOOGLE_MAPS ->
                                    "Google Maps Compose • Vijayawada Sector (16.5062°N, 80.6480°E)"
                                TreeMapRenderMode.RADAR_GIS ->
                                    "TreeGuard Canopy Radar GIS • Vijayawada Sector (16.5062°N, 80.6480°E)"
                                TreeMapRenderMode.OSM_TILES ->
                                    "Map data © OpenStreetMap contributors • Vijayawada Sector (16.5062°N, 80.6480°E)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Horizontal quick-selector strip for markers
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredTrees, key = { it.id }) { tree ->
                val isSelected = activeTree?.id == tree.id
                val pColor = priorityColor(tree.currentPriority)
                Surface(
                    color = if (isSelected) ForestCardElevated else ForestCardBg,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) pColor else ForestBorderSubtle
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { onSelectTree(tree) }
                        .testTag("map_marker_chip_${tree.treeCode}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(pColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${tree.treeCode} (${tree.currentPriority})",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Selected Tree Marker Telemetry Detail Card
        if (activeTree != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardElevated),
                border = BorderStroke(1.dp, priorityColor(activeTree.currentPriority).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_marker_telemetry_card")
            ) {
                val context = LocalContext.current
                val storedTreeBmp = remember(activeTree.treeCode) {
                    TreeImageStorage.getTreeImage(context, activeTree.treeCode)
                }
                val displayBmp = storedTreeBmp ?: remember(activeTree.treeCode, activeTree.visualCondition) {
                    SampleTreeBitmaps.getTreeBitmap(activeTree.species, activeTree.visualCondition)
                }

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Image(
                                bitmap = displayBmp.asImageBitmap(),
                                contentDescription = "Tree photo for ${activeTree.treeCode}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (storedTreeBmp != null) {
                                Surface(
                                    color = ForestAbyssBg.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(2.dp)
                                ) {
                                    Text(
                                        text = "📸 Photo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeTree.treeCode,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    DataOriginBadge(isDemo = activeTree.registrationType == "DEMO_SEED")
                                }
                                PriorityBadge(
                                    priority = activeTree.currentPriority,
                                    score = activeTree.currentRiskScore
                                )
                            }
                            Text(
                                text = activeTree.species,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${activeTree.zone} (${String.format(Locale.US, "%.4f, %.4f", activeTree.latitude, activeTree.longitude)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Visual Condition", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = activeTree.visualCondition,
                                style = MaterialTheme.typography.labelLarge,
                                color = conditionColor(activeTree.visualCondition)
                            )
                        }
                        Column {
                            Text("Priority Score (R)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "${activeTree.currentRiskScore} / 100",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                        Column {
                            Text("Stress Debt (TD_t)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "${activeTree.stressDebt} pts",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onOpenPassport(activeTree) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = ForestAbyssBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_passport_from_map_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Digital Tree Passport (${activeTree.treeCode})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Google Maps Compose view displaying color-coded markers for each registered tree
 * based on its Explainable Tree Inspection-Priority category (`Critical`, `High`, `Moderate`, `Low`).
 */
@Composable
fun GoogleMapsComposeTreeCanopyView(
    trees: List<TreeRecord>,
    selectedTree: TreeRecord?,
    onSelectTree: (TreeRecord) -> Unit,
    onOpenPassport: (TreeRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val vijayawadaDefault = remember { LatLng(16.5062, 80.6480) }
    val initialTarget = remember(selectedTree) {
        selectedTree?.let { LatLng(it.latitude, it.longitude) } ?: vijayawadaDefault
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialTarget, 13.5f)
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    LaunchedEffect(selectedTree?.id) {
        selectedTree?.let { tree ->
            runCatching {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(tree.latitude, tree.longitude),
                        14.5f
                    ),
                    durationMs = 550
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .testTag("google_map_canopy_view"),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isBuildingEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        ) {
            trees.forEach { tree ->
                val markerPosition = LatLng(tree.latitude, tree.longitude)
                val markerHue = priorityToGoogleMarkerHue(tree.currentPriority)
                val markerState = remember(tree.id, tree.latitude, tree.longitude) {
                    MarkerState(position = markerPosition)
                }

                Marker(
                    state = markerState,
                    title = "${tree.treeCode} • ${tree.currentPriority} (R=${tree.currentRiskScore})",
                    snippet = "${tree.species} • ${tree.visualCondition} (Tap info window for Passport)",
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                    onClick = {
                        onSelectTree(tree)
                        false // Show the Google Maps info window as well
                    },
                    onInfoWindowClick = {
                        onOpenPassport(tree)
                    }
                )
            }
        }

        // Top-right quick controls for Google Map type (Normal / Hybrid) and Re-center Vijayawada
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = ForestAbyssBg.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle Satellite / Hybrid Map Type",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(vijayawadaDefault, 13.5f)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Center on Vijayawada (16.5062, 80.6480)",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveVijayawadaRadarMap(
    trees: List<TreeRecord>,
    selectedTree: TreeRecord?,
    onSelectTree: (TreeRecord) -> Unit
) {
    // Bounding box around Vijayawada (16.485..16.530 N, 80.605..80.670 E)
    val minLat = 16.485
    val maxLat = 16.530
    val minLon = 80.605
    val maxLon = 80.670

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(trees) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    var closest: TreeRecord? = null
                    var bestDist = Float.MAX_VALUE
                    for (tree in trees) {
                        val normX = ((tree.longitude - minLon) / (maxLon - minLon)).coerceIn(0.08, 0.92).toFloat()
                        val normY = (1.0 - ((tree.latitude - minLat) / (maxLat - minLat))).coerceIn(0.10, 0.88).toFloat()
                        val px = normX * w
                        val py = normY * h
                        val d = hypot(tapOffset.x - px, tapOffset.y - py)
                        if (d < bestDist) {
                            bestDist = d
                            closest = tree
                        }
                    }
                    if (closest != null && bestDist <= 120f) {
                        onSelectTree(closest)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. GIS Coordinate Grid
        val gridPaintEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        for (i in 1..5) {
            val gx = w * (i / 6f)
            val gy = h * (i / 6f)
            drawLine(
                color = ForestBorderSubtle.copy(alpha = 0.55f),
                start = Offset(gx, 0f),
                end = Offset(gx, h),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridPaintEffect
            )
            drawLine(
                color = ForestBorderSubtle.copy(alpha = 0.55f),
                start = Offset(0f, gy),
                end = Offset(w, gy),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridPaintEffect
            )
        }

        // 2. Stylized Krishna River Channel across south-west Vijayawada
        val riverPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.35f)
            quadraticTo(w * 0.35f, h * 0.52f, w * 0.65f, h * 0.85f)
            lineTo(w * 0.50f, h)
            quadraticTo(w * 0.25f, h * 0.65f, 0f, h * 0.50f)
            close()
        }
        drawPath(
            path = riverPath,
            color = ElectricCyan.copy(alpha = 0.10f)
        )

        // 3. NH-16 / MG Road Arterial Corridor
        drawLine(
            color = EmeraldPrimary.copy(alpha = 0.22f),
            start = Offset(w * 0.08f, h * 0.42f),
            end = Offset(w * 0.94f, h * 0.58f),
            strokeWidth = 4.dp.toPx()
        )

        // 4. Tree Priority Markers
        trees.forEach { tree ->
            val normX = ((tree.longitude - minLon) / (maxLon - minLon)).coerceIn(0.08, 0.92).toFloat()
            val normY = (1.0 - ((tree.latitude - minLat) / (maxLat - minLat))).coerceIn(0.10, 0.88).toFloat()
            val center = Offset(normX * w, normY * h)
            val mColor = priorityColor(tree.currentPriority)
            val isSelected = selectedTree?.id == tree.id

            if (isSelected) {
                drawCircle(
                    color = mColor.copy(alpha = 0.28f),
                    radius = 28.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = ElectricCyan,
                    radius = 18.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            drawCircle(
                color = ForestAbyssBg,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = mColor,
                radius = 9.dp.toPx(),
                center = center
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OpenStreetMapLeafletView(
    trees: List<TreeRecord>,
    selectedTree: TreeRecord?
) {
    val centerLat = selectedTree?.latitude ?: 16.5062
    val centerLon = selectedTree?.longitude ?: 80.6480

    val markersJs = trees.joinToString("\n") { t ->
        val hex = when (t.currentPriority.lowercase()) {
            "critical" -> "#EF4444"
            "high" -> "#F97316"
            "moderate" -> "#F59E0B"
            else -> "#2EE59D"
        }
        """
        L.circleMarker([${t.latitude}, ${t.longitude}], {
            radius: 10,
            fillColor: "$hex",
            color: "#08110E",
            weight: 2,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(map).bindPopup("<b>${t.treeCode}</b><br/>${t.species}<br/>Priority: ${t.currentPriority} (${t.currentRiskScore})");
        """.trimIndent()
    }

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { height: 100%; margin: 0; background: #08110E; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$centerLat, $centerLon], 13);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap contributors'
                }).addTo(map);
                $markersJs
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
    }
}
