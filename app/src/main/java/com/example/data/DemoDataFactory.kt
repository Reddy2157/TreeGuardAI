package com.example.data

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Provides clearly labelled DEMO DATA for Vijayawada, Andhra Pradesh, India
 * so the application works out-of-the-box and can seed Firestore on user request.
 */
object DemoDataFactory {

    fun defaultVijayawadaTrees(userId: String = "demo_user"): List<TreeRecord> {
        val now = Timestamp(Date())
        return listOf(
            TreeRecord(
                id = "demo_tree_0001",
                userId = userId,
                treeCode = "TG-VJA-0001",
                species = "Azadirachta indica (Neem)",
                latitude = 16.5062,
                longitude = 80.6480,
                zone = "MG Road, Benz Circle [DEMO DATA]",
                registrationType = "DEMO_SEED",
                visualCondition = "Needs inspection",
                currentPriority = "Critical",
                currentRiskScore = 81.5,
                structuralScore = 85.0,
                exposureScore = 90.0,
                declineScore = 72.0,
                weatherScore = 68.0,
                historyScore = 70.0,
                stressDebt = 74.6,
                previousStressDebt = 56.2,
                heatContribution = 72.0,
                drynessContribution = 64.0,
                observationContribution = 80.0,
                notes = "[DEMO DATA] Post-monsoon gust damage: partially snapped secondary branch overhanging pedestrian bus bay and vertical trunk bark fissure.",
                createdAt = now,
                updatedAt = now
            ),
            TreeRecord(
                id = "demo_tree_0002",
                userId = userId,
                treeCode = "TG-VJA-0002",
                species = "Ficus benghalensis (Banyan)",
                latitude = 16.5150,
                longitude = 80.6320,
                zone = "Swaraj Maidan, Rythu Bazaar [DEMO DATA]",
                registrationType = "DEMO_SEED",
                visualCondition = "Monitor",
                currentPriority = "High",
                currentRiskScore = 61.0,
                structuralScore = 58.0,
                exposureScore = 78.0,
                declineScore = 60.0,
                weatherScore = 62.0,
                historyScore = 40.0,
                stressDebt = 58.4,
                previousStressDebt = 44.0,
                heatContribution = 70.0,
                drynessContribution = 62.0,
                observationContribution = 55.0,
                notes = "[DEMO DATA] Aerial root compaction near paved walkway; upper eastern crown shows mild chlorosis and leaf spotting.",
                createdAt = now,
                updatedAt = now
            ),
            TreeRecord(
                id = "demo_tree_0003",
                userId = userId,
                treeCode = "TG-VJA-0003",
                species = "Pongamia pinnata (Kanuga)",
                latitude = 16.5095,
                longitude = 80.6175,
                zone = "Prakasam Barrage Riverfront [DEMO DATA]",
                registrationType = "DEMO_SEED",
                visualCondition = "Monitor",
                currentPriority = "Moderate",
                currentRiskScore = 38.5,
                structuralScore = 30.0,
                exposureScore = 55.0,
                declineScore = 35.0,
                weatherScore = 48.0,
                historyScore = 25.0,
                stressDebt = 39.2,
                previousStressDebt = 31.8,
                heatContribution = 64.0,
                drynessContribution = 45.0,
                observationContribution = 30.0,
                notes = "[DEMO DATA] Minor windward twig dieback facing Krishna river breeze; main trunk flare intact.",
                createdAt = now,
                updatedAt = now
            ),
            TreeRecord(
                id = "demo_tree_0004",
                userId = userId,
                treeCode = "TG-VJA-0004",
                species = "Ficus religiosa (Peepal)",
                latitude = 16.4975,
                longitude = 80.6540,
                zone = "Auto Nagar Green Belt [DEMO DATA]",
                registrationType = "DEMO_SEED",
                visualCondition = "Healthy appearance",
                currentPriority = "Low",
                currentRiskScore = 18.0,
                structuralScore = 12.0,
                exposureScore = 30.0,
                declineScore = 10.0,
                weatherScore = 35.0,
                historyScore = 10.0,
                stressDebt = 24.5,
                previousStressDebt = 21.0,
                heatContribution = 66.0,
                drynessContribution = 50.0,
                observationContribution = 10.0,
                notes = "[DEMO DATA] Dense healthy foliage, balanced scaffold branching, no visible trunk cracks.",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun defaultObservations(userId: String = "demo_user"): List<ObservationRecord> {
        val now = Timestamp(Date())
        return listOf(
            ObservationRecord(
                id = "demo_obs_0001",
                userId = userId,
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                imageTag = "POST_STORM_NEEM",
                visualCondition = "Needs inspection",
                imageQuality = "Clear street-level daylight photograph",
                possibleSpecies = "Azadirachta indica (Neem)",
                visibleSymptoms = "Upper-right canopy thinning (~30% foliar loss), snapped hanging secondary branch, vertical bark fissure on main trunk.",
                possibleConcerns = "Potential hanging limb detachment hazard over walkway; bark fissure warrants structural inspection by a certified arborist.",
                supportingEvidence = "Visible splintered wood at secondary scaffold junction and dark vertical split along central trunk bark.",
                recommendedAction = "Immediate on-site inspection by a qualified municipal arborist and safe removal of confirmed hanging deadwood.",
                limitations = "[DEMO DATA] Single-angle visual screening cannot assess internal heartwood decay or subsurface root stability.",
                professionalInspection = true,
                source = "AI_TREE_DOCTOR",
                observedAt = now,
                createdAt = now
            ),
            ObservationRecord(
                id = "demo_obs_0001_baseline",
                userId = userId,
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                imageTag = "BASELINE_NEEM",
                visualCondition = "Healthy appearance",
                imageQuality = "Pre-monsoon baseline registration photo",
                possibleSpecies = "Azadirachta indica (Neem)",
                visibleSymptoms = "Full symmetric Neem canopy prior to October cyclone gusts; minor surface bark weathering only.",
                possibleConcerns = "High public exposure adjacent to MG Road bus bay requires seasonal monitoring.",
                supportingEvidence = "Continuous green foliar crown and intact secondary scaffold branches in May baseline photograph.",
                recommendedAction = "Routine pre-monsoon crown inspection.",
                limitations = "[DEMO DATA] Baseline registration record.",
                professionalInspection = false,
                source = "FIELD_INSPECTION",
                observedAt = now,
                createdAt = now
            ),
            ObservationRecord(
                id = "demo_obs_0002",
                userId = userId,
                treeId = "demo_tree_0002",
                treeCode = "TG-VJA-0002",
                imageTag = "BARK_FISSURE_BANYAN",
                visualCondition = "Monitor",
                imageQuality = "Clear daylight framing",
                possibleSpecies = "Ficus benghalensis (Banyan)",
                visibleSymptoms = "Patchy interveinal yellowing (chlorosis) in mid-canopy and dark necrotic leaf spots; superficial basal bark abrasion.",
                possibleConcerns = "Possible localized soil compaction / moisture deficit around paved root zone and minor foliar fungal spotting.",
                supportingEvidence = "Yellow-tinted foliage clusters contrasted against healthy green upper crown.",
                recommendedAction = "Schedule soil aeration & mulching assessment within 30 days; monitor basal bark wound.",
                limitations = "[DEMO DATA] Foliar discoloration requires physical leaf/soil sampling for definitive diagnosis.",
                professionalInspection = false,
                source = "AI_TREE_DOCTOR",
                observedAt = now,
                createdAt = now
            ),
            ObservationRecord(
                id = "demo_obs_0003",
                userId = userId,
                treeId = "demo_tree_0003",
                treeCode = "TG-VJA-0003",
                imageTag = "CHLOROSIS_KANUGA",
                visualCondition = "Monitor",
                imageQuality = "Clear riverfront daylight framing",
                possibleSpecies = "Pongamia pinnata (Kanuga)",
                visibleSymptoms = "Mild windward tip dieback on river-facing branches; main stem bark healthy.",
                possibleConcerns = "Moderate wind exposure along Prakasam Barrage embankment.",
                supportingEvidence = "Sparse outer twigs on western aspect; healthy inner canopy.",
                recommendedAction = "Routine 60-day monitoring and light dead-twig pruning.",
                limitations = "[DEMO DATA] Visual field screening only.",
                professionalInspection = false,
                source = "FIELD_INSPECTION",
                observedAt = now,
                createdAt = now
            ),
            ObservationRecord(
                id = "demo_obs_0004",
                userId = userId,
                treeId = "demo_tree_0004",
                treeCode = "TG-VJA-0004",
                imageTag = "BASELINE_NEEM",
                visualCondition = "Healthy appearance",
                imageQuality = "High-contrast morning capture",
                possibleSpecies = "Ficus religiosa (Peepal)",
                visibleSymptoms = "Vigorous foliage, balanced primary leader, intact root flare.",
                possibleConcerns = "None currently observed.",
                supportingEvidence = "Uniform chlorophyll density and zero visible bark lesions.",
                recommendedAction = "Continue annual municipal inventory check.",
                limitations = "[DEMO DATA] Visual screening only.",
                professionalInspection = false,
                source = "AI_TREE_DOCTOR",
                observedAt = now,
                createdAt = now
            )
        )
    }

    fun defaultStressHistory(userId: String = "demo_user"): List<StressRecord> {
        val now = Timestamp(Date())
        return listOf(
            StressRecord(
                id = "demo_stress_1",
                userId = userId,
                treeId = "demo_tree_0001",
                date = "2026-10-01",
                heatIndicator = 62.0,
                drynessIndicator = 55.0,
                observationIndicator = 35.0,
                dailyStress = 54.2,
                stressDebt = 54.2,
                deltaDays = 7.0,
                dataSource = "OPEN_METEO_REGIONAL",
                createdAt = now
            ),
            StressRecord(
                id = "demo_stress_2",
                userId = userId,
                treeId = "demo_tree_0001",
                date = "2026-10-10",
                heatIndicator = 68.0,
                drynessIndicator = 60.0,
                observationIndicator = 50.0,
                dailyStress = 61.6,
                stressDebt = 64.8,
                deltaDays = 9.0,
                dataSource = "OPEN_METEO_REGIONAL",
                createdAt = now
            ),
            StressRecord(
                id = "demo_stress_3",
                userId = userId,
                treeId = "demo_tree_0001",
                date = "2026-10-20",
                heatIndicator = 74.0,
                drynessIndicator = 66.0,
                observationIndicator = 75.0,
                dailyStress = 71.4,
                stressDebt = 72.1,
                deltaDays = 10.0,
                dataSource = "OPEN_METEO_REGIONAL",
                createdAt = now
            ),
            StressRecord(
                id = "demo_stress_4",
                userId = userId,
                treeId = "demo_tree_0001",
                date = "2026-10-30",
                heatIndicator = 72.0,
                drynessIndicator = 64.0,
                observationIndicator = 80.0,
                dailyStress = 70.8,
                stressDebt = 74.6,
                deltaDays = 10.0,
                dataSource = "OPEN_METEO_REGIONAL",
                createdAt = now
            ),
            StressRecord(
                id = "demo_stress_5",
                userId = userId,
                treeId = "demo_tree_0002",
                date = "2026-10-15",
                heatIndicator = 69.0,
                drynessIndicator = 60.0,
                observationIndicator = 45.0,
                dailyStress = 61.1,
                stressDebt = 51.2,
                deltaDays = 10.0,
                dataSource = "OPEN_METEO_REGIONAL",
                createdAt = now
            ),
            StressRecord(
                id = "demo_stress_6",
                userId = userId,
                treeId = "demo_tree_0002",
                date = "2026-10-30",
                heatIndicator = 70.0,
                drynessIndicator = 62.0,
                observationIndicator = 55.0,
                dailyStress = 64.2,
                stressDebt = 58.4,
                deltaDays = 15.0,
                dataSource = "OPEN_METEO_REGIONAL",
                createdAt = now
            )
        )
    }

    fun defaultPriorityHistory(): List<PriorityHistoryEntry> {
        return listOf(
            PriorityHistoryEntry(
                id = "prio_hist_1",
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                dateLabel = "2026-05-14 • Baseline Registration",
                finalScore = 39.8,
                category = "Moderate",
                structuralScore = 25.0,
                exposureScore = 90.0,
                declineScore = 15.0,
                weatherScore = 40.0,
                historyScore = 15.0,
                triggerEvent = "Initial Digital Passport registration on MG Road (high corridor exposure E=90).",
                dataCompletenessPercent = 100
            ),
            PriorityHistoryEntry(
                id = "prio_hist_2",
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                dateLabel = "2026-10-12 • Heatwave & Wind Gust Update",
                finalScore = 54.6,
                category = "High",
                structuralScore = 45.0,
                exposureScore = 90.0,
                declineScore = 42.0,
                weatherScore = 64.0,
                historyScore = 45.0,
                triggerEvent = "Open-Meteo regional wind gusts (38 km/h) and thermal stress elevated W and S.",
                dataCompletenessPercent = 100
            ),
            PriorityHistoryEntry(
                id = "prio_hist_3",
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                dateLabel = "2026-10-30 • Post-Storm AI Screening",
                finalScore = 81.5,
                category = "Critical",
                structuralScore = 85.0,
                exposureScore = 90.0,
                declineScore = 72.0,
                weatherScore = 68.0,
                historyScore = 70.0,
                triggerEvent = "AI Tree Doctor & Storm Memory identified snapped hanging limb and trunk bark fissure (S=85, D=72).",
                dataCompletenessPercent = 100
            ),
            PriorityHistoryEntry(
                id = "prio_hist_4",
                treeId = "demo_tree_0002",
                treeCode = "TG-VJA-0002",
                dateLabel = "2026-08-20 • Baseline Registration",
                finalScore = 44.5,
                category = "Moderate",
                structuralScore = 40.0,
                exposureScore = 78.0,
                declineScore = 35.0,
                weatherScore = 40.0,
                historyScore = 20.0,
                triggerEvent = "Initial registration at Swaraj Maidan Rythu Bazaar.",
                dataCompletenessPercent = 100
            ),
            PriorityHistoryEntry(
                id = "prio_hist_5",
                treeId = "demo_tree_0002",
                treeCode = "TG-VJA-0002",
                dateLabel = "2026-10-29 • Chlorosis & Root Compaction Log",
                finalScore = 61.0,
                category = "High",
                structuralScore = 58.0,
                exposureScore = 78.0,
                declineScore = 60.0,
                weatherScore = 62.0,
                historyScore = 40.0,
                triggerEvent = "AI screening flagged interveinal chlorosis and root-zone soil compaction (D=60, S=58).",
                dataCompletenessPercent = 100
            )
        )
    }

    fun defaultMaintenanceActions(userId: String = "demo_user"): List<MaintenanceAction> {
        val now = Timestamp(Date())
        return listOf(
            MaintenanceAction(
                id = "demo_maint_1",
                userId = userId,
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                action = "Certified arborist structural inspection & removal of confirmed hanging dead branch [DEMO ESTIMATE]",
                estimatedCost = 3200.0,
                expectedBenefit = 94.0,
                status = "PROPOSED",
                priorityScore = 81.5,
                isDemoAssumption = true,
                createdAt = now,
                updatedAt = now
            ),
            MaintenanceAction(
                id = "demo_maint_2",
                userId = userId,
                treeId = "demo_tree_0002",
                treeCode = "TG-VJA-0002",
                action = "Root-zone soil decompaction, organic mulching & supplemental irrigation check [DEMO ESTIMATE]",
                estimatedCost = 2400.0,
                expectedBenefit = 78.0,
                status = "PROPOSED",
                priorityScore = 61.0,
                isDemoAssumption = true,
                createdAt = now,
                updatedAt = now
            ),
            MaintenanceAction(
                id = "demo_maint_3",
                userId = userId,
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                action = "Resistograph / sonic trunk crack depth assessment by municipal tree officer [DEMO ESTIMATE]",
                estimatedCost = 2800.0,
                expectedBenefit = 82.0,
                status = "PROPOSED",
                priorityScore = 81.5,
                isDemoAssumption = true,
                createdAt = now,
                updatedAt = now
            ),
            MaintenanceAction(
                id = "demo_maint_4",
                userId = userId,
                treeId = "demo_tree_0003",
                treeCode = "TG-VJA-0003",
                action = "Riverfront windward crown deadwood pruning & follow-up visual monitoring [DEMO ESTIMATE]",
                estimatedCost = 1800.0,
                expectedBenefit = 62.0,
                status = "PROPOSED",
                priorityScore = 38.5,
                isDemoAssumption = true,
                createdAt = now,
                updatedAt = now
            ),
            MaintenanceAction(
                id = "demo_maint_5",
                userId = userId,
                treeId = "demo_tree_0004",
                treeCode = "TG-VJA-0004",
                action = "Routine seasonal bio-fertilizer application & reflective trunk guard [DEMO ESTIMATE]",
                estimatedCost = 1500.0,
                expectedBenefit = 38.0,
                status = "PROPOSED",
                priorityScore = 18.0,
                isDemoAssumption = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun defaultStormComparisons(userId: String = "demo_user"): List<StormComparison> {
        val now = Timestamp(Date())
        return listOf(
            StormComparison(
                id = "demo_storm_1",
                userId = userId,
                treeId = "demo_tree_0001",
                treeCode = "TG-VJA-0001",
                earlierLabel = "Pre-Monsoon Baseline (May 2026) [DEMO]",
                laterLabel = "Post-Cyclone Inspection (Oct 2026) [DEMO]",
                isValidComparison = true,
                anglesComparable = true,
                possibleChanges = "Noticeable loss of upper-right foliar density (~30% thinning) and change in secondary branch posture.",
                newlyVisibleDamage = "Freshly snapped hanging secondary limb on right scaffold and newly visible vertical bark fissure on central trunk.",
                unchangedFeatures = "Left-side primary scaffold limb and basal root flare orientation remain visually similar.",
                limitations = "Overcast post-storm lighting vs sunny baseline; 2D photographs cannot determine internal crack depth or root plate stability.",
                recommendedFollowUp = "Immediate on-site inspection by a certified arborist to cordon pedestrian bay and safely clear the hanging branch.",
                createdAt = now
            )
        )
    }
}
