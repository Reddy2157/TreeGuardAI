package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TreeGuardRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleTree(code: String = "TG-VJA-0001") = TreeRecord(
        treeCode = code,
        species = "Azadirachta indica (Neem)",
        latitude = 16.5062,
        longitude = 80.6480,
        zone = "MG Road, Benz Circle, Vijayawada",
        registrationType = "LIVE_GPS",
        visualCondition = "Monitor",
        currentPriority = "High",
        currentRiskScore = 62.5,
        structuralScore = 60.0,
        exposureScore = 80.0,
        declineScore = 55.0,
        weatherScore = 50.0,
        historyScore = 50.0,
        stressDebt = 48.2,
        previousStressDebt = 32.0,
        heatContribution = 68.0,
        drynessContribution = 54.0,
        observationContribution = 55.0,
        notes = "Canopy thinning observed on eastern crown."
    )

    @Test
    fun createAndObserveTree_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = TreeGuardRepository(firestore)
        val customId = "tree_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createTree(sampleTree(), customId)
        }
        assertTrue(createRes.isSuccess)
        assertEquals(customId, createRes.getOrThrow())

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTrees(uid).first { list -> list.any { it.id == customId } }
        }
        assertTrue(emitted.any { it.id == customId && it.treeCode == "TG-VJA-0001" })
    }

    @Test
    fun getTreeById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = TreeGuardRepository(firestore)
        val customId = "tree_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val treeId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTree(sampleTree(), customId).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = TreeGuardRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getTreeById(treeId)
        }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeTrees_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = TreeGuardRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTrees("unauthenticated_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@treeguard.org"
        const val BOB_EMAIL = "bob@treeguard.org"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
