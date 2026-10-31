package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TreeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val treeCode: String = "",
    val species: String = "",
    val latitude: Double = 16.5062,
    val longitude: Double = 80.6480,
    val zone: String = "",
    val registrationType: String = "LIVE_GPS", // LIVE_GPS, MANUAL, DEMO_SEED
    val visualCondition: String = "Healthy appearance", // Healthy appearance, Monitor, Needs inspection, Unclear
    val currentPriority: String = "Low", // Low, Moderate, High, Critical
    val currentRiskScore: Double = 0.0,
    val structuralScore: Double = 20.0, // -1.0 if unknown
    val exposureScore: Double = 50.0,   // -1.0 if unknown
    val declineScore: Double = 15.0,    // -1.0 if unknown
    val weatherScore: Double = 30.0,    // -1.0 if unknown
    val historyScore: Double = 20.0,    // -1.0 if unknown
    val stressDebt: Double = 18.0,
    val previousStressDebt: Double = 12.0,
    val heatContribution: Double = 45.0,
    val drynessContribution: Double = 40.0,
    val observationContribution: Double = 15.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ObservationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val treeId: String = "",
    val treeCode: String = "",
    val imageTag: String = "BASELINE_NEEM",
    val visualCondition: String = "Healthy appearance",
    val imageQuality: String = "Clear daylight framing",
    val possibleSpecies: String = "",
    val visibleSymptoms: String = "",
    val possibleConcerns: String = "",
    val supportingEvidence: String = "",
    val recommendedAction: String = "",
    val limitations: String = "",
    val professionalInspection: Boolean = false,
    val source: String = "AI_TREE_DOCTOR", // AI_TREE_DOCTOR, FIELD_INSPECTION, DEMO_SEED
    @ServerTimestamp val observedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StressRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val treeId: String = "",
    val date: String = "",
    val heatIndicator: Double = 0.0,
    val drynessIndicator: Double = 0.0,
    val observationIndicator: Double = 0.0,
    val dailyStress: Double = 0.0,
    val stressDebt: Double = 0.0,
    val deltaDays: Double = 1.0,
    val dataSource: String = "OPEN_METEO_REGIONAL", // OPEN_METEO_REGIONAL, ESTIMATED_SIMULATED, DEMO_SEED
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MaintenanceAction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val treeId: String = "",
    val treeCode: String = "",
    val action: String = "",
    val estimatedCost: Double = 1500.0,
    val expectedBenefit: Double = 75.0,
    val status: String = "PROPOSED", // PROPOSED, SCHEDULED, COMPLETED, DEFERRED
    val priorityScore: Double = 50.0,
    val isDemoAssumption: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val efficiency: Double
        get() = if (estimatedCost > 0.0) (expectedBenefit / estimatedCost) * 1000.0 else 0.0
}

@IgnoreExtraProperties
data class WeatherRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val zone: String = "Vijayawada Urban Core",
    val latitude: Double = 16.5062,
    val longitude: Double = 80.6480,
    val temperature: Double = 34.2,
    val humidity: Double = 62.0,
    val windSpeed: Double = 14.5,
    val windGusts: Double = 28.0,
    val precipitation: Double = 0.0,
    val soilMoisture: Double = 24.0, // -1.0 if unavailable
    val dataSource: String = "Open-Meteo Regional API",
    @ServerTimestamp val recordedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StormComparison(
    @DocumentId val id: String = "",
    val userId: String = "",
    val treeId: String = "",
    val treeCode: String = "",
    val earlierLabel: String = "",
    val laterLabel: String = "",
    val isValidComparison: Boolean = true,
    val anglesComparable: Boolean = true,
    val possibleChanges: String = "",
    val newlyVisibleDamage: String = "",
    val unchangedFeatures: String = "",
    val limitations: String = "",
    val recommendedFollowUp: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class PriorityHistoryEntry(
    val id: String,
    val treeId: String,
    val treeCode: String,
    val dateLabel: String,
    val finalScore: Double,
    val category: String, // Low, Moderate, High, Critical
    val structuralScore: Double,
    val exposureScore: Double,
    val declineScore: Double,
    val weatherScore: Double,
    val historyScore: Double,
    val triggerEvent: String,
    val dataCompletenessPercent: Int = 100
)

data class DailyForecastItem(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val precipitationSum: Double,
    val maxWindGusts: Double,
    val estimatedDailyHeatStress: Double = 55.0,
    val estimatedDailyWeatherRisk: Double = 45.0
)

data class RegionalWeatherSnapshot(
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val temperature: Double,
    val humidity: Double,
    val precipitation: Double,
    val windSpeed: Double,
    val windGusts: Double,
    val soilMoisture: Double?, // null if not reported by station/grid
    val isLiveApi: Boolean,
    val fetchedAtLabel: String,
    val forecast7Day: List<DailyForecastItem>,
    val cacheStatus: String = "LIVE_API", // LIVE_API, CACHED_RECENT, CACHED_FALLBACK, DEMO_FALLBACK
    val cachedLocationsCount: Int = 1,
    val lastErrorMessage: String? = null,
    val derivedHeatIndicator: Double = 60.0,
    val derivedDrynessIndicator: Double = 52.0,
    val derivedWeatherPriorityScore: Double = 48.0
)

data class TreeDoctorScreeningResult(
    val isTreeVisible: Boolean,
    val imageQuality: String,
    val possibleSpecies: String,
    val visibleSymptoms: List<String>,
    val possibleConcerns: List<String>,
    val overallVisualCondition: String, // Healthy appearance, Monitor, Needs inspection, Unclear
    val supportingVisualEvidence: String,
    val recommendedNextSteps: List<String>,
    val professionalInspectionRecommended: Boolean,
    val limitations: String
)

data class StormMemoryComparisonResult(
    val isValidComparison: Boolean,
    val anglesComparable: Boolean,
    val lightingAndAngleNotes: String,
    val possibleVisualChanges: List<String>,
    val newlyVisibleDamage: List<String>,
    val unchangedVisibleFeatures: List<String>,
    val comparisonLimitations: String,
    val recommendedFollowUpInspection: String
)

data class GroundedSpeciesIntelligence(
    val queryTopic: String,
    val summaryMarkdown: String,
    val sourceTitlesAndUrls: List<Pair<String, String>>
)

data class UrbanForestAiInsights(
    val title: String,
    val canopyHealthAssessment: String,
    val keyRiskObservations: List<String>,
    val climaticStressInterpretation: String,
    val maintenancePrioritizationAdvice: String,
    val preventativeRecommendations: List<String>,
    val limitationsDisclaimer: String
)

