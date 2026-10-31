package com.example.data

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

data class FactorContribution(
    val code: String,
    val label: String,
    val baseWeight: Double,
    val effectiveWeight: Double,
    val rawScore: Double?, // Normalized 0..100, or null if unknown/missing
    val weightedPoints: Double,
    val explanation: String
)

data class ExplainablePriorityBreakdown(
    val finalPriorityScore: Double,
    val category: String, // Low, Moderate, High, Critical
    val factors: List<FactorContribution>,
    val missingFactors: List<String>,
    val dataCompletenessPercent: Int,
    val confidenceLabel: String,
    val humanEscalationRequired: Boolean,
    val escalationReason: String?,
    val recommendedUrgency: String,
    val reasonsSummary: List<String>,
    val limitationsSummary: List<String>
)

data class WeatherEngineIndicators(
    val heatIndicatorHt: Double,        // 0..100 input for Stress Debt Engine (0.45 * H_t)
    val drynessIndicatorDt: Double,     // 0..100 input for Stress Debt Engine (0.35 * D_t)
    val weatherPriorityScoreW: Double,  // 0..100 input for Priority Engine (0.10 * W)
    val windHazardIndex: Double,        // 0..100 derived from windSpeed + windGusts
    val soilMoistureNote: String
)

data class StressCalculationOutput(
    val heatIndicator: Double,
    val drynessIndicator: Double,
    val observationIndicator: Double,
    val heatWeighted: Double,
    val drynessWeighted: Double,
    val observationWeighted: Double,
    val dailyStress: Double,
    val previousDebt: Double,
    val decayedPreviousDebt: Double,
    val newAccumulatedDebt: Double,
    val deltaDays: Double,
    val halfLifeDays: Double,
    val decayConstantLambda: Double,
    val dataSourceLabel: String
)

data class BudgetOptimizationResult(
    val budgetInr: Double,
    val selectedActions: List<MaintenanceAction>,
    val unfundedActions: List<MaintenanceAction>,
    val totalEstimatedExpenditure: Double,
    val remainingBudget: Double,
    val expectedAggregateBenefit: Double,
    val treesReceivingAttentionCount: Int
)

object TreeGuardEngines {

    /**
     * FEATURE 3: TREE STRESS DEBT ENGINE
     * Daily Stress: S_t = 0.45 * H_t + 0.35 * D_t + 0.20 * O_t
     * Accumulated Stress Debt: TD_t = TD_(t-1) * exp(-lambda * delta_t) + S_t
     * Default experimental half-life = 30 days -> lambda = ln(2) / 30
     */
    fun calculateStressDebt(
        previousStressDebt: Double,
        deltaDays: Double,
        heatIndicator: Double,
        drynessIndicator: Double,
        observationIndicator: Double,
        halfLifeDays: Double = 30.0,
        dataSourceLabel: String = "OPEN_METEO_REGIONAL"
    ): StressCalculationOutput {
        val safeHeat = heatIndicator.coerceIn(0.0, 100.0)
        val safeDryness = drynessIndicator.coerceIn(0.0, 100.0)
        val safeObs = observationIndicator.coerceIn(0.0, 100.0)
        val safeDelta = deltaDays.coerceAtLeast(0.0)
        val safeHalfLife = halfLifeDays.coerceAtLeast(1.0)

        val heatW = 0.45 * safeHeat
        val dryW = 0.35 * safeDryness
        val obsW = 0.20 * safeObs
        val dailyStress = round1(heatW + dryW + obsW)

        val lambda = ln(2.0) / safeHalfLife
        val decayedPrev = previousStressDebt.coerceAtLeast(0.0) * exp(-lambda * safeDelta)
        val newDebt = round1(decayedPrev + dailyStress)

        return StressCalculationOutput(
            heatIndicator = round1(safeHeat),
            drynessIndicator = round1(safeDryness),
            observationIndicator = round1(safeObs),
            heatWeighted = round1(heatW),
            drynessWeighted = round1(dryW),
            observationWeighted = round1(obsW),
            dailyStress = dailyStress,
            previousDebt = round1(previousStressDebt),
            decayedPreviousDebt = round1(decayedPrev),
            newAccumulatedDebt = newDebt,
            deltaDays = safeDelta,
            halfLifeDays = safeHalfLife,
            decayConstantLambda = lambda,
            dataSourceLabel = dataSourceLabel
        )
    }

    /**
     * Computes normalized (0-100) Heat (H_t), Dryness (D_t), and Weather Priority (W)
     * indicators from Open-Meteo metrics (temperature, humidity, precipitation, wind speed,
     * wind gusts, soil moisture).
     * Never silently substitutes zero for missing soil moisture; uses humidity/rainfall proxy when soil moisture is unavailable.
     */
    fun computeWeatherEngineIndicators(
        temperatureC: Double,
        humidityPercent: Double,
        precipitationMm: Double,
        windSpeedKmh: Double,
        windGustsKmh: Double,
        soilMoistureM3: Double?
    ): WeatherEngineIndicators {
        // 1. Heat Indicator H_t (0-100): 20°C -> 0, 34°C -> 58, 44°C -> 100
        val heatHt = ((temperatureC - 20.0) / 24.0 * 100.0).coerceIn(5.0, 100.0)

        // 2. Dryness Indicator D_t (0-100): combines relative humidity, rainfall relief, and 0-1cm soil moisture
        val humidityDryness = (100.0 - humidityPercent).coerceIn(0.0, 100.0)
        val rainRelief = (precipitationMm * 4.0).coerceIn(0.0, 40.0)

        val (drynessDt, soilNote) = if (soilMoistureM3 != null && soilMoistureM3 >= 0.0) {
            // Soil moisture in m3/m3 typically 0.05 (very dry) to 0.45 (saturated), or normalized %
            val normalizedSoilMoisture = if (soilMoistureM3 <= 1.0) {
                (soilMoistureM3 / 0.45 * 100.0).coerceIn(0.0, 100.0)
            } else {
                soilMoistureM3.coerceIn(0.0, 100.0)
            }
            val soilDryness = (100.0 - normalizedSoilMoisture).coerceIn(0.0, 100.0)
            val combined = ((0.55 * soilDryness) + (0.45 * humidityDryness) - rainRelief).coerceIn(5.0, 100.0)
            combined to "Direct Open-Meteo 0–1cm volumetric soil moisture (${String.format(java.util.Locale.US, "%.2f", soilMoistureM3)} m³/m³) integrated."
        } else {
            val proxy = (humidityDryness - rainRelief).coerceIn(10.0, 100.0)
            proxy to "Soil moisture grid unavailable for this coordinate; derived dryness from relative humidity & precipitation proxy."
        }

        // 3. Wind & Gust Mechanical Hazard (0-100): gusts > 50 km/h approach severe canopy stress
        val windHazard = (((windSpeedKmh * 0.4) + (windGustsKmh * 0.6)) / 60.0 * 100.0).coerceIn(5.0, 100.0)

        // 4. Weather Priority Factor W (0-100) for R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T
        val weatherPriorityW = ((0.40 * windHazard) + (0.35 * heatHt) + (0.25 * drynessDt)).coerceIn(5.0, 100.0)

        return WeatherEngineIndicators(
            heatIndicatorHt = round1(heatHt),
            drynessIndicatorDt = round1(drynessDt),
            weatherPriorityScoreW = round1(weatherPriorityW),
            windHazardIndex = round1(windHazard),
            soilMoistureNote = soilNote
        )
    }

    /**
     * Convenience wrapper returning (Heat H_t, Dryness D_t) from a RegionalWeatherSnapshot.
     */
    fun deriveWeatherStressIndicators(weather: RegionalWeatherSnapshot): Pair<Double, Double> {
        val indicators = computeWeatherEngineIndicators(
            temperatureC = weather.temperature,
            humidityPercent = weather.humidity,
            precipitationMm = weather.precipitation,
            windSpeedKmh = weather.windSpeed,
            windGustsKmh = weather.windGusts,
            soilMoistureM3 = weather.soilMoisture
        )
        return indicators.heatIndicatorHt to indicators.drynessIndicatorDt
    }

    /**
     * Normalizes raw domain measurements to the 0..100 scale required by the Explainable Priority Engine.
     * Negative values (< 0) indicate missing/unknown data and return null so they are excluded from weight sum.
     */
    fun normalizeFactorTo100(rawValue: Double?, minDomain: Double = 0.0, maxDomain: Double = 100.0): Double? {
        if (rawValue == null || rawValue < 0.0) return null
        if (maxDomain <= minDomain) return rawValue.coerceIn(0.0, 100.0)
        return round1(((rawValue - minDomain) / (maxDomain - minDomain) * 100.0).coerceIn(0.0, 100.0))
    }

    /**
     * FEATURE 5: EXPLAINABLE TREE INSPECTION-PRIORITY ENGINE
     * Weighted Formula: R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T
     * Normalizes each factor (S, E, D, W, T) to a 0-100 scale.
     * Missing values (< 0 or null) are treated as unknown, NOT zero; provisional score normalizes across available weights.
     * Categorizes priority into Low, Moderate, High, and Critical.
     */
    fun evaluateInspectionPriority(
        structuralScore: Double?, // S (0.35)
        exposureScore: Double?,   // E (0.25)
        declineScore: Double?,    // D (0.20)
        weatherScore: Double?,    // W (0.10)
        historyScore: Double?,    // T (0.10)
        visualCondition: String = "Monitor",
        soilMoistureAvailable: Boolean = true
    ): ExplainablePriorityBreakdown {
        val normS = normalizeFactorTo100(structuralScore)
        val normE = normalizeFactorTo100(exposureScore)
        val normD = normalizeFactorTo100(declineScore)
        val normW = normalizeFactorTo100(weatherScore)
        val normT = normalizeFactorTo100(historyScore)

        val rawFactors = listOf(
            Triple("S", "Structural Concerns", 0.35 to normS),
            Triple("E", "Public / Property Exposure", 0.25 to normE),
            Triple("D", "Visible Canopy / Trunk Decline", 0.20 to normD),
            Triple("W", "Regional Weather Stress", 0.10 to normW),
            Triple("T", "Inspection Lag / History", 0.10 to normT)
        )

        val availableWeightSum = rawFactors.sumOf { (_, _, pair) ->
            if (pair.second != null) pair.first else 0.0
        }

        val missingLabels = rawFactors.filter { it.third.second == null }.map { "${it.second} (${it.first})" }
        val completenessPercent = ((rawFactors.count { it.third.second != null } / 5.0) * 100.0).roundToInt()

        val contributions = rawFactors.map { (code, label, pair) ->
            val baseWeight = pair.first
            val rawVal = pair.second
            val effectiveWeight = if (rawVal != null && availableWeightSum > 0.0) {
                baseWeight / availableWeightSum
            } else {
                0.0
            }
            val weightedPts = if (rawVal != null) round1(rawVal * effectiveWeight) else 0.0
            val reason = when {
                rawVal == null -> "Unknown / missing data — excluded from provisional weight normalization."
                rawVal >= 75.0 -> "Critical driver (${rawVal.roundToInt()}/100): warrants prompt physical verification (+${weightedPts} pts)."
                rawVal >= 50.0 -> "Elevated factor (${rawVal.roundToInt()}/100): contributes notably to priority (+${weightedPts} pts)."
                rawVal >= 25.0 -> "Moderate factor (${rawVal.roundToInt()}/100): routine monitoring advised (+${weightedPts} pts)."
                else -> "Low factor (${rawVal.roundToInt()}/100): minimal current concern observed (+${weightedPts} pts)."
            }
            FactorContribution(
                code = code,
                label = label,
                baseWeight = baseWeight,
                effectiveWeight = round2(effectiveWeight),
                rawScore = rawVal,
                weightedPoints = weightedPts,
                explanation = reason
            )
        }

        val finalScore = if (availableWeightSum > 0.0) {
            round1(contributions.sumOf { (it.rawScore ?: 0.0) * it.effectiveWeight }.coerceIn(0.0, 100.0))
        } else {
            50.0
        }

        val category = priorityCategoryForScore(finalScore)

        // Human escalation check: keep critical observations visible even when weighted score is moderate/low
        val sVal = normS ?: -1.0
        val dVal = normD ?: -1.0
        val eVal = normE ?: -1.0
        val humanEscalation = visualCondition == "Needs inspection" ||
            sVal >= 75.0 ||
            (sVal >= 60.0 && eVal >= 75.0) ||
            dVal >= 80.0

        val escalationReason = when {
            visualCondition == "Needs inspection" && finalScore < 75.0 ->
                "Visual screening flagged 'Needs inspection' — escalated for certified arborist review regardless of aggregate weighted index."
            sVal >= 75.0 ->
                "High structural concern score (${sVal.roundToInt()}/100) near public corridor requires direct human arborist assessment."
            sVal >= 60.0 && eVal >= 75.0 ->
                "Elevated structural concern combined with high public exposure triggers mandatory safety escalation."
            dVal >= 80.0 ->
                "Severe visible decline (${dVal.roundToInt()}/100) requires on-site arborist diagnosis."
            else -> null
        }

        val confidenceLabel = when {
            completenessPercent == 100 -> "High Data Completeness (5/5 factors present; visual & regional proxy limitations still apply)"
            completenessPercent >= 80 -> "Moderate Confidence (${completenessPercent}% factor coverage — provisional normalization applied)"
            else -> "Low Confidence (${completenessPercent}% factor coverage — collect missing field observations)"
        }

        val urgency = when {
            category == "Critical" || humanEscalation -> "Immediate on-site certified arborist inspection recommended (within 24–72 hours)."
            category == "High" -> "Priority field inspection recommended within 7–14 days."
            category == "Moderate" -> "Scheduled monitoring inspection within 30–60 days."
            else -> "Routine seasonal urban forest inventory check."
        }

        val reasons = mutableListOf<String>()
        contributions.filter { it.rawScore != null }
            .sortedByDescending { it.weightedPoints }
            .forEach { factor ->
                reasons.add(
                    "${factor.code} (${factor.label}): normalized ${factor.rawScore?.roundToInt()}/100 × weight ${String.format(java.util.Locale.US, "%.2f", factor.effectiveWeight)} = +${factor.weightedPoints} pts"
                )
            }
        if (escalationReason != null) {
            reasons.add("Escalation Trigger: $escalationReason")
        }

        val limitations = mutableListOf<String>()
        if (missingLabels.isNotEmpty()) {
            limitations.add(
                "Missing factors excluded from denominator (${missingLabels.joinToString()}); weights were re-normalized across ${5 - missingLabels.size}/5 available factors."
            )
        } else {
            limitations.add("All 5 factors (S, E, D, W, T) are populated (100% factor completeness).")
        }
        if (!soilMoistureAvailable) {
            limitations.add("Soil moisture telemetry was unavailable from Open-Meteo for this coordinate; W uses relative humidity and precipitation proxy.")
        }
        limitations.add("Weather factor W reflects regional Open-Meteo grid telemetry, not a root-zone sensor at the individual tree base.")
        limitations.add("Index R ($finalScore/100) is a transparent inspection-priority ranking, NOT a biomechanical probability of tree failure.")

        return ExplainablePriorityBreakdown(
            finalPriorityScore = finalScore,
            category = category,
            factors = contributions,
            missingFactors = missingLabels,
            dataCompletenessPercent = completenessPercent,
            confidenceLabel = confidenceLabel,
            humanEscalationRequired = humanEscalation,
            escalationReason = escalationReason,
            recommendedUrgency = urgency,
            reasonsSummary = reasons,
            limitationsSummary = limitations
        )
    }

    fun priorityCategoryForScore(score: Double): String = when {
        score >= 75.0 -> "Critical"
        score >= 50.0 -> "High"
        score >= 25.0 -> "Moderate"
        else -> "Low"
    }

    /**
     * FEATURE 6: WHAT-IF MAINTENANCE OPTIMIZER
     * Uses a greedy benefit-to-cost ratio (Efficiency = Expected Benefit / Estimated Cost)
     * to select proposed/scheduled actions within the available demonstration budget.
     */
    fun optimizeMaintenanceBudget(
        actions: List<MaintenanceAction>,
        budgetInr: Double
    ): BudgetOptimizationResult {
        val safeBudget = budgetInr.coerceAtLeast(0.0)
        val candidates = actions.filter { it.status == "PROPOSED" || it.status == "SCHEDULED" }
            .sortedWith(
                compareByDescending<MaintenanceAction> { it.efficiency }
                    .thenByDescending { it.priorityScore }
            )

        val selected = mutableListOf<MaintenanceAction>()
        val unfunded = mutableListOf<MaintenanceAction>()
        var spent = 0.0

        for (item in candidates) {
            if (item.estimatedCost > 0 && spent + item.estimatedCost <= safeBudget) {
                selected.add(item)
                spent += item.estimatedCost
            } else {
                unfunded.add(item)
            }
        }

        val totalBenefit = selected.sumOf { it.expectedBenefit }
        val uniqueTrees = selected.map { it.treeId.ifBlank { it.treeCode } }.distinct().size

        return BudgetOptimizationResult(
            budgetInr = round1(safeBudget),
            selectedActions = selected,
            unfundedActions = unfunded,
            totalEstimatedExpenditure = round1(spent),
            remainingBudget = round1((safeBudget - spent).coerceAtLeast(0.0)),
            expectedAggregateBenefit = round1(totalBenefit),
            treesReceivingAttentionCount = uniqueTrees
        )
    }

    private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
    private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
