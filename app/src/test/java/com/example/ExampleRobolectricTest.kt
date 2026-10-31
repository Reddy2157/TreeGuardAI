package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DemoDataFactory
import com.example.data.SampleTreeBitmaps
import com.example.data.SampleTreePreset
import com.example.data.TreeGuardEngines
import com.example.ui.TreeGuardSection
import com.example.ui.priorityToGoogleMarkerHue
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TreeGuard AI", appName)
    }

    @Test
    fun `navigation shell defines Command Centre, Map, Scan, and Intelligence sections`() {
        val sections = TreeGuardSection.entries
        assertEquals(4, sections.size)
        assertEquals("Command Centre", sections[0].title)
        assertEquals("Map", sections[1].title)
        assertEquals("Scan", sections[2].title)
        assertEquals("Intelligence", sections[3].title)
    }

    @Test
    fun `google maps marker hue maps tree inspection priority categories accurately`() {
        assertEquals(BitmapDescriptorFactory.HUE_RED, priorityToGoogleMarkerHue("Critical"), 0.01f)
        assertEquals(BitmapDescriptorFactory.HUE_ORANGE, priorityToGoogleMarkerHue("High"), 0.01f)
        assertEquals(BitmapDescriptorFactory.HUE_YELLOW, priorityToGoogleMarkerHue("Moderate"), 0.01f)
        assertEquals(BitmapDescriptorFactory.HUE_GREEN, priorityToGoogleMarkerHue("Low"), 0.01f)
    }

    @Test
    fun `sample tree preset bitmaps generate valid non-empty bitmaps for AI screening and Storm Memory`() {
        SampleTreePreset.entries.forEach { preset ->
            val bmp = SampleTreeBitmaps.createSampleBitmap(preset)
            assertNotNull(bmp)
            assertTrue(bmp.width > 0 && bmp.height > 0)
        }
    }

    @Test
    fun `demo data factory creates labeled Vijayawada trees and evaluates priority`() {
        val trees = DemoDataFactory.defaultVijayawadaTrees("test-user")
        assertEquals(4, trees.size)
        assertTrue(trees.all { it.registrationType == "DEMO_SEED" })
        val firstTree = trees.first()
        val breakdown = TreeGuardEngines.evaluateInspectionPriority(
            structuralScore = firstTree.structuralScore,
            exposureScore = firstTree.exposureScore,
            declineScore = firstTree.declineScore,
            weatherScore = firstTree.weatherScore,
            historyScore = firstTree.historyScore,
            visualCondition = firstTree.visualCondition,
            soilMoistureAvailable = true
        )
        assertEquals(100, breakdown.dataCompletenessPercent)
        assertTrue(breakdown.finalPriorityScore in 0.0..100.0)
    }
}
