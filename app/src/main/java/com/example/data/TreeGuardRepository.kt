package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TreeGuardRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- 1. TREES ---

    fun observeTrees(userId: String = requireUserId()): Flow<List<TreeRecord>> = flow {
        val path = "trees"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TreeRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.currentRiskScore }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getTreeById(treeId: String): Result<TreeRecord> = runCatching {
        val docRef = db.collection("trees").document(treeId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(TreeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw NoSuchElementException("Tree $treeId not found")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createTree(tree: TreeRecord, customId: String? = null): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = customId ?: "tree_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("trees").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "treeCode" to tree.treeCode,
            "species" to tree.species,
            "latitude" to tree.latitude,
            "longitude" to tree.longitude,
            "zone" to tree.zone,
            "registrationType" to tree.registrationType,
            "visualCondition" to tree.visualCondition,
            "currentPriority" to tree.currentPriority,
            "currentRiskScore" to tree.currentRiskScore,
            "structuralScore" to tree.structuralScore,
            "exposureScore" to tree.exposureScore,
            "declineScore" to tree.declineScore,
            "weatherScore" to tree.weatherScore,
            "historyScore" to tree.historyScore,
            "stressDebt" to tree.stressDebt,
            "previousStressDebt" to tree.previousStressDebt,
            "heatContribution" to tree.heatContribution,
            "drynessContribution" to tree.drynessContribution,
            "observationContribution" to tree.observationContribution,
            "notes" to tree.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTreeTelemetry(
        treeId: String,
        visualCondition: String,
        currentPriority: String,
        currentRiskScore: Double,
        structuralScore: Double,
        exposureScore: Double,
        declineScore: Double,
        weatherScore: Double,
        historyScore: Double,
        stressDebt: Double,
        previousStressDebt: Double,
        heatContribution: Double,
        drynessContribution: Double,
        observationContribution: Double,
        notes: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("trees").document(treeId)
        val updates = mapOf(
            "visualCondition" to visualCondition,
            "currentPriority" to currentPriority,
            "currentRiskScore" to currentRiskScore,
            "structuralScore" to structuralScore,
            "exposureScore" to exposureScore,
            "declineScore" to declineScore,
            "weatherScore" to weatherScore,
            "historyScore" to historyScore,
            "stressDebt" to stressDebt,
            "previousStressDebt" to previousStressDebt,
            "heatContribution" to heatContribution,
            "drynessContribution" to drynessContribution,
            "observationContribution" to observationContribution,
            "notes" to notes.take(2000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 2. OBSERVATIONS ---

    fun observeObservations(userId: String = requireUserId()): Flow<List<ObservationRecord>> = flow {
        val path = "observations"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ObservationRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.observedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addObservation(obs: ObservationRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "obs_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("observations").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "treeId" to obs.treeId,
            "treeCode" to obs.treeCode,
            "imageTag" to obs.imageTag.take(500),
            "visualCondition" to obs.visualCondition,
            "imageQuality" to obs.imageQuality.take(200),
            "possibleSpecies" to obs.possibleSpecies.take(200),
            "visibleSymptoms" to obs.visibleSymptoms.take(2000),
            "possibleConcerns" to obs.possibleConcerns.take(2000),
            "supportingEvidence" to obs.supportingEvidence.take(2000),
            "recommendedAction" to obs.recommendedAction.take(2000),
            "limitations" to obs.limitations.take(2000),
            "professionalInspection" to obs.professionalInspection,
            "source" to obs.source,
            "observedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- 3. STRESS RECORDS ---

    fun observeStressRecords(userId: String = requireUserId()): Flow<List<StressRecord>> = flow {
        val path = "stress_records"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StressRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addStressRecord(record: StressRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val safeDate = record.date.replace("[^a-zA-Z0-9_\\-]".toRegex(), "-")
        val docId = "stress_${record.treeId}_$safeDate".take(120)
        val docRef = db.collection("stress_records").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "treeId" to record.treeId,
            "date" to record.date,
            "heatIndicator" to record.heatIndicator,
            "drynessIndicator" to record.drynessIndicator,
            "observationIndicator" to record.observationIndicator,
            "dailyStress" to record.dailyStress,
            "stressDebt" to record.stressDebt,
            "deltaDays" to record.deltaDays,
            "dataSource" to record.dataSource,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- 4. MAINTENANCE ACTIONS ---

    fun observeMaintenanceActions(userId: String = requireUserId()): Flow<List<MaintenanceAction>> = flow {
        val path = "maintenance_actions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MaintenanceAction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.priorityScore }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addMaintenanceAction(action: MaintenanceAction): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "maint_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("maintenance_actions").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "treeId" to action.treeId,
            "treeCode" to action.treeCode,
            "action" to action.action.take(500),
            "estimatedCost" to action.estimatedCost.coerceAtLeast(1.0),
            "expectedBenefit" to action.expectedBenefit.coerceIn(0.0, 100.0),
            "status" to action.status,
            "priorityScore" to action.priorityScore.coerceIn(0.0, 100.0),
            "isDemoAssumption" to action.isDemoAssumption,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateMaintenanceActionStatus(actionId: String, newStatus: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("maintenance_actions").document(actionId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- 5. STORM COMPARISONS ---

    fun observeStormComparisons(userId: String = requireUserId()): Flow<List<StormComparison>> = flow {
        val path = "storm_comparisons"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StormComparison::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addStormComparison(comp: StormComparison): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "storm_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("storm_comparisons").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "treeId" to comp.treeId,
            "treeCode" to comp.treeCode,
            "earlierLabel" to comp.earlierLabel.take(200),
            "laterLabel" to comp.laterLabel.take(200),
            "isValidComparison" to comp.isValidComparison,
            "anglesComparable" to comp.anglesComparable,
            "possibleChanges" to comp.possibleChanges.take(2000),
            "newlyVisibleDamage" to comp.newlyVisibleDamage.take(2000),
            "unchangedFeatures" to comp.unchangedFeatures.take(2000),
            "limitations" to comp.limitations.take(2000),
            "recommendedFollowUp" to comp.recommendedFollowUp.take(2000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- 6. WEATHER RECORDS ---

    suspend fun addWeatherRecord(record: WeatherRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "weather_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("weather_records").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "zone" to record.zone.take(200),
            "latitude" to record.latitude.coerceIn(-90.0, 90.0),
            "longitude" to record.longitude.coerceIn(-180.0, 180.0),
            "temperature" to record.temperature.coerceIn(-60.0, 70.0),
            "humidity" to record.humidity.coerceIn(0.0, 100.0),
            "windSpeed" to record.windSpeed.coerceIn(0.0, 300.0),
            "windGusts" to record.windGusts.coerceIn(0.0, 400.0),
            "precipitation" to record.precipitation.coerceIn(0.0, 2000.0),
            "soilMoisture" to record.soilMoisture.coerceIn(-1.0, 100.0),
            "dataSource" to record.dataSource.take(200),
            "recordedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
