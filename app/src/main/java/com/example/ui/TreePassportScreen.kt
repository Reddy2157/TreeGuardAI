package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.example.data.StoredTreeImage
import com.example.data.TreeImageStorage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MaintenanceAction
import com.example.data.ObservationRecord
import com.example.data.PriorityHistoryEntry
import com.example.data.RegionalWeatherSnapshot
import com.example.data.SampleTreePreset
import com.example.data.StormComparison
import com.example.data.StormMemoryComparisonResult
import com.example.data.StressRecord
import com.example.data.TreeGuardEngines
import com.example.data.TreeRecord
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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private data class ChronologicalPassportItem(
    val sortOrder: Int,
    val dateLabel: String,
    val badgeLabel: String,
    val badgeColor: Color,
    val title: String,
    val body: String,
    val footer: String
)

@Composable
fun TreeRecordsAndPassportScreen(
    trees: List<TreeRecord>,
    observations: List<ObservationRecord>,
    stressRecords: List<StressRecord>,
    maintenanceActions: List<MaintenanceAction>,
    stormComparisons: List<StormComparison>,
    priorityHistory: List<PriorityHistoryEntry>,
    weather: RegionalWeatherSnapshot?,
    selectedTree: TreeRecord?,
    showPassportDetail: Boolean,
    stormMemoryState: AsyncTaskState<StormMemoryComparisonResult>,
    onSelectTreeForPassport: (TreeRecord) -> Unit,
    onClosePassportDetail: () -> Unit,
    onRunStormMemoryComparison: (
        tree: TreeRecord,
        earlierBmp: Bitmap,
        laterBmp: Bitmap,
        earlierLabel: String,
        laterLabel: String
    ) -> Unit,
    onUpdateRiskFactors: (
        tree: TreeRecord,
        s: Double,
        e: Double,
        d: Double,
        w: Double,
        t: Double
    ) -> Unit,
    onSyncWeatherToTree: (TreeRecord) -> Unit,
    onAddManualObservation: (
        tree: TreeRecord,
        condition: String,
        symptoms: String,
        action: String,
        requireArborist: Boolean,
        concerns: String,
        evidence: String,
        severityScore: Double
    ) -> Unit,
    onProvidePresetBitmap: (SampleTreePreset) -> Bitmap,
    onSpeakText: (String) -> Unit,
    onAddMaintenanceAction: ((TreeRecord, String, Double, Double, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    if (showPassportDetail && selectedTree != null) {
        BackHandler { onClosePassportDetail() }
        DigitalTreePassportDetailView(
            tree = selectedTree,
            treeObservations = observations.filter { it.treeId == selectedTree.id || it.treeCode == selectedTree.treeCode },
            treeStressRecords = stressRecords.filter { it.treeId == selectedTree.id },
            treeMaintActions = maintenanceActions.filter { it.treeId == selectedTree.id || it.treeCode == selectedTree.treeCode },
            treeStormComparisons = stormComparisons.filter { it.treeId == selectedTree.id || it.treeCode == selectedTree.treeCode },
            treePriorityHistory = priorityHistory.filter { it.treeId == selectedTree.id || it.treeCode == selectedTree.treeCode },
            weather = weather,
            stormMemoryState = stormMemoryState,
            onBack = onClosePassportDetail,
            onRunStormMemory = onRunStormMemoryComparison,
            onUpdateRiskFactors = onUpdateRiskFactors,
            onSyncWeatherToTree = onSyncWeatherToTree,
            onAddManualObservation = onAddManualObservation,
            onProvidePresetBitmap = onProvidePresetBitmap,
            onSpeakText = onSpeakText,
            onAddMaintenanceAction = onAddMaintenanceAction,
            modifier = modifier
        )
    } else {
        val filtered = remember(trees, searchQuery) {
            if (searchQuery.isBlank()) trees
            else trees.filter {
                it.treeCode.contains(searchQuery, true) ||
                    it.species.contains(searchQuery, true) ||
                    it.zone.contains(searchQuery, true)
            }
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(ForestAbyssBg),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "DIGITAL TREE PASSPORT REGISTRY",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Permanent longitudinal record per tree: ID, species, GPS, original photo, AI screening reports, Stress Debt, Explainable Priority (R), and maintenance history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan) },
                    placeholder = { Text("Search by ID (TG-VJA-0001), species, or ward...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tree_records_search_input")
                )
            }

            items(filtered, key = { it.id.ifBlank { it.treeCode } }) { tree ->
                val regDate = remember(tree.createdAt) {
                    tree.createdAt?.toDate()?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale.US).format(it)
                    } ?: "14 May 2026"
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, priorityColor(tree.currentPriority).copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTreeForPassport(tree) }
                        .testTag("tree_record_card_${tree.treeCode}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tree.treeCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                DataOriginBadge(isDemo = tree.registrationType == "DEMO_SEED")
                            }
                            PriorityBadge(priority = tree.currentPriority, score = tree.currentRiskScore)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tree.species,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "${tree.zone} • (${String.format(Locale.US, "%.4f", tree.latitude)}, ${String.format(Locale.US, "%.4f", tree.longitude)}) • Reg: $regDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Condition: ${tree.visualCondition}",
                                style = MaterialTheme.typography.labelMedium,
                                color = conditionColor(tree.visualCondition)
                            )
                            Text(
                                text = "Stress Debt: ${tree.stressDebt} pts",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalTreePassportDetailView(
    tree: TreeRecord,
    treeObservations: List<ObservationRecord>,
    treeStressRecords: List<StressRecord>,
    treeMaintActions: List<MaintenanceAction>,
    treeStormComparisons: List<StormComparison>,
    treePriorityHistory: List<PriorityHistoryEntry>,
    weather: RegionalWeatherSnapshot?,
    stormMemoryState: AsyncTaskState<StormMemoryComparisonResult>,
    onBack: () -> Unit,
    onRunStormMemory: (TreeRecord, Bitmap, Bitmap, String, String) -> Unit,
    onUpdateRiskFactors: (TreeRecord, Double, Double, Double, Double, Double) -> Unit,
    onSyncWeatherToTree: (TreeRecord) -> Unit,
    onAddManualObservation: (TreeRecord, String, String, String, Boolean, String, String, Double) -> Unit,
    onProvidePresetBitmap: (SampleTreePreset) -> Bitmap,
    onSpeakText: (String) -> Unit,
    onAddMaintenanceAction: ((TreeRecord, String, Double, Double, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showQrDialog by remember { mutableStateOf(false) }
    var showAddObsDialog by remember { mutableStateOf(false) }
    var showAddMaintDialog by remember { mutableStateOf(false) }
    var showFactorEditor by remember { mutableStateOf(false) }
    var viewMode by remember { mutableIntStateOf(0) } // 0 = Structured Passport, 1 = Unified Chronological Timeline

    val registrationDateStr = remember(tree.createdAt, tree.treeCode) {
        tree.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(it)
        } ?: "14 May 2026"
    }

    val priorityBreakdown = remember(tree, weather) {
        TreeGuardEngines.evaluateInspectionPriority(
            structuralScore = tree.structuralScore.takeIf { it >= 0.0 },
            exposureScore = tree.exposureScore.takeIf { it >= 0.0 },
            declineScore = tree.declineScore.takeIf { it >= 0.0 },
            weatherScore = tree.weatherScore.takeIf { it >= 0.0 },
            historyScore = tree.historyScore.takeIf { it >= 0.0 },
            visualCondition = tree.visualCondition,
            soilMoistureAvailable = weather?.soilMoisture != null
        )
    }

    val context = LocalContext.current
    val storedImage = remember(tree.treeCode) { TreeImageStorage.getTreeImage(context, tree.treeCode) }
    val storedMeta = remember(tree.treeCode) { TreeImageStorage.getStoredImageMetadata(context, tree.treeCode) }
    val allStoredPhotos = remember(tree.treeCode) { TreeImageStorage.getAllPhotosForTree(context, tree.treeCode) }
    var activePreviewPhoto by remember(storedImage) { mutableStateOf(allStoredPhotos.firstOrNull()) }

    val earlierBmp = remember { onProvidePresetBitmap(SampleTreePreset.BASELINE_HEALTHY_NEEM) }
    val currentBmp = remember(tree.treeCode, tree.visualCondition, activePreviewPhoto, storedImage) {
        activePreviewPhoto?.bitmap ?: storedImage ?: when {
            tree.visualCondition == "Needs inspection" -> onProvidePresetBitmap(SampleTreePreset.POST_STORM_DAMAGED_NEEM)
            tree.species.contains("Banyan", true) || tree.visualCondition == "Monitor" -> onProvidePresetBitmap(SampleTreePreset.BARK_FISSURE_BANYAN)
            else -> onProvidePresetBitmap(SampleTreePreset.BASELINE_HEALTHY_NEEM)
        }
    }

    var stormEarlierBmp by remember(tree.id) { mutableStateOf(earlierBmp) }
    var stormNewerBmp by remember(tree.id, currentBmp) { mutableStateOf(currentBmp) }
    var stormEarlierLabel by remember(tree.id, registrationDateStr) {
        mutableStateOf("Pre-Monsoon Baseline ($registrationDateStr)")
    }
    var stormNewerLabel by remember(tree.id) {
        mutableStateOf("Post-Cyclone Inspection (Oct 2026)")
    }

    val pickEarlierLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { decoded ->
                        stormEarlierBmp = decoded
                        stormEarlierLabel = "Uploaded Earlier Photo"
                    }
                }
            }
        }
    }

    val pickNewerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { decoded ->
                        stormNewerBmp = decoded
                        stormNewerLabel = "Uploaded Newer Photo"
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = ForestCardBg,
            title = {
                Text("Digital Tree Passport QR Identity", color = TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TreePassportQrMatrix(treeCode = tree.treeCode)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tree.treeCode,
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricCyan
                    )
                    Text(
                        text = "${tree.species}\n${tree.zone}\nRegistered: $registrationDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Close", color = EmeraldPrimary)
                }
            }
        )
    }

    if (showAddObsDialog) {
        var obsCondition by remember { mutableStateOf("Monitor") }
        var obsSymptoms by remember { mutableStateOf("") }
        var obsConcerns by remember { mutableStateOf("") }
        var obsEvidence by remember { mutableStateOf("") }
        var obsAction by remember { mutableStateOf("") }
        var obsSeverity by remember { mutableDoubleStateOf(tree.declineScore.coerceIn(0.0, 100.0)) }
        var requireArborist by remember { mutableStateOf(false) }

        val conditionChoices = listOf("Healthy appearance", "Monitor", "Needs inspection", "Unclear")

        AlertDialog(
            onDismissRequest = { showAddObsDialog = false },
            containerColor = ForestCardBg,
            title = { Text("Add Field Observation (${tree.treeCode})", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Appends a dated observation to the permanent Digital Tree Passport and recalculates Stress Debt & Priority R.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text("Visual Condition:", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(conditionChoices) { choice ->
                            FilterChip(
                                selected = obsCondition == choice,
                                onClick = {
                                    obsCondition = choice
                                    if (choice == "Needs inspection") requireArborist = true
                                },
                                label = { Text(choice, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = obsSymptoms,
                        onValueChange = { obsSymptoms = it },
                        label = { Text("Observable Visual Symptoms") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = obsConcerns,
                        onValueChange = { obsConcerns = it },
                        label = { Text("Possible Non-Diagnostic Concerns") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = obsEvidence,
                        onValueChange = { obsEvidence = it },
                        label = { Text("Supporting Visual Evidence") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = obsAction,
                        onValueChange = { obsAction = it },
                        label = { Text("Recommended Follow-up Action") },
                        singleLine = true
                    )
                    Text(
                        text = "Decline / Observation Severity (D / O_t): ${obsSeverity.roundToInt()}/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                    Slider(
                        value = obsSeverity.toFloat(),
                        onValueChange = { obsSeverity = it.toDouble() },
                        valueRange = 0f..100f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = requireArborist, onCheckedChange = { requireArborist = it })
                        Text(
                            text = "Escalate for Certified Arborist Inspection",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddManualObservation(
                            tree,
                            obsCondition,
                            obsSymptoms,
                            obsAction,
                            requireArborist,
                            obsConcerns,
                            obsEvidence,
                            obsSeverity
                        )
                        showAddObsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ForestAbyssBg),
                    modifier = Modifier.testTag("save_observation_confirm_button")
                ) {
                    Text("Save Observation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddObsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showAddMaintDialog) {
        var actionInput by remember { mutableStateOf("Deadwood removal & crown weight reduction") }
        var costInput by remember { mutableStateOf("3500") }
        var benefitInput by remember { mutableDoubleStateOf(70.0) }
        var statusChoice by remember { mutableStateOf("PROPOSED") }

        val commonTemplates = listOf(
            "Deadwood removal & crown reduction",
            "Deep root fungal soil drench",
            "Cabling installation for split limb",
            "Mulching & root aeration treatment",
            "Canopy clearance near utility line"
        )

        AlertDialog(
            onDismissRequest = { showAddMaintDialog = false },
            containerColor = ForestCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Engineering, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Arborist Intervention (${tree.treeCode})", color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Logs maintenance intervention into permanent Firestore registry, tracking cost, projected health benefit, and intervention status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text("Quick Intervention Templates:", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(commonTemplates) { tpl ->
                            FilterChip(
                                selected = actionInput == tpl,
                                onClick = { actionInput = tpl },
                                label = { Text(tpl.take(28) + if (tpl.length > 28) "..." else "", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = actionInput,
                        onValueChange = { actionInput = it },
                        label = { Text("Intervention Action Description") },
                        modifier = Modifier.fillMaxWidth().testTag("maint_dialog_action_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Est. Cost (₹)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("maint_dialog_cost_input")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Benefit: ${benefitInput.roundToInt()}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                            Slider(
                                value = benefitInput.toFloat(),
                                onValueChange = { benefitInput = it.toDouble() },
                                valueRange = 10f..100f,
                                modifier = Modifier.testTag("maint_dialog_benefit_slider")
                            )
                        }
                    }
                    Text("Intervention Status:", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PROPOSED", "SCHEDULED", "COMPLETED").forEach { st ->
                            FilterChip(
                                selected = statusChoice == st,
                                onClick = { statusChoice = st },
                                label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = costInput.toDoubleOrNull() ?: 2500.0
                        onAddMaintenanceAction?.invoke(tree, actionInput, cost, benefitInput, statusChoice)
                        showAddMaintDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ForestAbyssBg),
                    modifier = Modifier.testTag("confirm_save_maintenance_button")
                ) {
                    Text("Save to Firestore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMaintDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ForestAbyssBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Permanent Digital Tree Passport Identity Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardElevated),
                border = BorderStroke(1.5.dp, priorityColor(tree.currentPriority)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("passport_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Tree Registry",
                                    tint = ElectricCyan
                                )
                            }
                            Column {
                                Text(
                                    text = "PERMANENT DIGITAL TREE PASSPORT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = tree.treeCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    onSpeakText(
                                        "Digital Tree Passport ${tree.treeCode}, species ${tree.species}, registered on $registrationDateStr at ${tree.zone}. " +
                                            "Current condition is ${tree.visualCondition}. " +
                                            "Inspection priority is ${tree.currentPriority} with score ${priorityBreakdown.finalPriorityScore} out of 100. " +
                                            "Accumulated Stress Debt is ${tree.stressDebt} points. " +
                                            "${priorityBreakdown.recommendedUrgency}"
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Listen to Passport Summary (TTS)",
                                    tint = ElectricCyan
                                )
                            }
                            IconButton(onClick = { showQrDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Show Tree Passport QR Code",
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Permanent Metadata Grid: Species, Location, Registration Date, Current Condition
                    Surface(
                        color = ForestAbyssBg,
                        border = BorderStroke(1.dp, ForestBorderSubtle),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SPECIES", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(tree.species, style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
                                }
                                PriorityBadge(priority = priorityBreakdown.category, score = priorityBreakdown.finalPriorityScore)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LOCATION & GPS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        text = "${tree.zone}\n(${String.format(Locale.US, "%.4f, %.4f", tree.latitude, tree.longitude)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("REGISTRATION DATE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(registrationDateStr, style = MaterialTheme.typography.bodySmall, color = ElectricCyan)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("CURRENT CONDITION", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        text = tree.visualCondition,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = conditionColor(tree.visualCondition),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DataOriginBadge(isDemo = tree.registrationType == "DEMO_SEED")
                        Button(
                            onClick = { showAddObsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ForestAbyssBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_observation_button")
                        ) {
                            Icon(imageVector = Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add New Observation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tree.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    // View Mode Switcher: Structured Sections vs Unified Chronological River
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = viewMode == 0,
                            onClick = { viewMode = 0 },
                            label = { Text("Structured Passport") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldPrimary
                            ),
                            modifier = Modifier.testTag("passport_mode_structured")
                        )
                        FilterChip(
                            selected = viewMode == 1,
                            onClick = { viewMode = 1 },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Chronological Master Timeline") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("passport_mode_chronological")
                        )
                    }
                }
            }
        }

        if (viewMode == 1) {
            // UNIFIED CHRONOLOGICAL TIMELINE MODE
            item {
                UnifiedChronologicalPassportTimeline(
                    tree = tree,
                    registrationDateStr = registrationDateStr,
                    originalBitmap = earlierBmp,
                    observations = treeObservations,
                    stressRecords = treeStressRecords,
                    priorityHistory = treePriorityHistory,
                    stormComparisons = treeStormComparisons,
                    maintActions = treeMaintActions
                )
            }
        } else {
            // 2. Original Registration Photograph & Current Visual State Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ORIGINAL REGISTRATION PHOTOGRAPH & CURRENT VISUAL ARCHIVE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "Permanent baseline photograph registered on $registrationDateStr vs latest inspection frame",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ORIGINAL BASELINE ($registrationDateStr)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = earlierBmp.asImageBitmap(),
                                    contentDescription = "Original registration photograph for ${tree.treeCode}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(135.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Baseline: Healthy crown architecture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val isRealStored = activePreviewPhoto != null || storedImage != null
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isRealStored) "📸 STORED PHOTO" else "LATEST FRAME",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isRealStored) ElectricCyan else conditionColor(tree.visualCondition),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = currentBmp.asImageBitmap(),
                                    contentDescription = "Current visual condition photograph for ${tree.treeCode}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(135.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val displayLat = activePreviewPhoto?.latitude ?: storedMeta?.latitude ?: tree.latitude
                                val displayLon = activePreviewPhoto?.longitude ?: storedMeta?.longitude ?: tree.longitude
                                Text(
                                    text = "GPS: ${String.format(Locale.US, "%.4f, %.4f", displayLat, displayLon)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricCyan
                                )
                            }
                        }

                        if (allStoredPhotos.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "SAVED BOTANICAL PHOTO ARCHIVE (${allStoredPhotos.size} files on disk)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("tree_stored_photos_strip")
                            ) {
                                items(allStoredPhotos, key = { it.id }) { photo ->
                                    val isSelected = activePreviewPhoto?.id == photo.id
                                    Surface(
                                        color = if (isSelected) ElectricCyan.copy(alpha = 0.2f) else ForestAbyssBg,
                                        border = BorderStroke(1.dp, if (isSelected) ElectricCyan else ForestBorderSubtle),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .clickable { activePreviewPhoto = photo }
                                            .testTag("select_photo_${photo.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                bitmap = (photo.bitmap ?: earlierBmp).asImageBitmap(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(photo.dateLabel, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                                Text(photo.resolution, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. FEATURE 5: Explainable Tree Inspection-Priority Engine Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
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
                                    text = "EXPLAINABLE INSPECTION-PRIORITY ENGINE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T = ${priorityBreakdown.finalPriorityScore} / 100 (${priorityBreakdown.category})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { showFactorEditor = !showFactorEditor },
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("toggle_factor_editor_button")
                            ) {
                                Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showFactorEditor) "Hide" else "Tune Factors", color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (priorityBreakdown.humanEscalationRequired) {
                            Surface(
                                color = PriorityCriticalRed.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, PriorityCriticalRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = PriorityCriticalRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = priorityBreakdown.escalationReason ?: "Escalated for certified arborist review.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PriorityCriticalRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Individual Normalized Factor Scores (0-100) & Weighted Contributions
                        priorityBreakdown.factors.forEach { f ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${f.label} (${f.code} • Base ${(f.baseWeight * 100).roundToInt()}% / Eff ${(f.effectiveWeight * 100).roundToInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (f.rawScore != null) {
                                            "Norm ${f.rawScore.roundToInt()}/100 → +${f.weightedPoints} pts"
                                        } else {
                                            "UNKNOWN (Excluded)"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (f.rawScore != null) ElectricCyan else WarningAmber
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { ((f.rawScore ?: 0.0).toFloat() / 100f).coerceIn(0f, 1f) },
                                    color = if (f.rawScore == null) WarningAmber else priorityColor(priorityBreakdown.category),
                                    trackColor = ForestAbyssBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                Text(
                                    text = f.explanation,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Explainable Reasons for the Score
                        Surface(
                            color = ForestAbyssBg,
                            border = BorderStroke(1.dp, ForestBorderSubtle),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "REASONS FOR PRIORITY SCORE (R = ${priorityBreakdown.finalPriorityScore} • ${priorityBreakdown.category.uppercase()})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary
                                )
                                priorityBreakdown.reasonsSummary.forEach { reason ->
                                    Text(
                                        text = "• $reason",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "DATA COMPLETENESS (${priorityBreakdown.dataCompletenessPercent}%) & LIMITATIONS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = priorityBreakdown.confidenceLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmber
                                )
                                priorityBreakdown.limitationsSummary.forEach { lim ->
                                    Text(
                                        text = "• $lim",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Urgency: ${priorityBreakdown.recommendedUrgency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { onSyncWeatherToTree(tree) },
                                border = BorderStroke(1.dp, ElectricCyan),
                                modifier = Modifier.testTag("sync_weather_to_tree_button")
                            ) {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Weather (W)", color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (showFactorEditor) {
                            var sScore by remember(tree) { mutableDoubleStateOf(tree.structuralScore) }
                            var eScore by remember(tree) { mutableDoubleStateOf(tree.exposureScore) }
                            var dScore by remember(tree) { mutableDoubleStateOf(tree.declineScore) }
                            var wScore by remember(tree) { mutableDoubleStateOf(tree.weatherScore) }
                            var tScore by remember(tree) { mutableDoubleStateOf(tree.historyScore) }

                            Surface(
                                color = ForestAbyssBg,
                                border = BorderStroke(1.dp, ForestBorderSubtle),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Interactive Factor & Missing-Data Normalization Simulator (0–100 scale, or -1 for Unknown):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                    FactorSliderRow("S: Structural (35%)", sScore) { sScore = it }
                                    FactorSliderRow("E: Exposure (25%)", eScore) { eScore = it }
                                    FactorSliderRow("D: Decline (20%)", dScore) { dScore = it }
                                    FactorSliderRow("W: Weather (10%)", wScore) { wScore = it }
                                    FactorSliderRow("T: History Lag (10%)", tScore) { tScore = it }

                                    Button(
                                        onClick = { onUpdateRiskFactors(tree, sScore, eScore, dScore, wScore, tScore) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ForestAbyssBg),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Recalculate & Record Priority History", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Inspection Priority History Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "INSPECTION PRIORITY HISTORY (${treePriorityHistory.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Chronological log of Explainable Priority Score (R) updates across registration, weather surges, and inspections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        if (treePriorityHistory.isEmpty()) {
                            Text(
                                text = "Current Priority R = ${priorityBreakdown.finalPriorityScore} (${priorityBreakdown.category}). Recalculate factors or add an observation to log a new snapshot.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        } else {
                            treePriorityHistory.forEach { entry ->
                                Surface(
                                    color = ForestAbyssBg,
                                    border = BorderStroke(1.dp, priorityColor(entry.category).copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = entry.dateLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ElectricCyan
                                            )
                                            PriorityBadge(priority = entry.category, score = entry.finalScore)
                                        }
                                        Text(
                                            text = entry.triggerEvent,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Factors: S=${entry.structuralScore.roundToInt()} • E=${entry.exposureScore.roundToInt()} • D=${entry.declineScore.roundToInt()} • W=${entry.weatherScore.roundToInt()} • T=${entry.historyScore.roundToInt()} (${entry.dataCompletenessPercent}% complete)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. FEATURE 4: STORM MEMORY ENGINE (Side-by-Side Before & After Visual Comparison)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Compare, contentDescription = null, tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "STORM MEMORY ENGINE (HISTORICAL COMPARISON)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Side-by-side longitudinal photograph comparison via Gemini 3.8 Flash",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "EARLIER PHOTO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                                Image(
                                    bitmap = stormEarlierBmp.asImageBitmap(),
                                    contentDescription = "Earlier baseline tree photograph",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Text(
                                    text = stormEarlierLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            pickEarlierLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gallery", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            stormEarlierBmp = onProvidePresetBitmap(SampleTreePreset.BASELINE_HEALTHY_NEEM)
                                            stormEarlierLabel = "Pre-Monsoon Baseline ($registrationDateStr)"
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, ForestBorderSubtle)
                                    ) {
                                        Text("Baseline", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "NEWER PHOTO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmber
                                )
                                Image(
                                    bitmap = stormNewerBmp.asImageBitmap(),
                                    contentDescription = "Later post-storm tree photograph",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Text(
                                    text = stormNewerLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            pickNewerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gallery", color = WarningAmber, style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            stormNewerBmp = onProvidePresetBitmap(SampleTreePreset.POST_STORM_DAMAGED_NEEM)
                                            stormNewerLabel = "Post-Cyclone Inspection (Oct 2026)"
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, ForestBorderSubtle)
                                    ) {
                                        Text("Storm", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onRunStormMemory(
                                    tree,
                                    stormEarlierBmp,
                                    stormNewerBmp,
                                    stormEarlierLabel,
                                    stormNewerLabel
                                )
                            },
                            enabled = stormMemoryState !is AsyncTaskState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ForestAbyssBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_storm_memory_button")
                        ) {
                            if (stormMemoryState is AsyncTaskState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ForestAbyssBg,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Comparing Photographs with Gemini...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compare Earlier vs Newer Photo (Gemini)", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (stormMemoryState is AsyncTaskState.Error) {
                            Text(
                                text = "Comparison error: ${stormMemoryState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PriorityCriticalRed
                            )
                        }

                        if (stormMemoryState is AsyncTaskState.Success) {
                            val comp = stormMemoryState.data
                            Surface(
                                color = ForestAbyssBg,
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "LIVE GEMINI STORM MEMORY FINDINGS",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = "Valid Comparison: ${comp.isValidComparison} • Comparable Angles: ${comp.anglesComparable}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = "Possible Visual Changes: ${comp.possibleVisualChanges.joinToString("; ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Newly Visible Damage: ${comp.newlyVisibleDamage.joinToString("; ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarningAmber
                                    )
                                    Text(
                                        text = "Unchanged Features: ${comp.unchangedVisibleFeatures.joinToString("; ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Lighting/Angle & Limitations: ${comp.lightingAndAngleNotes} ${comp.comparisonLimitations}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Recommended Follow-up: ${comp.recommendedFollowUpInspection}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        treeStormComparisons.forEach { savedComp ->
                            Surface(
                                color = ForestAbyssBg,
                                border = BorderStroke(1.dp, ForestBorderSubtle),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${savedComp.earlierLabel} → ${savedComp.laterLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = "Changes: ${savedComp.possibleChanges}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Newly Visible Damage: ${savedComp.newlyVisibleDamage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarningAmber
                                    )
                                    Text(
                                        text = "Limitations: ${savedComp.limitations}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Stress Debt History for this Tree
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "TREE STRESS DEBT HISTORY (TD_t = ${tree.stressDebt})",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Previous TD_(t-1): ${tree.previousStressDebt} • Heat H_t: ${tree.heatContribution} • Dryness D_t: ${tree.drynessContribution} • Obs O_t: ${tree.observationContribution}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        StressDebtLineChart(records = treeStressRecords)
                        treeStressRecords.sortedByDescending { it.date }.forEach { sr ->
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
                                    Column {
                                        Text("${sr.date} • ${sr.dataSource}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                        Text(
                                            "H_t=${sr.heatIndicator.toInt()} • D_t=${sr.drynessIndicator.toInt()} • O_t=${sr.observationIndicator.toInt()} → Daily S_t=${sr.dailyStress}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Text("TD_t = ${sr.stressDebt}", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // 7. All Past Observations & AI Screening Reports Timeline
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PAST OBSERVATIONS & AI SCREENING REPORTS (${treeObservations.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = { showAddObsDialog = true },
                                border = BorderStroke(1.dp, EmeraldPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.AddComment, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Note", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (treeObservations.isEmpty()) {
                            Text(
                                text = "No observations logged yet. Run AI Tree Doctor or tap 'Add New Observation' to append a record.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        } else {
                            treeObservations.forEach { obs ->
                                val isAiReport = obs.source.contains("AI", true)
                                Surface(
                                    color = ForestAbyssBg,
                                    border = BorderStroke(1.dp, if (isAiReport) ElectricCyan.copy(alpha = 0.5f) else ForestBorderSubtle),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isAiReport) "AI SCREENING REPORT (${obs.source})" else "FIELD OBSERVATION (${obs.source})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isAiReport) ElectricCyan else EmeraldPrimary
                                            )
                                            Text(
                                                text = obs.visualCondition,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = conditionColor(obs.visualCondition),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Observable Symptoms: ${obs.visibleSymptoms}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                        if (obs.possibleConcerns.isNotBlank()) {
                                            Text(
                                                text = "Possible Concerns: ${obs.possibleConcerns}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = WarningAmber
                                            )
                                        }
                                        if (obs.supportingEvidence.isNotBlank()) {
                                            Text(
                                                text = "Supporting Evidence: ${obs.supportingEvidence}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "Recommended Action: ${obs.recommendedAction}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "Limitations: ${obs.limitations}",
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

            // 8. Maintenance History for this Tree
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                    border = BorderStroke(1.dp, ForestBorderSubtle),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MAINTENANCE HISTORY (${treeMaintActions.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            if (onAddMaintenanceAction != null) {
                                OutlinedButton(
                                    onClick = { showAddMaintDialog = true },
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("log_intervention_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Engineering,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log Intervention", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (treeMaintActions.isEmpty()) {
                            Text(
                                text = "No maintenance interventions proposed for this tree yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        } else {
                            treeMaintActions.forEach { act ->
                                Surface(
                                    color = ForestAbyssBg,
                                    border = BorderStroke(1.dp, ForestBorderSubtle),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(act.action, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                            Text(
                                                "Est. Cost: ₹${act.estimatedCost.toInt()} • Benefit: ${act.expectedBenefit.toInt()}/100 • Efficiency: ${String.format(Locale.US, "%.1f", act.efficiency)} pts/₹1k",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = act.status,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldPrimary
                                        )
                                    }
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
private fun UnifiedChronologicalPassportTimeline(
    tree: TreeRecord,
    registrationDateStr: String,
    originalBitmap: Bitmap,
    observations: List<ObservationRecord>,
    stressRecords: List<StressRecord>,
    priorityHistory: List<PriorityHistoryEntry>,
    stormComparisons: List<StormComparison>,
    maintActions: List<MaintenanceAction>
) {
    val events = remember(tree, observations, stressRecords, priorityHistory, stormComparisons, maintActions) {
        val list = mutableListOf<ChronologicalPassportItem>()

        // 1. Observations & AI Screening Reports (Newest first)
        observations.forEachIndexed { idx, obs ->
            val isAi = obs.source.contains("AI", true)
            list.add(
                ChronologicalPassportItem(
                    sortOrder = 900 - idx,
                    dateLabel = obs.observedAt?.toDate()?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(it)
                    } ?: "2026-10-30",
                    badgeLabel = if (isAi) "AI SCREENING REPORT" else "FIELD OBSERVATION",
                    badgeColor = if (isAi) ElectricCyan else EmeraldPrimary,
                    title = "${obs.visualCondition} • ${obs.imageQuality}",
                    body = "Symptoms: ${obs.visibleSymptoms}\nConcerns: ${obs.possibleConcerns}\nEvidence: ${obs.supportingEvidence}",
                    footer = "Action: ${obs.recommendedAction}"
                )
            )
        }

        // 2. Storm Memory Comparisons
        stormComparisons.forEachIndexed { idx, comp ->
            list.add(
                ChronologicalPassportItem(
                    sortOrder = 850 - idx,
                    dateLabel = "2026-10-30 • Storm Memory",
                    badgeLabel = "STORM MEMORY COMPARISON",
                    badgeColor = WarningAmber,
                    title = "${comp.earlierLabel} → ${comp.laterLabel}",
                    body = "Changes: ${comp.possibleChanges}\nNew Damage: ${comp.newlyVisibleDamage}",
                    footer = "Follow-up: ${comp.recommendedFollowUp}"
                )
            )
        }

        // 3. Inspection Priority History
        priorityHistory.forEachIndexed { idx, ph ->
            list.add(
                ChronologicalPassportItem(
                    sortOrder = 800 - idx,
                    dateLabel = ph.dateLabel,
                    badgeLabel = "PRIORITY ENGINE (${ph.category.uppercase()})",
                    badgeColor = priorityColor(ph.category),
                    title = "Priority Score R = ${ph.finalScore} / 100",
                    body = ph.triggerEvent,
                    footer = "S=${ph.structuralScore.toInt()} E=${ph.exposureScore.toInt()} D=${ph.declineScore.toInt()} W=${ph.weatherScore.toInt()} T=${ph.historyScore.toInt()}"
                )
            )
        }

        // 4. Stress Debt History
        stressRecords.sortedByDescending { it.date }.forEachIndexed { idx, sr ->
            list.add(
                ChronologicalPassportItem(
                    sortOrder = 700 - idx,
                    dateLabel = "${sr.date} • Stress Debt Engine",
                    badgeLabel = "STRESS DEBT SNAPSHOT",
                    badgeColor = ElectricCyan,
                    title = "Accumulated Stress Debt TD_t = ${sr.stressDebt} (Daily S_t = +${sr.dailyStress})",
                    body = "Heat H_t=${sr.heatIndicator} • Dryness D_t=${sr.drynessIndicator} • Observation O_t=${sr.observationIndicator}",
                    footer = "Source: ${sr.dataSource}"
                )
            )
        }

        // 5. Maintenance History
        maintActions.forEachIndexed { idx, act ->
            list.add(
                ChronologicalPassportItem(
                    sortOrder = 600 - idx,
                    dateLabel = "2026-10-28 • Maintenance Log",
                    badgeLabel = "MAINTENANCE (${act.status})",
                    badgeColor = EmeraldPrimary,
                    title = act.action,
                    body = "Estimated Cost: ₹${act.estimatedCost.toInt()} • Expected Benefit: ${act.expectedBenefit.toInt()}/100",
                    footer = "Efficiency: ${String.format(Locale.US, "%.1f", act.efficiency)} pts/₹1k"
                )
            )
        }

        // 6. Original Passport Registration Event (Earliest)
        list.add(
            ChronologicalPassportItem(
                sortOrder = 100,
                dateLabel = "$registrationDateStr • Origin Record",
                badgeLabel = "PASSPORT REGISTRATION",
                badgeColor = EmeraldPrimary,
                title = "Registered ${tree.treeCode} (${tree.species})",
                body = "Location: ${tree.zone} (${tree.latitude}, ${tree.longitude})\nOrigin: ${tree.registrationType}",
                footer = "Original Baseline Photograph archived in Digital Tree Passport."
            )
        )

        list.sortedByDescending { it.sortOrder }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ForestCardBg),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "UNIFIED CHRONOLOGICAL PASSPORT TIMELINE (${events.size} EVENTS)",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )
            Text(
                text = "Ordered newest-to-oldest from current observations and priority evaluations back to original registration.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            events.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 12.dp, top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(item.badgeColor, CircleShape)
                        )
                        if (index < events.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(95.dp)
                                    .background(ForestBorderSubtle)
                            )
                        }
                    }

                    Surface(
                        color = ForestAbyssBg,
                        border = BorderStroke(1.dp, item.badgeColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = item.badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.dateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = item.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (item.sortOrder == 100) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = originalBitmap.asImageBitmap(),
                                    contentDescription = "Original registration photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Text(
                                text = item.footer,
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

@Composable
private fun FactorSliderRow(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = if (value < 0) "UNKNOWN (-1)" else "${value.roundToInt()}/100",
                style = MaterialTheme.typography.labelSmall,
                color = if (value < 0) WarningAmber else EmeraldPrimary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = -1f..100f
        )
    }
}
