package com.example.ui

import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.R
import com.example.data.BudgetOptimizationResult
import com.example.data.DemoDataFactory
import com.example.data.GroundedSpeciesIntelligence
import com.example.data.MaintenanceAction
import com.example.data.ObservationRecord
import com.example.data.OpenMeteoWeatherService
import com.example.data.PriorityHistoryEntry
import com.example.data.RegionalWeatherSnapshot
import com.example.data.SampleTreeBitmaps
import com.example.data.SampleTreePreset
import com.example.data.StormComparison
import com.example.data.StormMemoryComparisonResult
import com.example.data.StressCalculationOutput
import com.example.data.StressRecord
import com.example.data.TreeDoctorScreeningResult
import com.example.data.TreeGuardAiService
import com.example.data.TreeGuardEngines
import com.example.data.TreeGuardRepository
import com.example.data.TreeRecord
import com.example.data.UrbanForestAiInsights
import com.example.data.WeatherRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface AsyncTaskState<out T> {
    data object Idle : AsyncTaskState<Nothing>
    data object Loading : AsyncTaskState<Nothing>
    data class Success<T>(val data: T) : AsyncTaskState<T>
    data class Error(val message: String) : AsyncTaskState<Nothing>
}

data class TreeGuardUiState(
    val trees: List<TreeRecord> = emptyList(),
    val observations: List<ObservationRecord> = emptyList(),
    val stressRecords: List<StressRecord> = emptyList(),
    val maintenanceActions: List<MaintenanceAction> = emptyList(),
    val stormComparisons: List<StormComparison> = emptyList(),
    val priorityHistory: List<PriorityHistoryEntry> = emptyList(),
    val isUsingDemoFallback: Boolean = true,
    val weather: RegionalWeatherSnapshot? = null,
    val isWeatherLoading: Boolean = false,
    val budgetInr: Double = 10000.0,
    val optimizationResult: BudgetOptimizationResult = TreeGuardEngines.optimizeMaintenanceBudget(
        DemoDataFactory.defaultMaintenanceActions(),
        10000.0
    ),
    val selectedTreeId: String? = null,
    val statusBannerMessage: String? = null
)

class TreeGuardViewModel(
    private val repository: TreeGuardRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _weather = MutableStateFlow<RegionalWeatherSnapshot?>(null)
    private val _isWeatherLoading = MutableStateFlow(false)
    private val _budgetInr = MutableStateFlow(10000.0)
    private val _selectedTreeId = MutableStateFlow<String?>(null)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _priorityHistory = MutableStateFlow(DemoDataFactory.defaultPriorityHistory())

    private val _treeDoctorState = MutableStateFlow<AsyncTaskState<TreeDoctorScreeningResult>>(AsyncTaskState.Idle)
    val treeDoctorState: StateFlow<AsyncTaskState<TreeDoctorScreeningResult>> = _treeDoctorState.asStateFlow()

    private val _stormMemoryState = MutableStateFlow<AsyncTaskState<StormMemoryComparisonResult>>(AsyncTaskState.Idle)
    val stormMemoryState: StateFlow<AsyncTaskState<StormMemoryComparisonResult>> = _stormMemoryState.asStateFlow()

    private val _groundedSearchState = MutableStateFlow<AsyncTaskState<GroundedSpeciesIntelligence>>(AsyncTaskState.Idle)
    val groundedSearchState: StateFlow<AsyncTaskState<GroundedSpeciesIntelligence>> = _groundedSearchState.asStateFlow()

    private val _forestInsightsState = MutableStateFlow<AsyncTaskState<UrbanForestAiInsights>>(AsyncTaskState.Idle)
    val forestInsightsState: StateFlow<AsyncTaskState<UrbanForestAiInsights>> = _forestInsightsState.asStateFlow()

    private val _isSeedingCloud = MutableStateFlow(false)
    val isSeedingCloud: StateFlow<Boolean> = _isSeedingCloud.asStateFlow()

    private val cloudTreesFlow = repository.observeTrees(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing trees, using demo fallback", e)
            emit(emptyList())
        }

    private val cloudObsFlow = repository.observeObservations(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing observations", e)
            emit(emptyList())
        }

    private val cloudStressFlow = repository.observeStressRecords(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing stress records", e)
            emit(emptyList())
        }

    private val cloudMaintFlow = repository.observeMaintenanceActions(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing maintenance actions", e)
            emit(emptyList())
        }

    private val cloudStormFlow = repository.observeStormComparisons(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing storm comparisons", e)
            emit(emptyList())
        }

    val uiState: StateFlow<TreeGuardUiState> = combine(
        combine(cloudTreesFlow, cloudObsFlow, cloudStressFlow) { t, o, s -> Triple(t, o, s) },
        combine(cloudMaintFlow, cloudStormFlow) { m, st -> m to st },
        combine(_weather, _isWeatherLoading, _budgetInr) { w, wl, b -> Triple(w, wl, b) },
        combine(_selectedTreeId, _statusBanner, _priorityHistory) { sel, msg, ph -> Triple(sel, msg, ph) }
    ) { (cloudTrees, cloudObs, cloudStress), (cloudMaint, cloudStorm), (weather, weatherLoading, budget), (selectedId, banner, prioHistory) ->
        val useDemo = cloudTrees.isEmpty()
        val activeTrees = if (useDemo) DemoDataFactory.defaultVijayawadaTrees(currentUserId) else cloudTrees
        val activeObs = if (useDemo && cloudObs.isEmpty()) DemoDataFactory.defaultObservations(currentUserId) else cloudObs
        val activeStress = if (useDemo && cloudStress.isEmpty()) DemoDataFactory.defaultStressHistory(currentUserId) else cloudStress
        val activeMaint = if (useDemo && cloudMaint.isEmpty()) DemoDataFactory.defaultMaintenanceActions(currentUserId) else cloudMaint
        val activeStorm = if (useDemo && cloudStorm.isEmpty()) DemoDataFactory.defaultStormComparisons(currentUserId) else cloudStorm

        TreeGuardUiState(
            trees = activeTrees,
            observations = activeObs,
            stressRecords = activeStress,
            maintenanceActions = activeMaint,
            stormComparisons = activeStorm,
            priorityHistory = prioHistory,
            isUsingDemoFallback = useDemo,
            weather = weather,
            isWeatherLoading = weatherLoading,
            budgetInr = budget,
            optimizationResult = TreeGuardEngines.optimizeMaintenanceBudget(activeMaint, budget),
            selectedTreeId = selectedId ?: activeTrees.firstOrNull()?.id,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TreeGuardUiState(
            trees = DemoDataFactory.defaultVijayawadaTrees(currentUserId),
            observations = DemoDataFactory.defaultObservations(currentUserId),
            stressRecords = DemoDataFactory.defaultStressHistory(currentUserId),
            maintenanceActions = DemoDataFactory.defaultMaintenanceActions(currentUserId),
            stormComparisons = DemoDataFactory.defaultStormComparisons(currentUserId),
            priorityHistory = DemoDataFactory.defaultPriorityHistory(),
            isUsingDemoFallback = true
        )
    )

    init {
        refreshRegionalWeather()
    }

    fun selectTree(treeId: String?) {
        _selectedTreeId.value = treeId
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun setBudgetScenario(newBudgetInr: Double) {
        _budgetInr.value = newBudgetInr.coerceIn(500.0, 500000.0)
    }

    fun refreshRegionalWeather(
        lat: Double = OpenMeteoWeatherService.VIJAYAWADA_LAT,
        lon: Double = OpenMeteoWeatherService.VIJAYAWADA_LON,
        label: String = OpenMeteoWeatherService.VIJAYAWADA_LABEL,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            try {
                val snapshot = OpenMeteoWeatherService.fetchRegionalWeather(
                    latitude = lat,
                    longitude = lon,
                    locationLabel = label,
                    forceRefresh = forceRefresh
                )
                _weather.value = snapshot
                val cacheTag = when (snapshot.cacheStatus) {
                    "CACHED_RECENT" -> "Served from recent Open-Meteo cache"
                    "CACHED_FALLBACK" -> "Offline fallback: served from cached Open-Meteo snapshot"
                    "DEMO_FALLBACK" -> "Using Vijayawada regional fallback"
                    else -> "Live Open-Meteo weather retrieved"
                }
                if (forceRefresh) {
                    _statusBanner.value = "$cacheTag for ${snapshot.locationLabel} (${snapshot.temperature}°C)"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Weather refresh failed", e)
            } finally {
                _isWeatherLoading.value = false
            }
        }
    }

    /**
     * Integrates live/cached Open-Meteo weather telemetry directly as input into the
     * Tree Stress Debt Engine (H_t, D_t) and Explainable Tree Inspection-Priority Engine (W)
     * for either a specific tree or all active trees.
     */
    fun applyWeatherToEngines(targetTree: TreeRecord? = null) {
        viewModelScope.launch {
            val w = _weather.value ?: OpenMeteoWeatherService.fetchRegionalWeather()
            _weather.value = w

            // Log weather record to Firestore
            repository.addWeatherRecord(
                WeatherRecord(
                    userId = currentUserId,
                    zone = w.locationLabel,
                    latitude = w.latitude,
                    longitude = w.longitude,
                    temperature = w.temperature,
                    humidity = w.humidity,
                    windSpeed = w.windSpeed,
                    windGusts = w.windGusts,
                    precipitation = w.precipitation,
                    soilMoisture = w.soilMoisture ?: -1.0,
                    dataSource = "Open-Meteo (${w.cacheStatus})"
                )
            )

            val treesToUpdate = if (targetTree != null) listOf(targetTree) else uiState.value.trees
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val dateTimeLabel = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

            val newHistoryEntries = mutableListOf<PriorityHistoryEntry>()

            for (tree in treesToUpdate) {
                val stressCalc = TreeGuardEngines.calculateStressDebt(
                    previousStressDebt = tree.stressDebt,
                    deltaDays = 1.0,
                    heatIndicator = w.derivedHeatIndicator,
                    drynessIndicator = w.derivedDrynessIndicator,
                    observationIndicator = tree.observationContribution,
                    halfLifeDays = 30.0,
                    dataSourceLabel = "OPEN_METEO_REGIONAL"
                )
                val priorityEval = TreeGuardEngines.evaluateInspectionPriority(
                    structuralScore = tree.structuralScore.takeIf { it >= 0.0 },
                    exposureScore = tree.exposureScore.takeIf { it >= 0.0 },
                    declineScore = tree.declineScore.takeIf { it >= 0.0 },
                    weatherScore = w.derivedWeatherPriorityScore,
                    historyScore = tree.historyScore.takeIf { it >= 0.0 },
                    visualCondition = tree.visualCondition,
                    soilMoistureAvailable = w.soilMoisture != null
                )

                repository.addStressRecord(
                    StressRecord(
                        userId = currentUserId,
                        treeId = tree.id,
                        date = today,
                        heatIndicator = stressCalc.heatIndicator,
                        drynessIndicator = stressCalc.drynessIndicator,
                        observationIndicator = stressCalc.observationIndicator,
                        dailyStress = stressCalc.dailyStress,
                        stressDebt = stressCalc.newAccumulatedDebt,
                        deltaDays = 1.0,
                        dataSource = "OPEN_METEO_REGIONAL"
                    )
                )

                if (uiState.value.isUsingDemoFallback) {
                    repository.createTree(
                        tree.copy(
                            currentPriority = priorityEval.category,
                            currentRiskScore = priorityEval.finalPriorityScore,
                            weatherScore = w.derivedWeatherPriorityScore,
                            stressDebt = stressCalc.newAccumulatedDebt,
                            previousStressDebt = tree.stressDebt,
                            heatContribution = w.derivedHeatIndicator,
                            drynessContribution = w.derivedDrynessIndicator
                        ),
                        tree.id
                    )
                } else {
                    repository.updateTreeTelemetry(
                        treeId = tree.id,
                        visualCondition = tree.visualCondition,
                        currentPriority = priorityEval.category,
                        currentRiskScore = priorityEval.finalPriorityScore,
                        structuralScore = tree.structuralScore,
                        exposureScore = tree.exposureScore,
                        declineScore = tree.declineScore,
                        weatherScore = w.derivedWeatherPriorityScore,
                        historyScore = tree.historyScore,
                        stressDebt = stressCalc.newAccumulatedDebt,
                        previousStressDebt = tree.stressDebt,
                        heatContribution = w.derivedHeatIndicator,
                        drynessContribution = w.derivedDrynessIndicator,
                        observationContribution = tree.observationContribution,
                        notes = tree.notes
                    )
                }

                newHistoryEntries.add(
                    PriorityHistoryEntry(
                        id = "prio_${UUID.randomUUID().toString().take(8)}",
                        treeId = tree.id,
                        treeCode = tree.treeCode,
                        dateLabel = "$dateTimeLabel • Open-Meteo Telemetry Sync",
                        finalScore = priorityEval.finalPriorityScore,
                        category = priorityEval.category,
                        structuralScore = tree.structuralScore,
                        exposureScore = tree.exposureScore,
                        declineScore = tree.declineScore,
                        weatherScore = w.derivedWeatherPriorityScore,
                        historyScore = tree.historyScore,
                        triggerEvent = "Synced Open-Meteo (${w.temperature}°C, Gusts ${w.windGusts} km/h) -> H_t=${w.derivedHeatIndicator}, D_t=${w.derivedDrynessIndicator}, W=${w.derivedWeatherPriorityScore}",
                        dataCompletenessPercent = priorityEval.dataCompletenessPercent
                    )
                )
            }

            _priorityHistory.value = newHistoryEntries + _priorityHistory.value
            val scopeLabel = targetTree?.treeCode ?: "${treesToUpdate.size} Vijayawada trees"
            _statusBanner.value = "Integrated Open-Meteo weather (H_t=${w.derivedHeatIndicator}, D_t=${w.derivedDrynessIndicator}, W=${w.derivedWeatherPriorityScore}) into Stress Debt & Priority Engine for $scopeLabel"
        }
    }

    fun seedDemoDatasetToFirestore() {
        if (_isSeedingCloud.value) return
        viewModelScope.launch {
            _isSeedingCloud.value = true
            try {
                val demoTrees = DemoDataFactory.defaultVijayawadaTrees(currentUserId)
                for (tree in demoTrees) {
                    repository.createTree(tree, tree.id)
                }
                for (obs in DemoDataFactory.defaultObservations(currentUserId)) {
                    repository.addObservation(obs)
                }
                for (stress in DemoDataFactory.defaultStressHistory(currentUserId)) {
                    repository.addStressRecord(stress)
                }
                for (maint in DemoDataFactory.defaultMaintenanceActions(currentUserId)) {
                    repository.addMaintenanceAction(maint)
                }
                for (storm in DemoDataFactory.defaultStormComparisons(currentUserId)) {
                    repository.addStormComparison(storm)
                }
                _statusBanner.value = "Seeded Vijayawada demo inventory into Cloud Firestore."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed demo dataset", e)
                _statusBanner.value = "Cloud sync error: ${e.localizedMessage ?: "Failed to seed"}"
            } finally {
                _isSeedingCloud.value = false
            }
        }
    }

    fun registerNewTree(
        species: String,
        latitude: Double,
        longitude: Double,
        zone: String,
        registrationType: String,
        notes: String,
        exposureScore: Double = 65.0,
        onRegistered: (TreeRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentList = uiState.value.trees
            val nextNum = currentList.size + 1
            val treeCode = String.format(Locale.US, "TG-VJA-%04d", nextNum)

            val weatherSnap = _weather.value
            val heatInd = weatherSnap?.derivedHeatIndicator ?: 65.0
            val dryInd = weatherSnap?.derivedDrynessIndicator ?: 55.0
            val weatherRisk = weatherSnap?.derivedWeatherPriorityScore
                ?: ((heatInd * 0.6) + (dryInd * 0.4)).coerceIn(0.0, 100.0)

            val priorityEval = TreeGuardEngines.evaluateInspectionPriority(
                structuralScore = 25.0,
                exposureScore = exposureScore,
                declineScore = 20.0,
                weatherScore = weatherRisk,
                historyScore = 15.0,
                visualCondition = "Healthy appearance",
                soilMoistureAvailable = weatherSnap?.soilMoisture != null
            )
            val initialStress = TreeGuardEngines.calculateStressDebt(
                previousStressDebt = 15.0,
                deltaDays = 1.0,
                heatIndicator = heatInd,
                drynessIndicator = dryInd,
                observationIndicator = 20.0
            )

            val newTree = TreeRecord(
                userId = currentUserId,
                treeCode = treeCode,
                species = species.ifBlank { "Azadirachta indica (Neem)" },
                latitude = latitude,
                longitude = longitude,
                zone = zone.ifBlank { "Vijayawada Municipal Ward" },
                registrationType = registrationType,
                visualCondition = "Healthy appearance",
                currentPriority = priorityEval.category,
                currentRiskScore = priorityEval.finalPriorityScore,
                structuralScore = 25.0,
                exposureScore = exposureScore,
                declineScore = 20.0,
                weatherScore = weatherRisk,
                historyScore = 15.0,
                stressDebt = initialStress.newAccumulatedDebt,
                previousStressDebt = 15.0,
                heatContribution = heatInd,
                drynessContribution = dryInd,
                observationContribution = 20.0,
                notes = notes.ifBlank { "Initial field registration completed." }
            )

            val result = repository.createTree(newTree)
            result.onSuccess { createdId ->
                val savedTree = newTree.copy(id = createdId)
                _selectedTreeId.value = createdId
                val dateTimeLabel = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                _priorityHistory.value = listOf(
                    PriorityHistoryEntry(
                        id = "prio_${UUID.randomUUID().toString().take(8)}",
                        treeId = createdId,
                        treeCode = treeCode,
                        dateLabel = "$dateTimeLabel • Initial Passport Registration",
                        finalScore = priorityEval.finalPriorityScore,
                        category = priorityEval.category,
                        structuralScore = 25.0,
                        exposureScore = exposureScore,
                        declineScore = 20.0,
                        weatherScore = weatherRisk,
                        historyScore = 15.0,
                        triggerEvent = "Registered Digital Tree Passport ($registrationType) at $zone",
                        dataCompletenessPercent = priorityEval.dataCompletenessPercent
                    )
                ) + _priorityHistory.value

                _statusBanner.value = "Registered Digital Tree Passport $treeCode ($registrationType)"
                onRegistered(savedTree)
            }.onFailure { err ->
                _statusBanner.value = "Failed to register tree: ${err.localizedMessage}"
            }
        }
    }

    fun runAiTreeDoctor(
        bitmap: Bitmap,
        targetTree: TreeRecord?,
        speciesHint: String,
        zoneHint: String,
        notesHint: String,
        imageTagLabel: String
    ) {
        viewModelScope.launch {
            _treeDoctorState.value = AsyncTaskState.Loading
            try {
                val result = TreeGuardAiService.runTreeDoctorScreening(
                    bitmap = bitmap,
                    speciesHint = speciesHint.ifBlank { targetTree?.species.orEmpty() },
                    zoneHint = zoneHint.ifBlank { targetTree?.zone ?: "Vijayawada, AP" },
                    notesHint = notesHint
                )
                _treeDoctorState.value = AsyncTaskState.Success(result)

                // Persist observation & update target tree if selected
                if (targetTree != null) {
                    val obs = ObservationRecord(
                        userId = currentUserId,
                        treeId = targetTree.id,
                        treeCode = targetTree.treeCode,
                        imageTag = imageTagLabel,
                        visualCondition = result.overallVisualCondition,
                        imageQuality = result.imageQuality,
                        possibleSpecies = result.possibleSpecies,
                        visibleSymptoms = result.visibleSymptoms.joinToString("; ").ifBlank { "No severe abnormalities observed" },
                        possibleConcerns = result.possibleConcerns.joinToString("; ").ifBlank { "Routine monitoring" },
                        supportingEvidence = result.supportingVisualEvidence,
                        recommendedAction = result.recommendedNextSteps.joinToString("; ").ifBlank { "Continue periodic visual checks" },
                        limitations = result.limitations,
                        professionalInspection = result.professionalInspectionRecommended,
                        source = "AI_TREE_DOCTOR"
                    )
                    repository.addObservation(obs)

                    // Update tree risk & stress indicators based on AI visual screening
                    val newObsStress = when (result.overallVisualCondition) {
                        "Needs inspection" -> 82.0
                        "Monitor" -> 55.0
                        "Unclear" -> 40.0
                        else -> 15.0
                    }
                    val newStructural = if (result.professionalInspectionRecommended) {
                        maxOf(targetTree.structuralScore, 76.0)
                    } else {
                        targetTree.structuralScore.coerceAtLeast(20.0)
                    }
                    val priorityEval = TreeGuardEngines.evaluateInspectionPriority(
                        structuralScore = newStructural,
                        exposureScore = targetTree.exposureScore,
                        declineScore = newObsStress,
                        weatherScore = targetTree.weatherScore,
                        historyScore = 10.0, // freshly inspected
                        visualCondition = result.overallVisualCondition
                    )
                    val stressOut = TreeGuardEngines.calculateStressDebt(
                        previousStressDebt = targetTree.stressDebt,
                        deltaDays = 1.0,
                        heatIndicator = targetTree.heatContribution,
                        drynessIndicator = targetTree.drynessContribution,
                        observationIndicator = newObsStress
                    )
                    if (!uiState.value.isUsingDemoFallback) {
                        repository.updateTreeTelemetry(
                            treeId = targetTree.id,
                            visualCondition = result.overallVisualCondition,
                            currentPriority = priorityEval.category,
                            currentRiskScore = priorityEval.finalPriorityScore,
                            structuralScore = newStructural,
                            exposureScore = targetTree.exposureScore,
                            declineScore = newObsStress,
                            weatherScore = targetTree.weatherScore,
                            historyScore = 10.0,
                            stressDebt = stressOut.newAccumulatedDebt,
                            previousStressDebt = targetTree.stressDebt,
                            heatContribution = targetTree.heatContribution,
                            drynessContribution = targetTree.drynessContribution,
                            observationContribution = newObsStress,
                            notes = targetTree.notes
                        )
                    }

                    val dateTimeLabel = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                    _priorityHistory.value = listOf(
                        PriorityHistoryEntry(
                            id = "prio_${UUID.randomUUID().toString().take(8)}",
                            treeId = targetTree.id,
                            treeCode = targetTree.treeCode,
                            dateLabel = "$dateTimeLabel • AI Tree Doctor Screening",
                            finalScore = priorityEval.finalPriorityScore,
                            category = priorityEval.category,
                            structuralScore = newStructural,
                            exposureScore = targetTree.exposureScore,
                            declineScore = newObsStress,
                            weatherScore = targetTree.weatherScore,
                            historyScore = 10.0,
                            triggerEvent = "AI Tree Doctor classified visual condition as '${result.overallVisualCondition}'",
                            dataCompletenessPercent = priorityEval.dataCompletenessPercent
                        )
                    ) + _priorityHistory.value

                    _statusBanner.value = "Saved AI Tree Doctor screening to Passport ${targetTree.treeCode}"
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Tree Doctor timed out", e)
                _treeDoctorState.value = AsyncTaskState.Error("AI screening timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Tree Doctor failed", e)
                _treeDoctorState.value = AsyncTaskState.Error(e.localizedMessage ?: "AI visual screening failed.")
            }
        }
    }

    fun runStormMemoryComparison(
        tree: TreeRecord,
        earlierBitmap: Bitmap,
        laterBitmap: Bitmap,
        earlierLabel: String,
        laterLabel: String
    ) {
        viewModelScope.launch {
            _stormMemoryState.value = AsyncTaskState.Loading
            try {
                val result = TreeGuardAiService.runStormMemoryComparison(
                    earlierBitmap = earlierBitmap,
                    laterBitmap = laterBitmap,
                    earlierLabel = earlierLabel,
                    laterLabel = laterLabel,
                    treeCode = tree.treeCode
                )
                _stormMemoryState.value = AsyncTaskState.Success(result)

                val record = StormComparison(
                    userId = currentUserId,
                    treeId = tree.id,
                    treeCode = tree.treeCode,
                    earlierLabel = earlierLabel,
                    laterLabel = laterLabel,
                    isValidComparison = result.isValidComparison,
                    anglesComparable = result.anglesComparable,
                    possibleChanges = result.possibleVisualChanges.joinToString("; ").ifBlank { "No major crown changes detected" },
                    newlyVisibleDamage = result.newlyVisibleDamage.joinToString("; ").ifBlank { "None clearly visible" },
                    unchangedFeatures = result.unchangedVisibleFeatures.joinToString("; ").ifBlank { "Primary trunk alignment preserved" },
                    limitations = "${result.lightingAndAngleNotes} ${result.comparisonLimitations}",
                    recommendedFollowUp = result.recommendedFollowUpInspection
                )
                repository.addStormComparison(record)
                _statusBanner.value = "Storm Memory comparison logged for ${tree.treeCode}"
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Storm Memory comparison timed out", e)
                _stormMemoryState.value = AsyncTaskState.Error("Storm comparison timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Storm Memory comparison failed", e)
                _stormMemoryState.value = AsyncTaskState.Error(e.localizedMessage ?: "Comparison failed.")
            }
        }
    }

    fun recordStressCalculation(
        tree: TreeRecord,
        calc: StressCalculationOutput
    ) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val record = StressRecord(
                userId = currentUserId,
                treeId = tree.id,
                date = today,
                heatIndicator = calc.heatIndicator,
                drynessIndicator = calc.drynessIndicator,
                observationIndicator = calc.observationIndicator,
                dailyStress = calc.dailyStress,
                stressDebt = calc.newAccumulatedDebt,
                deltaDays = calc.deltaDays,
                dataSource = calc.dataSourceLabel
            )
            repository.addStressRecord(record)
            if (!uiState.value.isUsingDemoFallback) {
                repository.updateTreeTelemetry(
                    treeId = tree.id,
                    visualCondition = tree.visualCondition,
                    currentPriority = tree.currentPriority,
                    currentRiskScore = tree.currentRiskScore,
                    structuralScore = tree.structuralScore,
                    exposureScore = tree.exposureScore,
                    declineScore = tree.declineScore,
                    weatherScore = tree.weatherScore,
                    historyScore = tree.historyScore,
                    stressDebt = calc.newAccumulatedDebt,
                    previousStressDebt = calc.previousDebt,
                    heatContribution = calc.heatIndicator,
                    drynessContribution = calc.drynessIndicator,
                    observationContribution = calc.observationIndicator,
                    notes = tree.notes
                )
            }
            _statusBanner.value = "Recorded Stress Debt TD_t = ${calc.newAccumulatedDebt} for ${tree.treeCode}"
        }
    }

    fun updateTreeRiskFactors(
        tree: TreeRecord,
        structuralScore: Double,
        exposureScore: Double,
        declineScore: Double,
        weatherScore: Double,
        historyScore: Double
    ) {
        viewModelScope.launch {
            val breakdown = TreeGuardEngines.evaluateInspectionPriority(
                structuralScore = structuralScore.takeIf { it >= 0.0 },
                exposureScore = exposureScore.takeIf { it >= 0.0 },
                declineScore = declineScore.takeIf { it >= 0.0 },
                weatherScore = weatherScore.takeIf { it >= 0.0 },
                historyScore = historyScore.takeIf { it >= 0.0 },
                visualCondition = tree.visualCondition,
                soilMoistureAvailable = _weather.value?.soilMoisture != null
            )
            if (uiState.value.isUsingDemoFallback) {
                repository.createTree(
                    tree.copy(
                        currentPriority = breakdown.category,
                        currentRiskScore = breakdown.finalPriorityScore,
                        structuralScore = structuralScore,
                        exposureScore = exposureScore,
                        declineScore = declineScore,
                        weatherScore = weatherScore,
                        historyScore = historyScore
                    ),
                    tree.id
                )
            } else {
                repository.updateTreeTelemetry(
                    treeId = tree.id,
                    visualCondition = tree.visualCondition,
                    currentPriority = breakdown.category,
                    currentRiskScore = breakdown.finalPriorityScore,
                    structuralScore = structuralScore,
                    exposureScore = exposureScore,
                    declineScore = declineScore,
                    weatherScore = weatherScore,
                    historyScore = historyScore,
                    stressDebt = tree.stressDebt,
                    previousStressDebt = tree.previousStressDebt,
                    heatContribution = tree.heatContribution,
                    drynessContribution = tree.drynessContribution,
                    observationContribution = tree.observationContribution,
                    notes = tree.notes
                )
            }

            val dateTimeLabel = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            _priorityHistory.value = listOf(
                PriorityHistoryEntry(
                    id = "prio_${UUID.randomUUID().toString().take(8)}",
                    treeId = tree.id,
                    treeCode = tree.treeCode,
                    dateLabel = "$dateTimeLabel • Factor Recalculation",
                    finalScore = breakdown.finalPriorityScore,
                    category = breakdown.category,
                    structuralScore = structuralScore,
                    exposureScore = exposureScore,
                    declineScore = declineScore,
                    weatherScore = weatherScore,
                    historyScore = historyScore,
                    triggerEvent = "Updated priority factors (${breakdown.confidenceLabel})",
                    dataCompletenessPercent = breakdown.dataCompletenessPercent
                )
            ) + _priorityHistory.value

            _statusBanner.value = "Updated ${tree.treeCode} Priority Score to ${breakdown.finalPriorityScore} (${breakdown.category})"
        }
    }

    fun addManualObservationNote(
        tree: TreeRecord,
        visualCondition: String,
        symptoms: String,
        recommendedAction: String,
        requireArborist: Boolean,
        concerns: String = "",
        supportingEvidence: String = "",
        observationSeverity: Double = -1.0
    ) {
        viewModelScope.launch {
            val obs = ObservationRecord(
                userId = currentUserId,
                treeId = tree.id,
                treeCode = tree.treeCode,
                imageTag = "FIELD_INSPECTION",
                visualCondition = visualCondition,
                imageQuality = "On-site field inspection",
                possibleSpecies = tree.species,
                visibleSymptoms = symptoms.ifBlank { "Logged field walk-around check" },
                possibleConcerns = concerns.ifBlank {
                    if (requireArborist) "Escalated for certified arborist verification" else "Routine monitoring"
                },
                supportingEvidence = supportingEvidence.ifBlank { "Direct field observation by logged-in inspector" },
                recommendedAction = recommendedAction.ifBlank { "Continue scheduled monitoring" },
                limitations = "Visual field walk-around without sonic tomography.",
                professionalInspection = requireArborist,
                source = "FIELD_INSPECTION"
            )
            repository.addObservation(obs)

            // Recalculate Stress Debt & Inspection Priority from the new observation
            val newDecline = if (observationSeverity >= 0.0) {
                observationSeverity.coerceIn(0.0, 100.0)
            } else {
                when (visualCondition) {
                    "Needs inspection" -> maxOf(tree.declineScore, 78.0)
                    "Monitor" -> maxOf(tree.declineScore, 52.0)
                    "Healthy appearance" -> minOf(tree.declineScore, 22.0)
                    else -> tree.declineScore
                }
            }
            val newStructural = if (requireArborist) maxOf(tree.structuralScore, 75.0) else tree.structuralScore

            val priorityEval = TreeGuardEngines.evaluateInspectionPriority(
                structuralScore = newStructural.takeIf { it >= 0.0 },
                exposureScore = tree.exposureScore.takeIf { it >= 0.0 },
                declineScore = newDecline,
                weatherScore = tree.weatherScore.takeIf { it >= 0.0 },
                historyScore = 10.0, // freshly inspected
                visualCondition = visualCondition
            )

            val stressCalc = TreeGuardEngines.calculateStressDebt(
                previousStressDebt = tree.stressDebt,
                deltaDays = 1.0,
                heatIndicator = tree.heatContribution,
                drynessIndicator = tree.drynessContribution,
                observationIndicator = newDecline
            )

            if (uiState.value.isUsingDemoFallback) {
                repository.createTree(
                    tree.copy(
                        visualCondition = visualCondition,
                        currentPriority = priorityEval.category,
                        currentRiskScore = priorityEval.finalPriorityScore,
                        structuralScore = newStructural,
                        declineScore = newDecline,
                        historyScore = 10.0,
                        stressDebt = stressCalc.newAccumulatedDebt,
                        previousStressDebt = tree.stressDebt,
                        observationContribution = newDecline
                    ),
                    tree.id
                )
            } else {
                repository.updateTreeTelemetry(
                    treeId = tree.id,
                    visualCondition = visualCondition,
                    currentPriority = priorityEval.category,
                    currentRiskScore = priorityEval.finalPriorityScore,
                    structuralScore = newStructural,
                    exposureScore = tree.exposureScore,
                    declineScore = newDecline,
                    weatherScore = tree.weatherScore,
                    historyScore = 10.0,
                    stressDebt = stressCalc.newAccumulatedDebt,
                    previousStressDebt = tree.stressDebt,
                    heatContribution = tree.heatContribution,
                    drynessContribution = tree.drynessContribution,
                    observationContribution = newDecline,
                    notes = tree.notes
                )
            }

            val dateTimeLabel = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            _priorityHistory.value = listOf(
                PriorityHistoryEntry(
                    id = "prio_${UUID.randomUUID().toString().take(8)}",
                    treeId = tree.id,
                    treeCode = tree.treeCode,
                    dateLabel = "$dateTimeLabel • New Field Observation",
                    finalScore = priorityEval.finalPriorityScore,
                    category = priorityEval.category,
                    structuralScore = newStructural,
                    exposureScore = tree.exposureScore,
                    declineScore = newDecline,
                    weatherScore = tree.weatherScore,
                    historyScore = 10.0,
                    triggerEvent = "Field observation logged ($visualCondition): ${symptoms.ifBlank { "Walk-around check" }}",
                    dataCompletenessPercent = priorityEval.dataCompletenessPercent
                )
            ) + _priorityHistory.value

            _statusBanner.value = "Added observation to ${tree.treeCode} • Updated Priority R=${priorityEval.finalPriorityScore} (${priorityEval.category})"
        }
    }

    fun addProposedMaintenanceAction(
        tree: TreeRecord,
        actionDescription: String,
        estimatedCostInr: Double,
        expectedBenefitScore: Double
    ) {
        addMaintenanceRecord(
            tree = tree,
            action = "$actionDescription [DEMO ESTIMATE]",
            estimatedCost = estimatedCostInr,
            expectedBenefit = expectedBenefitScore,
            status = "PROPOSED",
            isDemoAssumption = true
        )
    }

    fun addMaintenanceRecord(
        tree: TreeRecord,
        action: String,
        estimatedCost: Double,
        expectedBenefit: Double,
        status: String = "PROPOSED",
        isDemoAssumption: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val newAction = MaintenanceAction(
                userId = currentUserId,
                treeId = tree.id,
                treeCode = tree.treeCode,
                action = action.ifBlank { "Arborist tree maintenance" },
                estimatedCost = estimatedCost.coerceAtLeast(10.0),
                expectedBenefit = expectedBenefit.coerceIn(5.0, 100.0),
                status = status,
                priorityScore = tree.currentRiskScore,
                isDemoAssumption = isDemoAssumption
            )
            val res = repository.addMaintenanceAction(newAction)
            res.onSuccess {
                _statusBanner.value = "Logged maintenance record for ${tree.treeCode}: $action ($status)"
                onSuccess()
            }.onFailure { err ->
                _statusBanner.value = "Failed to log maintenance record: ${err.localizedMessage}"
            }
        }
    }

    fun toggleMaintenanceStatus(action: MaintenanceAction) {
        viewModelScope.launch {
            val nextStatus = when (action.status) {
                "PROPOSED" -> "SCHEDULED"
                "SCHEDULED" -> "COMPLETED"
                else -> "PROPOSED"
            }
            if (uiState.value.isUsingDemoFallback && action.id.startsWith("demo_")) {
                repository.addMaintenanceAction(action.copy(status = nextStatus))
            } else {
                repository.updateMaintenanceActionStatus(action.id, nextStatus)
            }
            _statusBanner.value = "Updated action for ${action.treeCode} -> $nextStatus"
        }
    }

    fun runSearchGroundedQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _groundedSearchState.value = AsyncTaskState.Loading
            try {
                val res = TreeGuardAiService.querySearchGroundedIntelligence(query)
                _groundedSearchState.value = AsyncTaskState.Success(res)
            } catch (e: TimeoutCancellationException) {
                _groundedSearchState.value = AsyncTaskState.Error("Search grounding timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Grounded search failed", e)
                _groundedSearchState.value = AsyncTaskState.Error(e.localizedMessage ?: "Search grounding failed.")
            }
        }
    }

    fun fetchUrbanForestAiInsights() {
        viewModelScope.launch {
            _forestInsightsState.value = AsyncTaskState.Loading
            try {
                val insights = TreeGuardAiService.generateUrbanForestInsights(
                    trees = uiState.value.trees,
                    weather = uiState.value.weather,
                    budgetInr = uiState.value.budgetInr
                )
                _forestInsightsState.value = AsyncTaskState.Success(insights)
                _statusBanner.value = "Generated AI Urban Forest Insights via Gemini 3.8 Flash"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate urban forest insights", e)
                _forestInsightsState.value = AsyncTaskState.Error(e.localizedMessage ?: "Failed to generate AI forest insights.")
            }
        }
    }

    fun speakTextWithGeminiTts(text: String, ttsFallback: TextToSpeech?) {
        viewModelScope.launch {
            try {
                val status = TreeGuardAiService.speakArboristBriefing(text, ttsFallback)
                _statusBanner.value = status
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "TTS failed", e)
            }
        }
    }

    fun getPresetBitmap(preset: SampleTreePreset): Bitmap {
        return SampleTreeBitmaps.createSampleBitmap(preset)
    }

    companion object {
        private const val TAG = "TreeGuardVM"

        fun factory(currentUserId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                return TreeGuardViewModel(TreeGuardRepository(db), currentUserId) as T
            }
        }
    }
}
