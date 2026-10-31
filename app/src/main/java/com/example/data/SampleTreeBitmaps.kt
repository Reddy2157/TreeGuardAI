package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

enum class SampleTreePreset(val id: String, val title: String, val subtitle: String) {
    BASELINE_HEALTHY_NEEM(
        "BASELINE_NEEM",
        "Baseline Healthy Neem (Pre-Monsoon)",
        "Dense symmetric green canopy, intact trunk bark (Vijayawada Ward 14)"
    ),
    POST_STORM_DAMAGED_NEEM(
        "POST_STORM_NEEM",
        "Post-Cyclone Neem (Storm Memory Target)",
        "Upper-right canopy thinning, snapped hanging limb & vertical trunk fissure"
    ),
    BARK_FISSURE_BANYAN(
        "BARK_FISSURE_BANYAN",
        "Stressed Banyan (Chlorosis & Bark Crack)",
        "Yellowing foliage patches, fungal leaf spots & deep basal bark crack"
    )
}

object SampleTreeBitmaps {

    fun createSampleBitmap(preset: SampleTreePreset, width: Int = 640, height: Int = 480): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Sky background
        val isStormy = preset == SampleTreePreset.POST_STORM_DAMAGED_NEEM
        val skyTop = if (isStormy) Color.parseColor("#4A6070") else Color.parseColor("#5CA8E6")
        val skyBottom = if (isStormy) Color.parseColor("#9BAAB5") else Color.parseColor("#CBE7FF")
        paint.shader = LinearGradient(0f, 0f, 0f, height * 0.78f, skyTop, skyBottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.78f, paint)
        paint.shader = null

        // 2. Ground & urban sidewalk
        paint.color = Color.parseColor("#4B5B45")
        canvas.drawRect(0f, height * 0.76f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.parseColor("#6E7378")
        canvas.drawRect(0f, height * 0.85f, width.toFloat(), height * 0.93f, paint)

        val cx = width * 0.5f
        val baseY = height * 0.84f

        // 3. Tree Trunk & Primary Scaffold Branches
        paint.color = Color.parseColor("#4E3629")
        paint.style = Paint.Style.FILL
        val trunkPath = Path().apply {
            moveTo(cx - 38f, baseY)
            quadTo(cx - 26f, height * 0.62f, cx - 22f, height * 0.44f)
            lineTo(cx + 22f, height * 0.44f)
            quadTo(cx + 26f, height * 0.62f, cx + 38f, baseY)
            close()
        }
        canvas.drawPath(trunkPath, paint)

        // Primary branches
        paint.strokeWidth = 18f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx - 8f, height * 0.48f, cx - 115f, height * 0.30f, paint)
        canvas.drawLine(cx, height * 0.45f, cx - 20f, height * 0.22f, paint)
        if (preset != SampleTreePreset.POST_STORM_DAMAGED_NEEM) {
            canvas.drawLine(cx + 10f, height * 0.48f, cx + 120f, height * 0.29f, paint)
        } else {
            // Snapped hanging right limb after storm!
            canvas.drawLine(cx + 10f, height * 0.48f, cx + 65f, height * 0.38f, paint)
            paint.color = Color.parseColor("#6B4C35")
            paint.strokeWidth = 14f
            canvas.drawLine(cx + 65f, height * 0.38f, cx + 135f, height * 0.62f, paint)
            // Exposed splintered wood at break point
            paint.color = Color.parseColor("#E3C598")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx + 65f, height * 0.38f, 9f, paint)
        }

        // 4. Canopy foliage clusters
        paint.style = Paint.Style.FILL
        when (preset) {
            SampleTreePreset.BASELINE_HEALTHY_NEEM -> {
                // Full, lush healthy green canopy
                val greens = listOf("#1E6B3A", "#28864B", "#34A85A", "#42BD6A")
                val clusters = listOf(
                    Triple(cx - 120f, height * 0.32f, 78f),
                    Triple(cx - 65f, height * 0.22f, 88f),
                    Triple(cx + 10f, height * 0.20f, 94f),
                    Triple(cx + 85f, height * 0.25f, 84f),
                    Triple(cx + 125f, height * 0.34f, 74f),
                    Triple(cx - 35f, height * 0.34f, 82f),
                    Triple(cx + 45f, height * 0.34f, 82f)
                )
                clusters.forEachIndexed { idx, (x, y, r) ->
                    paint.color = Color.parseColor(greens[idx % greens.size])
                    canvas.drawCircle(x, y, r, paint)
                }
            }

            SampleTreePreset.POST_STORM_DAMAGED_NEEM -> {
                // Noticeable upper-right canopy thinning + fallen branch foliage + trunk fissure
                val leftClusters = listOf(
                    Triple(cx - 120f, height * 0.32f, 75f),
                    Triple(cx - 65f, height * 0.23f, 84f),
                    Triple(cx - 10f, height * 0.24f, 72f),
                    Triple(cx - 35f, height * 0.35f, 76f)
                )
                leftClusters.forEachIndexed { idx, (x, y, r) ->
                    paint.color = Color.parseColor(if (idx % 2 == 0) "#236E3E" else "#2F8B50")
                    canvas.drawCircle(x, y, r, paint)
                }
                // Sparse wilting foliage on hanging snapped branch
                paint.color = Color.parseColor("#6E7D3B")
                canvas.drawCircle(cx + 135f, height * 0.63f, 44f, paint)
                canvas.drawCircle(cx + 105f, height * 0.56f, 36f, paint)

                // Visible vertical bark crack / fissure on trunk
                paint.color = Color.parseColor("#1C110B")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                val crack = Path().apply {
                    moveTo(cx + 4f, height * 0.49f)
                    lineTo(cx - 3f, height * 0.58f)
                    lineTo(cx + 6f, height * 0.67f)
                    lineTo(cx + 1f, height * 0.76f)
                }
                canvas.drawPath(crack, paint)
            }

            SampleTreePreset.BARK_FISSURE_BANYAN -> {
                // Chlorotic yellowing canopy + dark necrotic spots + basal trunk wound
                val stressedColors = listOf("#3B7A47", "#7D9B3A", "#B8A832", "#4B834E", "#9E8F2E")
                val clusters = listOf(
                    Triple(cx - 115f, height * 0.33f, 76f),
                    Triple(cx - 55f, height * 0.23f, 85f),
                    Triple(cx + 15f, height * 0.22f, 88f),
                    Triple(cx + 95f, height * 0.30f, 78f),
                    Triple(cx, height * 0.35f, 80f)
                )
                clusters.forEachIndexed { idx, (x, y, r) ->
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor(stressedColors[idx % stressedColors.size])
                    canvas.drawCircle(x, y, r, paint)
                }
                // Leaf spots
                paint.color = Color.parseColor("#4A2F18")
                for (i in 0..14) {
                    val sx = cx - 90f + (i * 14f)
                    val sy = height * 0.24f + ((i % 4) * 22f)
                    canvas.drawCircle(sx, sy, 5f, paint)
                }
                // Basal bark cavity / wound
                paint.color = Color.parseColor("#1A0F0A")
                canvas.drawOval(RectF(cx - 16f, height * 0.62f, cx + 14f, height * 0.79f), paint)
            }
        }

        return bmp
    }

    fun getTreeBitmap(treeCode: String, species: String = "", visualCondition: String = ""): Bitmap {
        val preset = when {
            treeCode.contains("0001") || visualCondition.equals("Needs inspection", ignoreCase = true) ->
                SampleTreePreset.POST_STORM_DAMAGED_NEEM
            treeCode.contains("0002") || species.contains("Banyan", ignoreCase = true) || visualCondition.equals("Monitor", ignoreCase = true) ->
                SampleTreePreset.BARK_FISSURE_BANYAN
            else ->
                SampleTreePreset.BASELINE_HEALTHY_NEEM
        }
        return createSampleBitmap(preset, width = 320, height = 240)
    }
}
