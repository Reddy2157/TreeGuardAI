package com.example.ui

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TreeImageStorage
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestAbyssBg
import com.example.ui.theme.ForestBorderSubtle
import com.example.ui.theme.ForestCardElevated
import com.example.ui.theme.ForestSecondaryBg
import com.example.ui.theme.PriorityCriticalRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.util.Locale

/**
 * Primary navigation destinations for the TreeGuard AI UI Shell:
 * Command Centre, Map, Scan, and Intelligence.
 */
enum class TreeGuardSection(
    val route: String,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val icon: ImageVector
) {
    COMMAND_CENTRE(
        route = "command_centre",
        title = "Command Centre",
        shortLabel = "Command Centre",
        subtitle = "Urban Forest Telemetry • Vijayawada",
        icon = Icons.Default.Dashboard
    ),
    MAP(
        route = "map",
        title = "Map",
        shortLabel = "Map",
        subtitle = "Interactive Canopy GIS & OpenStreetMap",
        icon = Icons.Default.Map
    ),
    SCAN(
        route = "scan",
        title = "Scan",
        shortLabel = "Scan",
        subtitle = "GPS Tree Registration & AI Tree Doctor",
        icon = Icons.Default.DocumentScanner
    ),
    INTELLIGENCE(
        route = "intelligence",
        title = "Intelligence",
        shortLabel = "Intelligence",
        subtitle = "Weather, Stress Debt & Budget Optimizer",
        icon = Icons.Default.Psychology
    )
}

/**
 * Main Jetpack Compose UI Shell with a Material 3 [Scaffold] and [TreeGuardBottomNavigationBar]
 * for switching between Command Centre, Map, Scan, and Intelligence, plus an integrated
 * Digital Tree Passport overlay for inspecting any tree's longitudinal history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeGuardMainScaffold(
    currentUserId: String,
    userEmail: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: TreeGuardViewModel = viewModel(
        key = currentUserId,
        factory = TreeGuardViewModel.factory(currentUserId)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val treeDoctorState by viewModel.treeDoctorState.collectAsStateWithLifecycle()
    val stormMemoryState by viewModel.stormMemoryState.collectAsStateWithLifecycle()
    val groundedSearchState by viewModel.groundedSearchState.collectAsStateWithLifecycle()
    val forestInsightsState by viewModel.forestInsightsState.collectAsStateWithLifecycle()
    val isSeedingCloud by viewModel.isSeedingCloud.collectAsStateWithLifecycle()

    // Pre-seed botanical photo plates to disk if not yet stored
    LaunchedEffect(Unit) {
        TreeImageStorage.seedDefaultDemoPhotosIfEmpty(context)
    }

    // Initialize Android TextToSpeech companion for gemini-3.8-flash-tts audio briefings
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    var currentSectionName by rememberSaveable { mutableStateOf(TreeGuardSection.COMMAND_CENTRE.name) }
    val currentSection = remember(currentSectionName) {
        TreeGuardSection.entries.firstOrNull { it.name == currentSectionName }
            ?: TreeGuardSection.COMMAND_CENTRE
    }

    // Controls whether the Digital Tree Passport registry / detail view is open over the active section
    var isPassportOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var showPassportDetail by rememberSaveable { mutableStateOf(false) }

    val selectedTree = remember(uiState.trees, uiState.selectedTreeId) {
        uiState.trees.firstOrNull { it.id == uiState.selectedTreeId } ?: uiState.trees.firstOrNull()
    }

    val needsInspectionCount = remember(uiState.trees) {
        uiState.trees.count {
            it.visualCondition.equals("Needs inspection", ignoreCase = true) ||
                it.currentPriority.equals("Critical", ignoreCase = true)
        }
    }

    // Handle system back navigation across secondary sections and the Digital Tree Passport overlay
    BackHandler(enabled = isPassportOverlayOpen || currentSection != TreeGuardSection.COMMAND_CENTRE) {
        when {
            isPassportOverlayOpen && showPassportDetail -> {
                showPassportDetail = false
                isPassportOverlayOpen = false
            }
            isPassportOverlayOpen -> {
                isPassportOverlayOpen = false
            }
            currentSection != TreeGuardSection.COMMAND_CENTRE -> {
                currentSectionName = TreeGuardSection.COMMAND_CENTRE.name
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("treeguard_main_scaffold"),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ForestAbyssBg,
            topBar = {
                TreeGuardTopAppBar(
                    currentSection = currentSection,
                    isPassportOverlayOpen = isPassportOverlayOpen,
                    treeCount = uiState.trees.size,
                    userEmail = userEmail,
                    temperatureC = uiState.weather?.temperature,
                    onTogglePassportOverlay = {
                        if (isPassportOverlayOpen) {
                            isPassportOverlayOpen = false
                            showPassportDetail = false
                        } else {
                            isPassportOverlayOpen = true
                            showPassportDetail = false
                        }
                    },
                    onSignOut = {
                        signOutTreeGuard(
                            context = context,
                            onSignOutComplete = {},
                            scope = scope
                        )
                    }
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    TreeGuardBottomNavigationBar(
                        currentSection = currentSection,
                        isPassportOverlayOpen = isPassportOverlayOpen,
                        treeCount = uiState.trees.size,
                        needsInspectionCount = needsInspectionCount,
                        onSelectSection = { section ->
                            currentSectionName = section.name
                            isPassportOverlayOpen = false
                            showPassportDetail = false
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    TreeGuardNavigationRail(
                        currentSection = currentSection,
                        isPassportOverlayOpen = isPassportOverlayOpen,
                        treeCount = uiState.trees.size,
                        needsInspectionCount = needsInspectionCount,
                        onSelectSection = { section ->
                            currentSectionName = section.name
                            isPassportOverlayOpen = false
                            showPassportDetail = false
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                        Surface(
                            color = ForestCardElevated,
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("status_banner_surface")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = uiState.statusBannerMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isPassportOverlayOpen) {
                            TreeRecordsAndPassportScreen(
                                trees = uiState.trees,
                                observations = uiState.observations,
                                stressRecords = uiState.stressRecords,
                                maintenanceActions = uiState.maintenanceActions,
                                stormComparisons = uiState.stormComparisons,
                                priorityHistory = uiState.priorityHistory,
                                weather = uiState.weather,
                                selectedTree = selectedTree,
                                showPassportDetail = showPassportDetail,
                                stormMemoryState = stormMemoryState,
                                onSelectTreeForPassport = { tree ->
                                    viewModel.selectTree(tree.id)
                                    showPassportDetail = true
                                },
                                onClosePassportDetail = {
                                    if (showPassportDetail) {
                                        showPassportDetail = false
                                    } else {
                                        isPassportOverlayOpen = false
                                    }
                                },
                                onRunStormMemoryComparison = { tree, earlier, later, eLabel, lLabel ->
                                    viewModel.runStormMemoryComparison(tree, earlier, later, eLabel, lLabel)
                                },
                                onUpdateRiskFactors = { tree, s, e, d, w, t ->
                                    viewModel.updateTreeRiskFactors(tree, s, e, d, w, t)
                                },
                                onSyncWeatherToTree = { tree ->
                                    viewModel.applyWeatherToEngines(tree)
                                },
                                onAddManualObservation = { tree, cond, symp, act, reqArb, concerns, evidence, sev ->
                                    viewModel.addManualObservationNote(
                                        tree = tree,
                                        visualCondition = cond,
                                        symptoms = symp,
                                        recommendedAction = act,
                                        requireArborist = reqArb,
                                        concerns = concerns,
                                        supportingEvidence = evidence,
                                        observationSeverity = sev
                                    )
                                },
                                onProvidePresetBitmap = { preset -> viewModel.getPresetBitmap(preset) },
                                onSpeakText = { text -> viewModel.speakTextWithGeminiTts(text, ttsEngine) },
                                onAddMaintenanceAction = { tree, act, cost, ben, status ->
                                    viewModel.addMaintenanceRecord(tree, act, cost, ben, status)
                                }
                            )
                        } else {
                            Crossfade(
                                targetState = currentSection,
                                label = "treeguard_section_crossfade"
                            ) { section ->
                                when (section) {
                                    TreeGuardSection.COMMAND_CENTRE -> CommandCentreScreen(
                                        uiState = uiState,
                                        isSeedingCloud = isSeedingCloud,
                                        onSeedCloud = { viewModel.seedDemoDatasetToFirestore() },
                                        onRefreshWeather = { viewModel.refreshRegionalWeather(forceRefresh = true) },
                                        onOpenTreePassport = { tree ->
                                            viewModel.selectTree(tree.id)
                                            showPassportDetail = true
                                            isPassportOverlayOpen = true
                                        },
                                        onNavigateToMap = {
                                            isPassportOverlayOpen = false
                                            currentSectionName = TreeGuardSection.MAP.name
                                        },
                                        onNavigateToScan = {
                                            isPassportOverlayOpen = false
                                            currentSectionName = TreeGuardSection.SCAN.name
                                        },
                                        onNavigateToIntelligence = {
                                            isPassportOverlayOpen = false
                                            currentSectionName = TreeGuardSection.INTELLIGENCE.name
                                        },
                                        onSpeakSummary = { text -> viewModel.speakTextWithGeminiTts(text, ttsEngine) }
                                    )

                                    TreeGuardSection.MAP -> TreeMapScreen(
                                        trees = uiState.trees,
                                        selectedTree = selectedTree,
                                        onSelectTree = { viewModel.selectTree(it.id) },
                                        onOpenPassport = { tree ->
                                            viewModel.selectTree(tree.id)
                                            showPassportDetail = true
                                            isPassportOverlayOpen = true
                                        }
                                    )

                                    TreeGuardSection.SCAN -> ScanTreeScreen(
                                        trees = uiState.trees,
                                        selectedTree = selectedTree,
                                        treeDoctorState = treeDoctorState,
                                        onSelectTargetTree = { viewModel.selectTree(it.id) },
                                        onRegisterNewTree = { species, lat, lon, zone, regType, notes, onCreated ->
                                            viewModel.registerNewTree(
                                                species = species,
                                                latitude = lat,
                                                longitude = lon,
                                                zone = zone,
                                                registrationType = regType,
                                                notes = notes,
                                                onRegistered = onCreated
                                            )
                                        },
                                        onRunAiTreeDoctor = { bmp, tree, species, zone, notes, tag ->
                                            viewModel.runAiTreeDoctor(bmp, tree, species, zone, notes, tag)
                                        },
                                        onProvidePresetBitmap = { preset -> viewModel.getPresetBitmap(preset) },
                                        onSpeakReport = { text -> viewModel.speakTextWithGeminiTts(text, ttsEngine) },
                                        onAddMaintenanceAction = { tree, act, cost, ben, status ->
                                            viewModel.addMaintenanceRecord(tree, act, cost, ben, status)
                                        }
                                    )

                                    TreeGuardSection.INTELLIGENCE -> IntelligenceScreen(
                                        uiState = uiState,
                                        forestInsightsState = forestInsightsState,
                                        groundedSearchState = groundedSearchState,
                                        onFetchForestInsights = { viewModel.fetchUrbanForestAiInsights() },
                                        onSetBudgetScenario = { viewModel.setBudgetScenario(it) },
                                        onToggleMaintenanceStatus = { viewModel.toggleMaintenanceStatus(it) },
                                        onAddMaintenanceAction = { tree, act, cost, ben ->
                                            viewModel.addProposedMaintenanceAction(tree, act, cost, ben)
                                        },
                                        onRecordStressSnapshot = { tree, calc ->
                                            viewModel.recordStressCalculation(tree, calc)
                                        },
                                        onFetchWeatherForCoordinates = { lat, lon, label, forceRefresh ->
                                            viewModel.refreshRegionalWeather(lat, lon, label, forceRefresh)
                                        },
                                        onPropagateWeatherToEngines = { targetTree ->
                                            viewModel.applyWeatherToEngines(targetTree)
                                        },
                                        onRunGroundedSearch = { query -> viewModel.runSearchGroundedQuery(query) },
                                        onSpeakText = { text -> viewModel.speakTextWithGeminiTts(text, ttsEngine) }
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

/**
 * TopAppBar for the TreeGuard AI UI Shell displaying the active section subtitle,
 * quick access to the Digital Tree Passports registry, live regional temperature pill, and Sign Out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeGuardTopAppBar(
    currentSection: TreeGuardSection,
    isPassportOverlayOpen: Boolean,
    treeCount: Int,
    userEmail: String,
    temperatureC: Double?,
    onTogglePassportOverlay: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("treeguard_top_app_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ForestSecondaryBg,
            titleContentColor = TextPrimary
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Forest,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TREEGUARD AI",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (temperatureC != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ForestCardElevated,
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f°C", temperatureC),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isPassportOverlayOpen) {
                            "Digital Tree Passports • $userEmail"
                        } else {
                            "${currentSection.title} • ${currentSection.subtitle}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            OutlinedButton(
                onClick = onTogglePassportOverlay,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                border = BorderStroke(
                    1.dp,
                    if (isPassportOverlayOpen) EmeraldPrimary else ForestBorderSubtle
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isPassportOverlayOpen) {
                        EmeraldPrimary.copy(alpha = 0.18f)
                    } else {
                        ForestCardElevated
                    }
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("top_bar_passports_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = "Digital Tree Passports",
                    tint = if (isPassportOverlayOpen) EmeraldPrimary else ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Passports ($treeCount)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPassportOverlayOpen) EmeraldPrimary else TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = TextSecondary
                )
            }
        }
    )
}

/**
 * BottomNavigationBar for the TreeGuard AI sections:
 * Command Centre, Map, Scan, and Intelligence.
 */
@Composable
fun TreeGuardBottomNavigationBar(
    currentSection: TreeGuardSection,
    isPassportOverlayOpen: Boolean,
    treeCount: Int,
    needsInspectionCount: Int,
    onSelectSection: (TreeGuardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = ForestSecondaryBg,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("treeguard_bottom_navigation_bar")
    ) {
        TreeGuardSection.entries.forEachIndexed { idx, section ->
            val isSelected = !isPassportOverlayOpen && currentSection == section
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectSection(section) },
                icon = {
                    BadgedBox(
                        badge = {
                            when (section) {
                                TreeGuardSection.COMMAND_CENTRE -> {
                                    if (needsInspectionCount > 0) {
                                        Badge(
                                            containerColor = PriorityCriticalRed,
                                            contentColor = TextPrimary
                                        ) {
                                            Text(needsInspectionCount.toString())
                                        }
                                    }
                                }
                                TreeGuardSection.MAP -> {
                                    if (treeCount > 0) {
                                        Badge(
                                            containerColor = ElectricCyan,
                                            contentColor = ForestAbyssBg
                                        ) {
                                            Text(treeCount.toString())
                                        }
                                    }
                                }
                                TreeGuardSection.SCAN -> {
                                    Badge(
                                        containerColor = EmeraldPrimary,
                                        contentColor = ForestAbyssBg
                                    ) {
                                        Text("AI")
                                    }
                                }
                                TreeGuardSection.INTELLIGENCE -> {
                                    Badge(
                                        containerColor = WarningAmber,
                                        contentColor = ForestAbyssBg
                                    ) {
                                        Text("R")
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.title
                        )
                    }
                },
                label = {
                    Text(
                        text = section.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ForestAbyssBg,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier
                    .testTag("bottom_nav_${section.route}")
            )
        }
    }
}

/**
 * Adaptive NavigationRail for Expanded / Tablet screens with the same four sections:
 * Command Centre, Map, Scan, and Intelligence.
 */
@Composable
fun TreeGuardNavigationRail(
    currentSection: TreeGuardSection,
    isPassportOverlayOpen: Boolean,
    treeCount: Int,
    needsInspectionCount: Int,
    onSelectSection: (TreeGuardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = ForestSecondaryBg,
        contentColor = TextPrimary,
        modifier = modifier
            .fillMaxHeight()
            .background(ForestSecondaryBg)
            .testTag("treeguard_navigation_rail")
    ) {
        Spacer(modifier = Modifier.weight(1f))
        TreeGuardSection.entries.forEach { section ->
            val isSelected = !isPassportOverlayOpen && currentSection == section
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelectSection(section) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (section == TreeGuardSection.COMMAND_CENTRE && needsInspectionCount > 0) {
                                Badge(containerColor = PriorityCriticalRed, contentColor = TextPrimary) {
                                    Text(needsInspectionCount.toString())
                                }
                            } else if (section == TreeGuardSection.MAP && treeCount > 0) {
                                Badge(containerColor = ElectricCyan, contentColor = ForestAbyssBg) {
                                    Text(treeCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = section.icon, contentDescription = section.title)
                    }
                },
                label = {
                    Text(
                        text = section.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = ForestAbyssBg,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("rail_nav_${section.route}")
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Standalone Jetpack Compose Scaffold + BottomNavigationBar shell with structured
 * dark-theme placeholders for the four TreeGuard AI tabs:
 * Command Centre, Map, Scan, and Intelligence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeGuardScaffoldWithPlaceholders(
    initialSection: TreeGuardSection = TreeGuardSection.COMMAND_CENTRE,
    modifier: Modifier = Modifier
) {
    var currentSection by rememberSaveable { mutableStateOf(initialSection) }

    BackHandler(enabled = currentSection != TreeGuardSection.COMMAND_CENTRE) {
        currentSection = TreeGuardSection.COMMAND_CENTRE
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("treeguard_placeholder_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ForestAbyssBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForestSecondaryBg,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Column {
                        Text(
                            text = "TREEGUARD AI • ${currentSection.title.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentSection.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            )
        },
        bottomBar = {
            TreeGuardBottomNavigationBar(
                currentSection = currentSection,
                isPassportOverlayOpen = false,
                treeCount = 4,
                needsInspectionCount = 1,
                onSelectSection = { selected -> currentSection = selected }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ForestAbyssBg)
                .padding(innerPadding)
        ) {
            when (currentSection) {
                TreeGuardSection.COMMAND_CENTRE -> CommandCentreTabPlaceholder(
                    onNavigateToSection = { currentSection = it }
                )
                TreeGuardSection.MAP -> MapTabPlaceholder(
                    onNavigateToSection = { currentSection = it }
                )
                TreeGuardSection.SCAN -> ScanTabPlaceholder(
                    onNavigateToSection = { currentSection = it }
                )
                TreeGuardSection.INTELLIGENCE -> IntelligenceTabPlaceholder(
                    onNavigateToSection = { currentSection = it }
                )
            }
        }
    }
}

@Composable
fun CommandCentreTabPlaceholder(
    onNavigateToSection: (TreeGuardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    TreeGuardSectionPlaceholderCard(
        section = TreeGuardSection.COMMAND_CENTRE,
        headline = "Command Centre Dashboard",
        description = "Displays Total Trees, High-Priority Trees (R ≥ 50), Arborist Escalation Alerts, and the Longitudinal Stress Debt (TD_t) telemetry curve for Vijayawada.",
        quickActionLabel = "Open Canopy Map",
        quickActionTarget = TreeGuardSection.MAP,
        onNavigateToSection = onNavigateToSection,
        modifier = modifier.testTag("placeholder_tab_command_centre")
    )
}

@Composable
fun MapTabPlaceholder(
    onNavigateToSection: (TreeGuardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    TreeGuardSectionPlaceholderCard(
        section = TreeGuardSection.MAP,
        headline = "Interactive Canopy GIS & OpenStreetMap",
        description = "Visualizes registered tree coordinates in Vijayawada (16.5062, 80.6480) with priority-coded markers (Low, Moderate, High, Critical) and Digital Tree Passport links.",
        quickActionLabel = "Scan & Register Tree",
        quickActionTarget = TreeGuardSection.SCAN,
        onNavigateToSection = onNavigateToSection,
        modifier = modifier.testTag("placeholder_tab_map")
    )
}

@Composable
fun ScanTabPlaceholder(
    onNavigateToSection: (TreeGuardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    TreeGuardSectionPlaceholderCard(
        section = TreeGuardSection.SCAN,
        headline = "GPS Tree Registration & AI Tree Doctor",
        description = "Captures real device GPS coordinates and street-level tree photographs for Gemini 3.8 Flash visual screening, separating observable symptoms from non-diagnostic concerns.",
        quickActionLabel = "Open Intelligence Engine",
        quickActionTarget = TreeGuardSection.INTELLIGENCE,
        onNavigateToSection = onNavigateToSection,
        modifier = modifier.testTag("placeholder_tab_scan")
    )
}

@Composable
fun IntelligenceTabPlaceholder(
    onNavigateToSection: (TreeGuardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    TreeGuardSectionPlaceholderCard(
        section = TreeGuardSection.INTELLIGENCE,
        headline = "Weather, Stress Debt & Maintenance Optimizer",
        description = "Integrates Open-Meteo 7-day forecasts, Tree Stress Debt (TD_t = λTD_(t-1) + S_t), Explainable Priority (R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T), and What-If Budget Optimization.",
        quickActionLabel = "Return to Command Centre",
        quickActionTarget = TreeGuardSection.COMMAND_CENTRE,
        onNavigateToSection = onNavigateToSection,
        modifier = modifier.testTag("placeholder_tab_intelligence")
    )
}

@Composable
private fun TreeGuardSectionPlaceholderCard(
    section: TreeGuardSection,
    headline: String,
    description: String,
    quickActionLabel: String,
    quickActionTarget: TreeGuardSection,
    onNavigateToSection: (TreeGuardSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = ForestCardElevated,
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.title,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = section.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedButton(
                    onClick = { onNavigateToSection(quickActionTarget) },
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ForestSecondaryBg,
                        contentColor = EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("placeholder_action_${section.route}")
                ) {
                    Text(
                        text = quickActionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

