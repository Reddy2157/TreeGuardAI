package com.example

import com.example.data.DemoDataFactory
import com.example.data.TreeGuardEngines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun explainablePriorityEngine_weightedFormulaAndCategories_areAccurate() {
        // Formula: R = 0.35S + 0.25E + 0.20D + 0.10W + 0.10T
        // S=80 (28.0), E=60 (15.0), D=50 (10.0), W=40 (4.0), T=30 (3.0) => R = 60.0 (High)
        val result = TreeGuardEngines.evaluateInspectionPriority(
            structuralScore = 80.0,
            exposureScore = 60.0,
            declineScore = 50.0,
            weatherScore = 40.0,
            historyScore = 30.0,
            visualCondition = "Monitor",
            soilMoistureAvailable = true
        )

        assertEquals(60.0, result.finalPriorityScore, 0.01)
        assertEquals("High", result.category)
        assertEquals(100, result.dataCompletenessPercent)
        assertTrue(result.missingFactors.isEmpty())
        assertTrue(result.reasonsSummary.isNotEmpty())
        assertTrue(result.limitationsSummary.isNotEmpty())

        // Verify category boundaries: Low (<25), Moderate (25..49.9), High (50..74.9), Critical (>=75)
        assertEquals("Low", TreeGuardEngines.priorityCategoryForScore(18.0))
        assertEquals("Moderate", TreeGuardEngines.priorityCategoryForScore(38.5))
        assertEquals("High", TreeGuardEngines.priorityCategoryForScore(61.0))
        assertEquals("Critical", TreeGuardEngines.priorityCategoryForScore(81.5))
    }

    @Test
    fun explainablePriorityEngine_missingFactorNormalization_excludesUnknowns() {
        // When W and T are unknown (null), available weight sum is 0.35 + 0.25 + 0.20 = 0.80
        val result = TreeGuardEngines.evaluateInspectionPriority(
            structuralScore = 80.0,
            exposureScore = 80.0,
            declineScore = 80.0,
            weatherScore = null,
            historyScore = null,
            visualCondition = "Needs inspection",
            soilMoistureAvailable = false
        )

        assertEquals(80.0, result.finalPriorityScore, 0.1)
        assertEquals("Critical", result.category)
        assertEquals(60, result.dataCompletenessPercent)
        assertEquals(2, result.missingFactors.size)
        assertTrue(result.humanEscalationRequired)
    }

    @Test
    fun weatherIntelligence_derivesStressDebtAndPriorityInputs() {
        val withSoil = TreeGuardEngines.computeWeatherEngineIndicators(
            temperatureC = 36.0,
            humidityPercent = 52.0,
            precipitationMm = 0.0,
            windSpeedKmh = 18.0,
            windGustsKmh = 38.0,
            soilMoistureM3 = 0.18
        )
        assertTrue(withSoil.heatIndicatorHt in 5.0..100.0)
        assertTrue(withSoil.drynessIndicatorDt in 5.0..100.0)
        assertTrue(withSoil.weatherPriorityScoreW in 5.0..100.0)

        val withoutSoil = TreeGuardEngines.computeWeatherEngineIndicators(
            temperatureC = 36.0,
            humidityPercent = 52.0,
            precipitationMm = 0.0,
            windSpeedKmh = 18.0,
            windGustsKmh = 38.0,
            soilMoistureM3 = null
        )
        assertTrue(withoutSoil.soilMoistureNote.contains("proxy", ignoreCase = true))
    }

    @Test
    fun digitalTreePassport_seedRecordsAndHistories_arePopulated() {
        val trees = DemoDataFactory.defaultVijayawadaTrees("test_uid")
        val obs = DemoDataFactory.defaultObservations("test_uid")
        val stress = DemoDataFactory.defaultStressHistory("test_uid")
        val prioHistory = DemoDataFactory.defaultPriorityHistory()
        val maint = DemoDataFactory.defaultMaintenanceActions("test_uid")

        assertFalse(trees.isEmpty())
        assertEquals("TG-VJA-0001", trees.first().treeCode)
        assertTrue(obs.any { it.treeCode == "TG-VJA-0001" })
        assertTrue(stress.any { it.treeId == "demo_tree_0001" })
        assertTrue(prioHistory.any { it.treeCode == "TG-VJA-0001" })
        assertTrue(maint.any { it.treeCode == "TG-VJA-0001" })
    }
}
