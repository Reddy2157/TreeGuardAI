package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metadata record for a tree photo stored locally on device and referenced in Firestore.
 */
data class StoredTreeImage(
    val treeCode: String,
    val fileName: String,
    val filePath: String,
    val latitude: Double,
    val longitude: Double,
    val captureSource: String, // "CAMERAX_CAPTURE", "GALLERY_PHOTO", "FIELD_CAMERA", "BOTANICAL_PRESET"
    val timestamp: Long,
    val dateLabel: String,
    val resolution: String,
    val visualCondition: String = "Healthy appearance"
) {
    val id: String get() = fileName
    val bitmap: Bitmap?
        get() = try {
            BitmapFactory.decodeFile(filePath)
        } catch (_: Exception) {
            null
        }
}

/**
 * Persistent local image storage and in-memory cache for urban tree photographs.
 *
 * Ensures tree photographs captured via CameraX, Photo Picker, or field inspections
 * are preserved across app lifecycles, mapped by [treeCode], and rendered immediately
 * in Command Centre gallery, Tree Passports, and Map inspection overlays.
 */
object TreeImageStorage {
    private const val TAG = "TreeImageStorage"
    private const val DIRECTORY_NAME = "tree_photos"
    private const val PREFS_NAME = "tree_image_metadata"

    // 32MB in-memory LRU cache for instant Compose image rendering
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    private fun getPhotoDirectory(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Persists a tree photograph to disk and caches it in memory.
     * Records GPS coordinates and capture telemetry.
     */
    fun saveTreePhoto(
        context: Context,
        treeCode: String,
        bitmap: Bitmap,
        latitude: Double,
        longitude: Double,
        captureSource: String = "CAMERAX_CAPTURE",
        visualCondition: String = "Healthy appearance"
    ): StoredTreeImage? {
        return try {
            val dir = getPhotoDirectory(context)
            val timestamp = System.currentTimeMillis()
            val safeTreeCode = treeCode.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
            val fileName = "IMG_${safeTreeCode}_${timestamp}.jpg"
            val file = File(dir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Save also as primary latest photo for fast retrieval
            val primaryFile = File(dir, "${safeTreeCode}_latest.jpg")
            FileOutputStream(primaryFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Update in-memory cache
            memoryCache.put(fileName, bitmap)
            memoryCache.put(treeCode, bitmap)
            memoryCache.put("${safeTreeCode}_latest", bitmap)

            val dateLabel = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestamp))
            val resolution = "${bitmap.width}x${bitmap.height}"

            // Persist metadata
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val metadataLine = "$fileName|$latitude|$longitude|$captureSource|$timestamp|$dateLabel|$resolution|$visualCondition"

            prefs.edit().putString("latest_$treeCode", metadataLine).apply()

            val existingLines = prefs.getString("all_$treeCode", "") ?: ""
            val updatedLines = if (existingLines.isBlank()) metadataLine else "$existingLines\n$metadataLine"
            prefs.edit().putString("all_$treeCode", updatedLines).apply()

            StoredTreeImage(
                treeCode = treeCode,
                fileName = fileName,
                filePath = file.absolutePath,
                latitude = latitude,
                longitude = longitude,
                captureSource = captureSource,
                timestamp = timestamp,
                dateLabel = dateLabel,
                resolution = resolution,
                visualCondition = visualCondition
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save tree photo for $treeCode", e)
            null
        }
    }

    /**
     * Retrieves the latest stored photograph for the specified tree code.
     * Returns null if no user photo exists yet.
     */
    fun getTreeImage(context: Context, treeCode: String): Bitmap? {
        // 1. Check memory cache
        memoryCache.get(treeCode)?.let { return it }
        val safeTreeCode = treeCode.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
        memoryCache.get("${safeTreeCode}_latest")?.let { return it }

        // 2. Check disk for primary file
        val dir = getPhotoDirectory(context)
        val primaryFile = File(dir, "${safeTreeCode}_latest.jpg")
        if (primaryFile.exists()) {
            try {
                val bmp = BitmapFactory.decodeFile(primaryFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(treeCode, bmp)
                    return bmp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed decoding primary image for $treeCode", e)
            }
        }

        // 3. Fallback: Check newest file matching prefix
        val matching = dir.listFiles { _, name -> name.startsWith("IMG_${safeTreeCode}_") }
            ?.sortedByDescending { it.lastModified() }
            ?.firstOrNull()
        if (matching != null && matching.exists()) {
            try {
                val bmp = BitmapFactory.decodeFile(matching.absolutePath)
                if (bmp != null) {
                    memoryCache.put(treeCode, bmp)
                    return bmp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed decoding matching image for $treeCode", e)
            }
        }

        return null
    }

    /**
     * Checks if a user or system photo is stored for [treeCode].
     */
    fun hasStoredImage(context: Context, treeCode: String): Boolean {
        if (memoryCache.get(treeCode) != null) return true
        val safeTreeCode = treeCode.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
        val dir = getPhotoDirectory(context)
        val primaryFile = File(dir, "${safeTreeCode}_latest.jpg")
        if (primaryFile.exists()) return true
        val matching = dir.listFiles { _, name -> name.startsWith("IMG_${safeTreeCode}_") }
        return matching != null && matching.isNotEmpty()
    }

    /**
     * Retrieves the metadata of the latest stored photograph for [treeCode].
     */
    fun getStoredImageMetadata(context: Context, treeCode: String): StoredTreeImage? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("latest_$treeCode", null) ?: return null
        val parts = raw.split("|")
        if (parts.size < 7) return null
        val fileName = parts[0]
        val lat = parts.getOrNull(1)?.toDoubleOrNull() ?: 16.5062
        val lon = parts.getOrNull(2)?.toDoubleOrNull() ?: 80.6480
        val source = parts.getOrNull(3) ?: "CAMERAX_CAPTURE"
        val ts = parts.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()
        val dateLabel = parts.getOrNull(5) ?: ""
        val res = parts.getOrNull(6) ?: "1280x960"
        val condition = parts.getOrNull(7) ?: "Healthy appearance"
        val file = File(getPhotoDirectory(context), fileName)

        return StoredTreeImage(
            treeCode = treeCode,
            fileName = fileName,
            filePath = file.absolutePath,
            latitude = lat,
            longitude = lon,
            captureSource = source,
            timestamp = ts,
            dateLabel = dateLabel,
            resolution = res,
            visualCondition = condition
        )
    }

    /**
     * Retrieves all chronological stored photographs for [treeCode].
     */
    fun getAllPhotosForTree(context: Context, treeCode: String): List<StoredTreeImage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawList = prefs.getString("all_$treeCode", "") ?: ""
        if (rawList.isBlank()) return emptyList()

        return rawList.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 7) {
                val fileName = parts[0]
                val lat = parts.getOrNull(1)?.toDoubleOrNull() ?: 16.5062
                val lon = parts.getOrNull(2)?.toDoubleOrNull() ?: 80.6480
                val source = parts.getOrNull(3) ?: "CAMERAX_CAPTURE"
                val ts = parts.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()
                val dateLabel = parts.getOrNull(5) ?: ""
                val res = parts.getOrNull(6) ?: "1280x960"
                val condition = parts.getOrNull(7) ?: "Healthy appearance"
                val file = File(getPhotoDirectory(context), fileName)
                StoredTreeImage(
                    treeCode = treeCode,
                    fileName = fileName,
                    filePath = file.absolutePath,
                    latitude = lat,
                    longitude = lon,
                    captureSource = source,
                    timestamp = ts,
                    dateLabel = dateLabel,
                    resolution = res,
                    visualCondition = condition
                )
            } else null
        }.reversed()
    }

    /**
     * Seeds initial botanical photo plates for baseline inventory if not yet written,
     * ensuring the app starts with high-resolution visual plates for TG-VJA-0001, 0002, and 0003.
     */
    fun seedDefaultDemoPhotosIfEmpty(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSeeded = prefs.getBoolean("demo_photos_seeded", false)
        if (isSeeded) return

        try {
            // Seed TG-VJA-0001 (Post-Storm Neem)
            val bmp0001 = SampleTreeBitmaps.createSampleBitmap(SampleTreePreset.POST_STORM_DAMAGED_NEEM)
            saveTreePhoto(
                context = context,
                treeCode = "TG-VJA-0001",
                bitmap = bmp0001,
                latitude = 16.5062,
                longitude = 80.6480,
                captureSource = "BOTANICAL_PRESET",
                visualCondition = "Needs inspection"
            )

            // Seed TG-VJA-0002 (Stressed Banyan)
            val bmp0002 = SampleTreeBitmaps.createSampleBitmap(SampleTreePreset.BARK_FISSURE_BANYAN)
            saveTreePhoto(
                context = context,
                treeCode = "TG-VJA-0002",
                bitmap = bmp0002,
                latitude = 16.5125,
                longitude = 80.6288,
                captureSource = "BOTANICAL_PRESET",
                visualCondition = "Monitor"
            )

            // Seed TG-VJA-0003 (Healthy Neem)
            val bmp0003 = SampleTreeBitmaps.createSampleBitmap(SampleTreePreset.BASELINE_HEALTHY_NEEM)
            saveTreePhoto(
                context = context,
                treeCode = "TG-VJA-0003",
                bitmap = bmp0003,
                latitude = 16.5180,
                longitude = 80.6550,
                captureSource = "BOTANICAL_PRESET",
                visualCondition = "Healthy appearance"
            )

            prefs.edit().putBoolean("demo_photos_seeded", true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding default demo photos", e)
        }
    }
}
