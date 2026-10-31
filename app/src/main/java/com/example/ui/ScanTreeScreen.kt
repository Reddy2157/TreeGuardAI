package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.SampleTreePreset
import com.example.data.StoredTreeImage
import com.example.data.TreeDoctorScreeningResult
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
import com.google.android.gms.location.LocationServices
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun ScanTreeScreen(
    trees: List<TreeRecord>,
    selectedTree: TreeRecord?,
    treeDoctorState: AsyncTaskState<TreeDoctorScreeningResult>,
    onSelectTargetTree: (TreeRecord) -> Unit,
    onRegisterNewTree: (
        species: String,
        lat: Double,
        lon: Double,
        zone: String,
        regType: String,
        notes: String,
        onCreated: (TreeRecord) -> Unit
    ) -> Unit,
    onRunAiTreeDoctor: (
        bitmap: Bitmap,
        targetTree: TreeRecord?,
        speciesHint: String,
        zoneHint: String,
        notesHint: String,
        imageTag: String
    ) -> Unit,
    onProvidePresetBitmap: (SampleTreePreset) -> Bitmap,
    onSpeakReport: (String) -> Unit,
    onAddMaintenanceAction: ((tree: TreeRecord, action: String, cost: Double, benefit: Double, status: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedPreset by remember { mutableStateOf(SampleTreePreset.POST_STORM_DAMAGED_NEEM) }
    var activeBitmap by remember {
        mutableStateOf(
            selectedTree?.let { TreeImageStorage.getTreeImage(context, it.treeCode) }
                ?: onProvidePresetBitmap(SampleTreePreset.POST_STORM_DAMAGED_NEEM)
        )
    }
    var activeImageSourceLabel by remember { mutableStateOf(SampleTreePreset.POST_STORM_DAMAGED_NEEM.id) }

    var speciesInput by remember(selectedTree) {
        mutableStateOf(selectedTree?.species ?: "Azadirachta indica (Neem)")
    }
    var zoneInput by remember(selectedTree) {
        mutableStateOf(selectedTree?.zone ?: "MG Road, Vijayawada")
    }
    var notesInput by remember { mutableStateOf("Visible upper-right canopy thinning & hanging limb after monsoon gusts.") }
    var latitude by remember(selectedTree) { mutableDoubleStateOf(selectedTree?.latitude ?: 16.5062) }
    var longitude by remember(selectedTree) { mutableDoubleStateOf(selectedTree?.longitude ?: 80.6480) }
    var registrationType by remember(selectedTree) { mutableStateOf(selectedTree?.registrationType ?: "MANUAL") }
    var gpsStatusText by remember { mutableStateOf("GPS telemetry ready (Capturing an image will tag exact device fix)") }

    var isCameraXOpen by remember { mutableStateOf(false) }
    var photoRefreshTrigger by remember { mutableIntStateOf(0) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Maintenance action inputs
    var maintActionInput by remember { mutableStateOf("Deadwood Canopy Pruning & Limb Clearance") }
    var maintCostInput by remember { mutableStateOf("1500") }
    var maintBenefitInput by remember { mutableDoubleStateOf(75.0) }
    var maintStatusInput by remember { mutableStateOf("PROPOSED") }
    var maintStatusMessage by remember { mutableStateOf<String?>(null) }

    // Real GPS Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    registrationType = "LIVE_GPS"
                    gpsStatusText = String.format(
                        Locale.US,
                        "Live GPS Fix Acquired: %.5f, %.5f (play-services-location)",
                        loc.latitude,
                        loc.longitude
                    )
                } else {
                    gpsStatusText = "Device GPS active; using coordinates: %.5f, %.5f".format(Locale.US, latitude, longitude)
                }
            }
        } else {
            gpsStatusText = "Location permission denied; using coordinates: %.5f, %.5f".format(Locale.US, latitude, longitude)
        }
    }

    // Handles any newly captured or selected photograph: fetches real GPS coordinates and persists to TreeImageStorage
    fun onPhotoAcquired(bmp: Bitmap, sourceTag: String) {
        activeBitmap = bmp
        activeImageSourceLabel = sourceTag

        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val targetCode = selectedTree?.treeCode ?: "TG-VJA-${String.format(Locale.US, "%04d", trees.size + 1)}"

        if (fineGranted || coarseGranted) {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    registrationType = "LIVE_GPS"
                    gpsStatusText = String.format(
                        Locale.US,
                        "GPS Tagged to Photo: %.5f, %.5f (Live LocationServices)",
                        loc.latitude,
                        loc.longitude
                    )
                } else {
                    gpsStatusText = String.format(Locale.US, "Photo tagged with coordinates: %.5f, %.5f", latitude, longitude)
                }
                TreeImageStorage.saveTreePhoto(
                    context = context,
                    treeCode = targetCode,
                    bitmap = bmp,
                    latitude = latitude,
                    longitude = longitude,
                    captureSource = sourceTag,
                    visualCondition = selectedTree?.visualCondition ?: "Healthy appearance"
                )
                photoRefreshTrigger++
            }.addOnFailureListener {
                TreeImageStorage.saveTreePhoto(
                    context = context,
                    treeCode = targetCode,
                    bitmap = bmp,
                    latitude = latitude,
                    longitude = longitude,
                    captureSource = sourceTag,
                    visualCondition = selectedTree?.visualCondition ?: "Healthy appearance"
                )
                photoRefreshTrigger++
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            TreeImageStorage.saveTreePhoto(
                context = context,
                treeCode = targetCode,
                bitmap = bmp,
                latitude = latitude,
                longitude = longitude,
                captureSource = sourceTag,
                visualCondition = selectedTree?.visualCondition ?: "Healthy appearance"
            )
            photoRefreshTrigger++
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onPhotoAcquired(bmp, "GALLERY_PHOTO")
            } catch (e: Exception) {
                gpsStatusText = "Could not decode selected gallery photo: ${e.localizedMessage}"
            }
        }
    }

    // Camera capture preview launcher (System fallback)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            onPhotoAcquired(bmp, "CAMERA_CAPTURE")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isCameraXOpen = true
        } else {
            gpsStatusText = "Camera permission denied — use Gallery Picker or Botanical Presets below."
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ForestAbyssBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        item {
            Column {
                Text(
                    text = "REGISTER TREE & AI TREE DOCTOR",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Capture or select a tree photograph, tag real GPS coordinates, and run Gemini visual health screening.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 2. Photograph Capture / Selection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STEP 1: CAPTURE OR SELECT TREE PHOTOGRAPH",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        Image(
                            bitmap = activeBitmap.asImageBitmap(),
                            contentDescription = "Selected tree photograph for AI Tree Doctor screening",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                        )
                        if (activeImageSourceLabel == "CAMERAX_CAPTURE") {
                            Surface(
                                color = EmeraldPrimary,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = ForestAbyssBg,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CameraX Live Photo",
                                        color = ForestAbyssBg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    isCameraXOpen = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestCardElevated,
                                contentColor = EmeraldPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_camera_button")
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CameraX", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestCardElevated,
                                contentColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_gallery_button")
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Or test immediately with built-in botanical inspection plates:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SampleTreePreset.entries) { preset ->
                            val isSelected = selectedPreset == preset && activeImageSourceLabel == preset.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPreset = preset
                                    val bmp = onProvidePresetBitmap(preset)
                                    onPhotoAcquired(bmp, preset.id)
                                },
                                label = {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. GPS & Tree Passport Registration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STEP 2: TREE IDENTITY & REAL GPS COORDINATES",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = String.format(Locale.US, "Lat: %.5f • Lon: %.5f (%s)", latitude, longitude, registrationType),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = gpsStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            border = BorderStroke(1.dp, ElectricCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("acquire_gps_button")
                        ) {
                            Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get GPS", color = ElectricCyan, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = speciesInput,
                        onValueChange = { speciesInput = it },
                        label = { Text("Species (if known)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("species_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = zoneInput,
                        onValueChange = { zoneInput = it },
                        label = { Text("Zone / Street / Ward Description") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zone_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Field Observation Note") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val enrichedNote = if (notesInput.contains("Image:")) {
                                notesInput
                            } else {
                                "$notesInput [Image: $activeImageSourceLabel | GPS: ${String.format(Locale.US, "%.5f, %.5f", latitude, longitude)} | Res: ${activeBitmap.width}x${activeBitmap.height}]"
                            }
                            onRegisterNewTree(
                                speciesInput,
                                latitude,
                                longitude,
                                zoneInput,
                                registrationType,
                                enrichedNote
                            ) { createdTree ->
                                TreeImageStorage.saveTreePhoto(
                                    context = context,
                                    treeCode = createdTree.treeCode,
                                    bitmap = activeBitmap,
                                    latitude = latitude,
                                    longitude = longitude,
                                    captureSource = activeImageSourceLabel,
                                    visualCondition = createdTree.visualCondition
                                )
                                photoRefreshTrigger++
                                onSelectTargetTree(createdTree)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = ForestAbyssBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_tree_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Register New Digital Tree Passport (TG-VJA-${String.format(Locale.US, "%04d", trees.size + 1)})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Target Tree Selection + Run AI Tree Doctor Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardElevated),
                border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STEP 3: RUN AI TREE DOCTOR VISUAL SCREENING",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Associate screening with a registered Tree Passport:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(trees) { tree ->
                            val isSelected = selectedTree?.id == tree.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectTargetTree(tree) },
                                label = {
                                    Text("${tree.treeCode} (${tree.species.substringBefore("(").trim()})")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onRunAiTreeDoctor(
                                activeBitmap,
                                selectedTree ?: trees.firstOrNull(),
                                speciesInput,
                                zoneInput,
                                notesInput,
                                activeImageSourceLabel
                            )
                        },
                        enabled = treeDoctorState !is AsyncTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ForestAbyssBg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("run_ai_tree_doctor_button")
                    ) {
                        if (treeDoctorState is AsyncTaskState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ForestAbyssBg,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Gemini 3.8 Flash Inspecting Canopy & Bark...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.HealthAndSafety, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Run AI Tree Doctor Screening (${selectedTree?.treeCode ?: "TG-VJA-0001"})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. AI Tree Doctor Report Output
        item {
            when (treeDoctorState) {
                is AsyncTaskState.Idle -> {}
                is AsyncTaskState.Loading -> {}
                is AsyncTaskState.Error -> {
                    Surface(
                        color = PriorityCriticalRed.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, PriorityCriticalRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Tree Doctor Error: ${treeDoctorState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriorityCriticalRed,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is AsyncTaskState.Success -> {
                    TreeDoctorReportCard(
                        report = treeDoctorState.data,
                        onSpeakReport = {
                            val r = treeDoctorState.data
                            val speech = "AI Tree Doctor Report. Overall visual condition: ${r.overallVisualCondition}. " +
                                "Possible species: ${r.possibleSpecies}. " +
                                "Visible symptoms: ${r.visibleSymptoms.joinToString(", ")}. " +
                                "Recommended next steps: ${r.recommendedNextSteps.joinToString(", ")}. " +
                                "Note: This is an initial visual observation tool, not a certified arborist diagnosis."
                            onSpeakReport(speech)
                        }
                    )
                }
            }
        }

        // 6. STEP 4: LOG MAINTENANCE RECORD TO FIRESTORE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ForestBorderSubtle),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_maintenance_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STEP 4: LOG ARBORIST MAINTENANCE RECORD",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Persist maintenance intervention to Firestore for ${selectedTree?.treeCode ?: "TG-VJA-0001"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quick Intervention Templates:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val quickInterventions = listOf(
                        Triple("Deadwood Pruning & Limb Clearance", 1500.0, 75.0),
                        Triple("Root Flare Mulching & Decompaction", 1200.0, 80.0),
                        Triple("Foliar Fungicide & Insect Spray", 850.0, 60.0),
                        Triple("Bark Antiseptic & Trunk Cavity Treatment", 950.0, 65.0),
                        Triple("Emergency Storm Hazard Removal", 2200.0, 90.0)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickInterventions) { (actionTitle, cost, benefit) ->
                            FilterChip(
                                selected = maintActionInput == actionTitle,
                                onClick = {
                                    maintActionInput = actionTitle
                                    maintCostInput = cost.toInt().toString()
                                    maintBenefitInput = benefit
                                },
                                label = { Text(actionTitle, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = maintActionInput,
                        onValueChange = { maintActionInput = it },
                        label = { Text("Intervention Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("maint_action_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = maintCostInput,
                            onValueChange = { maintCostInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Estimated Cost (₹ INR)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("maint_cost_input")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Expected Benefit: ${maintBenefitInput.toInt()}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                            Slider(
                                value = maintBenefitInput.toFloat(),
                                onValueChange = { maintBenefitInput = it.toDouble() },
                                valueRange = 10f..100f,
                                modifier = Modifier.testTag("maint_benefit_slider")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Workflow Status:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PROPOSED", "SCHEDULED", "COMPLETED").forEach { status ->
                            FilterChip(
                                selected = maintStatusInput == status,
                                onClick = { maintStatusInput = status },
                                label = { Text(status, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (status) {
                                        "COMPLETED" -> PriorityLowGreen.copy(alpha = 0.25f)
                                        "SCHEDULED" -> PriorityModerateYellow.copy(alpha = 0.25f)
                                        else -> PriorityHighOrange.copy(alpha = 0.25f)
                                    },
                                    selectedLabelColor = when (status) {
                                        "COMPLETED" -> PriorityLowGreen
                                        "SCHEDULED" -> PriorityModerateYellow
                                        else -> PriorityHighOrange
                                    }
                                )
                            )
                        }
                    }

                    if (maintStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = maintStatusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val target = selectedTree ?: trees.firstOrNull()
                            if (target != null && onAddMaintenanceAction != null) {
                                val cost = maintCostInput.toDoubleOrNull() ?: 1500.0
                                onAddMaintenanceAction(target, maintActionInput, cost, maintBenefitInput, maintStatusInput)
                                maintStatusMessage = "Logged maintenance record to Firestore for ${target.treeCode}!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = ForestAbyssBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_maintenance_button")
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Maintenance Record to Firestore (${selectedTree?.treeCode ?: "TG-VJA-0001"})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 7. STEP 5: STORED TREE IMAGES ARCHIVE (Shows user-captured & stored tree photos)
        item {
            val targetTreeCode = selectedTree?.treeCode ?: "TG-VJA-0001"
            val storedPhotos = remember(targetTreeCode, photoRefreshTrigger) {
                TreeImageStorage.getAllPhotosForTree(context, targetTreeCode)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ForestCardBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stored_tree_images_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STORED TREE IMAGES ARCHIVE",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "On-Device & Firestore Photographs for $targetTreeCode (${storedPhotos.size} saved)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (storedPhotos.isEmpty()) {
                        Surface(
                            color = ForestAbyssBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ForestBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No user photographs stored yet for $targetTreeCode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "Tap 'CameraX' or 'Gallery' above to capture or select a photo with live GPS coordinates.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(storedPhotos) { photo ->
                                val bmp = remember(photo.fileName, photoRefreshTrigger) {
                                    TreeImageStorage.getTreeImage(context, targetTreeCode)
                                }
                                Surface(
                                    color = ForestCardElevated,
                                    border = BorderStroke(1.dp, ForestBorderSubtle),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.width(180.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Stored photo for $targetTreeCode",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(ForestAbyssBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = TextMuted)
                                                }
                                            }
                                            Surface(
                                                color = ForestAbyssBg.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = photo.captureSource.replace("_", " "),
                                                    color = EmeraldPrimary,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.4f, %.4f", photo.latitude, photo.longitude),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = photo.dateLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = photo.resolution,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                bmp?.let { activeBitmap = it }
                                                activeImageSourceLabel = photo.captureSource
                                                latitude = photo.latitude
                                                longitude = photo.longitude
                                                gpsStatusText = String.format(Locale.US, "Loaded stored photo GPS: %.5f, %.5f", photo.latitude, photo.longitude)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text("Inspect Photo", style = MaterialTheme.typography.labelSmall)
                                        }
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

    if (isCameraXOpen) {
        CameraXTreeScannerModal(
            onPhotoCaptured = { bmp ->
                onPhotoAcquired(bmp, "CAMERAX_CAPTURE")
                isCameraXOpen = false
            },
            onDismiss = { isCameraXOpen = false }
        )
    }
}

@Composable
private fun TreeDoctorReportCard(
    report: TreeDoctorScreeningResult,
    onSpeakReport: () -> Unit
) {
    val condColor = conditionColor(report.overallVisualCondition)

    Card(
        colors = CardDefaults.cardColors(containerColor = ForestCardBg),
        border = BorderStroke(1.5.dp, condColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tree_doctor_report_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI TREE DOCTOR SCREENING REPORT",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Text(
                        text = report.overallVisualCondition,
                        style = MaterialTheme.typography.headlineSmall,
                        color = condColor
                    )
                }
                IconButton(
                    onClick = onSpeakReport,
                    modifier = Modifier.testTag("tts_tree_doctor_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Listen to AI Tree Doctor Report (TTS)",
                        tint = ElectricCyan
                    )
                }
            }

            if (report.professionalInspectionRecommended) {
                Surface(
                    color = PriorityCriticalRed.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, PriorityCriticalRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "PROFESSIONAL ARBORIST INSPECTION RECOMMENDED: Visible structural or bark abnormalities warrant on-site verification.",
                        style = MaterialTheme.typography.labelMedium,
                        color = PriorityCriticalRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            ReportSectionBlock(
                heading = "1. What the AI Can Actually See (Observable Visual Evidence)",
                accent = EmeraldPrimary,
                items = report.visibleSymptoms.ifEmpty { listOf("No acute foliar or bark lesions clearly visible.") },
                footer = "Supporting Evidence: ${report.supportingVisualEvidence} • Image Quality: ${report.imageQuality}"
            )

            ReportSectionBlock(
                heading = "2. What Those Visible Signs Might Indicate (Non-Diagnostic Concerns)",
                accent = WarningAmber,
                items = report.possibleConcerns.ifEmpty { listOf("No immediate visual stress concerns identified.") },
                footer = "Possible Species Identification: ${report.possibleSpecies}"
            )

            ReportSectionBlock(
                heading = "3. Recommended Next Steps & Screening Limitations",
                accent = ElectricCyan,
                items = report.recommendedNextSteps.ifEmpty { listOf("Continue scheduled seasonal monitoring.") },
                footer = "Limitations: ${report.limitations}"
            )
        }
    }
}

@Composable
private fun ReportSectionBlock(
    heading: String,
    accent: androidx.compose.ui.graphics.Color,
    items: List<String>,
    footer: String
) {
    Surface(
        color = ForestAbyssBg,
        border = BorderStroke(1.dp, ForestBorderSubtle),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            items.forEach { bullet ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
