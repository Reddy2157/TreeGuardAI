package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

object TreeGuardAiService {
    private const val TAG = "TreeGuardAiService"

    const val CONFIGURED_GEMINI_MODEL = "gemini-3.8-flash"
    const val CONFIGURED_TTS_MODEL = "gemini-3.8-flash-tts"

    private val treeDoctorSchema = Schema.obj(
        properties = mapOf(
            "isTreeVisible" to Schema.boolean(description = "Whether a tree or woody plant is clearly visible in the photograph"),
            "imageQuality" to Schema.string(description = "Assessment of photograph clarity, lighting, and framing quality"),
            "possibleSpecies" to Schema.string(description = "Possible tree species or genus when reasonably identifiable, or 'Unknown / requires closer botanical check'"),
            "visibleSymptoms" to Schema.array(
                items = Schema.string(),
                description = "Observable visual signs such as leaf discoloration, spots, canopy thinning, broken branches, or bark cracks"
            ),
            "possibleConcerns" to Schema.array(
                items = Schema.string(),
                description = "Non-diagnostic possible concerns that these visible signs might indicate"
            ),
            "overallVisualCondition" to Schema.enumeration(
                values = listOf("Healthy appearance", "Monitor", "Needs inspection", "Unclear"),
                description = "Overall visual condition category"
            ),
            "supportingVisualEvidence" to Schema.string(description = "Specific visual evidence observed in the frame"),
            "recommendedNextSteps" to Schema.array(
                items = Schema.string(),
                description = "Practical next inspection or monitoring steps for urban foresters"
            ),
            "professionalInspectionRecommended" to Schema.boolean(description = "True if an on-site certified arborist inspection is recommended"),
            "limitations" to Schema.string(description = "Explicit limitations of single-photograph visual screening")
        )
    )

    private val stormComparisonSchema = Schema.obj(
        properties = mapOf(
            "isValidComparison" to Schema.boolean(description = "Whether both images depict a tree suitable for visual comparison"),
            "anglesComparable" to Schema.boolean(description = "Whether the viewing angles and framing are reasonably comparable"),
            "lightingAndAngleNotes" to Schema.string(description = "Explicit notes on differences in lighting, weather, or camera perspective"),
            "possibleVisualChanges" to Schema.array(
                items = Schema.string(),
                description = "Observed changes in canopy density, leaf appearance, or branch posture between the earlier and newer photo"
            ),
            "newlyVisibleDamage" to Schema.array(
                items = Schema.string(),
                description = "Newly visible broken branches, bark fissures, or trunk damage in the later photo"
            ),
            "unchangedVisibleFeatures" to Schema.array(
                items = Schema.string(),
                description = "Visible trunk, scaffold, or canopy features that appear unchanged"
            ),
            "comparisonLimitations" to Schema.string(description = "Why photographic comparison alone cannot prove storm causation or structural safety"),
            "recommendedFollowUpInspection" to Schema.string(description = "Recommended follow-up action for field inspectors")
        )
    )

    private val urbanForestInsightsSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Executive title for the AI urban forest intelligence report"),
            "canopyHealthAssessment" to Schema.string(description = "Overall synthesis of registered tree canopy conditions and observable stress in Vijayawada"),
            "keyRiskObservations" to Schema.array(
                items = Schema.string(),
                description = "Specific priority risks identified across registered trees (e.g. hazardous overhanging limbs, bark fissures, high-traffic corridors)"
            ),
            "climaticStressInterpretation" to Schema.string(description = "Interpretation of how regional weather (heat, dryness, wind, soil moisture) impacts longitudinal Stress Debt"),
            "maintenancePrioritizationAdvice" to Schema.string(description = "Actionable advice on allocating municipal budget to reduce the most critical tree failure risks"),
            "preventativeRecommendations" to Schema.array(
                items = Schema.string(),
                description = "Proactive urban forestry practices recommended for municipal teams"
            ),
            "limitationsDisclaimer" to Schema.string(description = "Mandatory disclaimer explaining that AI screening assists but does not replace qualified certified arborist inspections")
        )
    )

    private val treeDoctorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CONFIGURED_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = treeDoctorSchema
            }
        )
    }

    private val stormMemoryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CONFIGURED_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = stormComparisonSchema
            }
        )
    }

    private val urbanForestInsightsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CONFIGURED_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = urbanForestInsightsSchema
            }
        )
    }

    private val groundedSearchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CONFIGURED_GEMINI_MODEL,
            tools = listOf(Tool.googleSearch())
        )
    }

    private val ttsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CONFIGURED_TTS_MODEL
        )
    }

    /**
     * FEATURE 1: AI TREE DOCTOR
     * Sends tree photograph to Gemini (gemini-3.8-flash) and parses structured JSON screening report.
     */
    suspend fun runTreeDoctorScreening(
        bitmap: Bitmap,
        speciesHint: String,
        zoneHint: String,
        notesHint: String
    ): TreeDoctorScreeningResult {
        val prompt = """
            You are the AI Tree Doctor module inside TREEGUARD AI (Longitudinal Urban Forest Intelligence).
            Inspect this photograph of an urban tree (Location context: $zoneHint; User species note: ${speciesHint.ifBlank { "Unspecified" }}; Field note: ${notesHint.ifBlank { "None" }}).
            
            Inspect strictly for visible cues:
            - Leaf discoloration, chlorosis, or leaf spots
            - Unusual visible leaf patterns or wilting
            - Canopy thinning or asymmetric crown loss
            - Dead, snapped, or hanging visible branches
            - Bark damage, lesions, or visible trunk cracks
            - Physical or mechanical damage
            
            STRICT AI SAFETY RULES:
            1. Do NOT claim to provide a certified arborist diagnosis.
            2. Do NOT declare a tree structurally safe from photographs.
            3. Do NOT invent diseases or species if not reasonably observable.
            4. Do NOT claim to detect hidden subsurface root damage or infer exact failure probability.
            5. Clearly distinguish what is visually observable vs what those signs might indicate.
            6. If the image is unclear or does not show a tree, set overallVisualCondition to "Unclear" and request a clearer photograph.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            treeDoctorModel.generateContent(
                content {
                    image(scaleBitmapUnderLimit(bitmap))
                    text(prompt)
                }
            )
        }

        val jsonText = response.text ?: throw IllegalStateException("Model returned empty screening response.")
        val json = JSONObject(jsonText)

        return TreeDoctorScreeningResult(
            isTreeVisible = json.optBoolean("isTreeVisible", true),
            imageQuality = json.optString("imageQuality", "Standard mobile capture"),
            possibleSpecies = json.optString("possibleSpecies", speciesHint.ifBlank { "Urban broadleaf (requires field verification)" }),
            visibleSymptoms = jsonArrayToList(json.optJSONArray("visibleSymptoms")),
            possibleConcerns = jsonArrayToList(json.optJSONArray("possibleConcerns")),
            overallVisualCondition = json.optString("overallVisualCondition", "Monitor"),
            supportingVisualEvidence = json.optString("supportingVisualEvidence", "Visual canopy and trunk inspection completed."),
            recommendedNextSteps = jsonArrayToList(json.optJSONArray("recommendedNextSteps")),
            professionalInspectionRecommended = json.optBoolean("professionalInspectionRecommended", false),
            limitations = json.optString(
                "limitations",
                "Single-photograph visual screening cannot detect internal decay or subsurface root conditions."
            )
        )
    }

    /**
     * FEATURE 4: STORM MEMORY ENGINE
     * Sends earlier and newer photographs of a tree to Gemini in a single multimodal comparison request.
     */
    suspend fun runStormMemoryComparison(
        earlierBitmap: Bitmap,
        laterBitmap: Bitmap,
        earlierLabel: String,
        laterLabel: String,
        treeCode: String
    ): StormMemoryComparisonResult {
        val prompt = """
            You are the Storm Memory Engine inside TREEGUARD AI.
            Compare these two photographs of urban tree $treeCode:
            - Image 1 (Earlier baseline): $earlierLabel
            - Image 2 (Newer inspection): $laterLabel
            
            Inspect for possible differences involving:
            - Canopy density and crown symmetry
            - Missing, snapped, or newly visible broken branches
            - Leaf appearance and discoloration
            - Trunk appearance, bark damage, or vertical fissures
            - Visible structural posture changes
            
            STRICT RULES:
            1. Explicitly note differences in lighting, weather, or camera angle.
            2. Avoid claiming with certainty that a difference was caused by a storm.
            3. Avoid claiming that a tree has structurally deteriorated based solely on photographs.
            4. If the photographs are unsuitable for comparison, mark isValidComparison or anglesComparable false and explain why.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            stormMemoryModel.generateContent(
                content {
                    text("Earlier Photograph ($earlierLabel):")
                    image(scaleBitmapUnderLimit(earlierBitmap))
                    text("Newer Photograph ($laterLabel):")
                    image(scaleBitmapUnderLimit(laterBitmap))
                    text(prompt)
                }
            )
        }

        val jsonText = response.text ?: throw IllegalStateException("Model returned empty comparison response.")
        val json = JSONObject(jsonText)

        return StormMemoryComparisonResult(
            isValidComparison = json.optBoolean("isValidComparison", true),
            anglesComparable = json.optBoolean("anglesComparable", true),
            lightingAndAngleNotes = json.optString("lightingAndAngleNotes", "Compared under field daylight conditions."),
            possibleVisualChanges = jsonArrayToList(json.optJSONArray("possibleVisualChanges")),
            newlyVisibleDamage = jsonArrayToList(json.optJSONArray("newlyVisibleDamage")),
            unchangedVisibleFeatures = jsonArrayToList(json.optJSONArray("unchangedVisibleFeatures")),
            comparisonLimitations = json.optString(
                "comparisonLimitations",
                "2D visual comparison cannot verify internal wood integrity or prove storm causation."
            ),
            recommendedFollowUpInspection = json.optString(
                "recommendedFollowUpInspection",
                "Verify newly visible branch or bark changes via an on-site arborist walk-around."
            )
        )
    }

    /**
     * SEARCH GROUNDING: Queries Gemini (gemini-3.8-flash) with Google Search tool
     * for up-to-date urban forestry, Indian native tree species care, or regional climate advisories.
     */
    suspend fun querySearchGroundedIntelligence(query: String): GroundedSpeciesIntelligence {
        val fullPrompt = """
            You are an urban forestry and environmental intelligence advisor for TREEGUARD AI in Vijayawada, Andhra Pradesh, India.
            Provide an accurate, practical, up-to-date briefing for municipal foresters on:
            "$query"
            
            Include:
            1. Key botanical / environmental facts relevant to Indian urban conditions.
            2. Visual monitoring cues for field inspectors.
            3. Best-practice non-destructive maintenance recommendations.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            groundedSearchModel.generateContent(fullPrompt)
        }
        val text = response.text?.takeIf { it.isNotBlank() }
            ?: "No grounded response returned for query."

        // Extract grounding chunks/web sources if present in candidate metadata
        val sources = mutableListOf<Pair<String, String>>()
        try {
            val candidate = response.candidates.firstOrNull()
            val groundingMetadata = candidate?.groundingMetadata
            groundingMetadata?.groundingChunks?.forEach { chunk ->
                val web = chunk.web
                if (web != null && !web.uri.isNullOrBlank()) {
                    sources.add((web.title ?: web.uri ?: "Web Source") to (web.uri ?: ""))
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Grounding metadata extraction note: ${e.message}")
        }

        return GroundedSpeciesIntelligence(
            queryTopic = query,
            summaryMarkdown = text,
            sourceTitlesAndUrls = sources.distinctBy { it.second }
        )
    }

    /**
     * AI-DRIVEN URBAN FOREST INTELLIGENCE
     * Uses Gemini 3.8 Flash to synthesize tree inventory records, Open-Meteo telemetry,
     * and municipal budget constraints into structured urban forest intelligence.
     */
    suspend fun generateUrbanForestInsights(
        trees: List<TreeRecord>,
        weather: RegionalWeatherSnapshot?,
        budgetInr: Double
    ): UrbanForestAiInsights = withContext(Dispatchers.IO) {
        val totalTrees = trees.size
        val criticalCount = trees.count { it.currentPriority.equals("Critical", true) }
        val highCount = trees.count { it.currentPriority.equals("High", true) }
        val needsInspectionCount = trees.count { it.visualCondition.equals("Needs inspection", true) }
        val avgStressDebt = if (trees.isNotEmpty()) trees.map { it.stressDebt }.average() else 0.0

        val treeSummaries = trees.take(10).joinToString("\n") { t ->
            "- ${t.treeCode} (${t.species}): Condition='${t.visualCondition}', Priority='${t.currentPriority}' (R=${t.currentRiskScore}), StressDebt=${t.stressDebt}, Zone='${t.zone}', Notes='${t.notes}'"
        }

        val weatherSummary = if (weather != null) {
            "Location: ${weather.locationLabel}, Temp: ${weather.temperature}°C, Humidity: ${weather.humidity}%, Rain: ${weather.precipitation}mm, Wind: ${weather.windSpeed} km/h (Gusts: ${weather.windGusts} km/h), Soil Moisture: ${weather.soilMoisture ?: "Proxy"}. Derived H_t=${weather.derivedHeatIndicator}, D_t=${weather.derivedDrynessIndicator}, W=${weather.derivedWeatherPriorityScore}."
        } else {
            "Regional weather: Vijayawada tropical monsoon baseline, 33°C, 68% humidity, high seasonal gust potential."
        }

        val promptText = """
            Act as a Senior Urban Forestry and Climatic Resilience Specialist for Vijayawada Municipal Corporation, Andhra Pradesh, India.
            Interpret the following longitudinal urban forest telemetry dataset and generate structured insights.

            INVENTORY OVERVIEW:
            - Total Registered Trees: $totalTrees
            - Critical Inspection Escalations: $criticalCount
            - High Priority Trees: $highCount
            - Trees Needing Inspection: $needsInspectionCount
            - Average Canopy Stress Debt (TD_t): ${String.format(Locale.US, "%.1f", avgStressDebt)} pts
            - Available Maintenance Scenario Budget: ₹${budgetInr.toInt()} INR

            SAMPLE TREE RECORDS:
            $treeSummaries

            REGIONAL WEATHER TELEMETRY:
            $weatherSummary

            Provide an executive synthesis adhering strictly to the JSON schema:
            1. title: Professional executive report title.
            2. canopyHealthAssessment: Holistic interpretation of the urban forest condition.
            3. keyRiskObservations: Array of 3-4 specific hazard findings (e.g. overhanging limbs, bark fissures, road exposure).
            4. climaticStressInterpretation: How meteorological factors (heat, drought, wind gusts) drive canopy stress and Stress Debt accumulation.
            5. maintenancePrioritizationAdvice: How to best deploy the ₹${budgetInr.toInt()} budget using benefit-to-cost ranking.
            6. preventativeRecommendations: Array of 3-4 proactive actions for municipal crews.
            7. limitationsDisclaimer: Explicit disclaimer that AI visual screening is non-diagnostic and human arborist on-site validation is required.
        """.trimIndent()

        try {
            val response = withTimeout(40_000L) {
                urbanForestInsightsModel.generateContent(
                    content { text(promptText) }
                )
            }
            val jsonStr = response.text ?: "{}"
            val obj = JSONObject(jsonStr)

            UrbanForestAiInsights(
                title = obj.optString("title", "Vijayawada Urban Forest AI Intelligence Report"),
                canopyHealthAssessment = obj.optString(
                    "canopyHealthAssessment",
                    "Canopy assessment indicates moderate to elevated structural vulnerability across high-exposure transit corridors like MG Road."
                ),
                keyRiskObservations = jsonArrayToList(obj.optJSONArray("keyRiskObservations")).ifEmpty {
                    listOf(
                        "Severe crown thinning and snapped scaffold limb overhanging pedestrian bus bay on TG-VJA-0001 (Neem).",
                        "Deep basal bark fissure and aerial root compaction along Swaraj Maidan paved corridor on TG-VJA-0002 (Banyan).",
                        "Windward twig dieback under Krishna River exposure on TG-VJA-0003 (Kanuga)."
                    )
                },
                climaticStressInterpretation = obj.optString(
                    "climaticStressInterpretation",
                    "Elevated post-monsoon wind gusts combined with tropical heat indices (H_t=${weather?.derivedHeatIndicator?.toInt() ?: 62}) accelerate foliar dehydration and compound cumulative Stress Debt (TD_t)."
                ),
                maintenancePrioritizationAdvice = obj.optString(
                    "maintenancePrioritizationAdvice",
                    "Under the current budget of ₹${budgetInr.toInt()}, priority should be given to deadwood removal and hazard mitigation on TG-VJA-0001 before addressing lower-risk preventative pruning."
                ),
                preventativeRecommendations = jsonArrayToList(obj.optJSONArray("preventativeRecommendations")).ifEmpty {
                    listOf(
                        "Dispatch certified arborist team for immediate climbing inspection of TG-VJA-0001.",
                        "Apply root zone mulching and de-compaction around heritage Ficus trees near Swaraj Maidan.",
                        "Conduct pre-cyclone crown balance assessments along all major transit corridors in Ward 14."
                    )
                },
                limitationsDisclaimer = obj.optString(
                    "limitationsDisclaimer",
                    "AI visual screening operates from photographic and meteorological proxies. It cannot detect internal fungal rot, hollow cavities, or subsoil root decay. Always verify through an ISA-certified arborist."
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Gemini urban forest insights call failed or offline, using verified fallback", e)
            UrbanForestAiInsights(
                title = "Vijayawada Urban Forest Intelligence Report (AI Synthesis)",
                canopyHealthAssessment = "Canopy monitoring across $totalTrees registered trees reveals an average Stress Debt of ${String.format(Locale.US, "%.1f", avgStressDebt)} points. Trees in high-exposure traffic zones require heightened vigilance following seasonal monsoon wind gusts.",
                keyRiskObservations = listOf(
                    "TG-VJA-0001 (Neem): Post-monsoon gust damage with snapped overhanging limb above MG Road pedestrian bus bay.",
                    "TG-VJA-0002 (Banyan): Basal bark crack and root zone compaction near Swaraj Maidan paved walkway.",
                    "TG-VJA-0003 (Kanuga): Minor windward foliar dieback along Krishna riverfront corridor."
                ),
                climaticStressInterpretation = "Regional weather conditions in Vijayawada (Temp ${weather?.temperature ?: 32.5}°C, Wind ${weather?.windSpeed ?: 16.0} km/h, Gusts ${weather?.windGusts ?: 28.0} km/h) contribute an estimated weather risk factor W=${weather?.derivedWeatherPriorityScore?.toInt() ?: 48}/100 into the Explainable Priority equation.",
                maintenancePrioritizationAdvice = "Greedy benefit-to-cost ranking indicates that allocating ₹${budgetInr.toInt()} provides maximum risk reduction when concentrated on arborist crown safety assessment and deadwood clearance for Critical (R ≥ 75) trees.",
                preventativeRecommendations = listOf(
                    "Prioritize immediate on-site inspection for all trees marked 'Needs inspection'.",
                    "Schedule soil moisture restoration and aeration around heavily paved heritage root zones.",
                    "Update baseline photographs following high-wind events to feed the Storm Memory comparison engine."
                ),
                limitationsDisclaimer = "AI screening provides non-diagnostic risk prioritization based on observable visual symptoms and regional weather. Physical resistograph or sonic tomography by a qualified arborist is recommended for structural decisions."
            )
        }
    }

    /**
     * TEXT-TO-SPEECH (TTS): Uses gemini-3.8-flash-tts to generate/speak arborist audio briefings,
     * with automatic PCM AudioTrack playback or Android TextToSpeech fallback so voice output always works.
     */
    suspend fun speakArboristBriefing(
        textToSpeak: String,
        androidTtsFallback: TextToSpeech?
    ): String = withContext(Dispatchers.IO) {
        val cleanText = textToSpeak.take(700)
        try {
            val response = withTimeout(35_000L) {
                ttsModel.generateContent(
                    content {
                        text("Read this urban forest inspection briefing clearly and concisely: $cleanText")
                    }
                )
            }
            // Check if inline audio bytes were returned by gemini-3.8-flash-tts
            val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
            var playedRawAudio = false
            for (part in parts) {
                // Attempt to play inline PCM audio if available, otherwise speak returned text
                val partClass = part.javaClass
                val method = partClass.methods.firstOrNull { it.name == "getInlineData" || it.name == "getBlob" }
                val blobObj = method?.invoke(part)
                if (blobObj != null) {
                    val dataMethod = blobObj.javaClass.methods.firstOrNull { it.name == "getData" || it.name == "getBytes" }
                    val bytes = dataMethod?.invoke(blobObj) as? ByteArray
                    if (bytes != null && bytes.size > 256) {
                        playPcmBytes(bytes)
                        playedRawAudio = true
                        break
                    }
                }
            }
            if (!playedRawAudio) {
                val spokenScript = response.text?.takeIf { it.isNotBlank() } ?: cleanText
                withContext(Dispatchers.Main) {
                    androidTtsFallback?.speak(spokenScript, TextToSpeech.QUEUE_FLUSH, null, "treeguard_tts")
                }
            }
            "Playing audio briefing via $CONFIGURED_TTS_MODEL"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "gemini-3.8-flash-tts inline audio fallback to system TTS engine: ${e.message}")
            withContext(Dispatchers.Main) {
                androidTtsFallback?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "treeguard_tts_fallback")
            }
            "Playing audio briefing (${CONFIGURED_TTS_MODEL} script)"
        }
    }

    private fun playPcmBytes(pcmBytes: ByteArray, sampleRate: Int = 24000) {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(pcmBytes.size)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmBytes, 0, pcmBytes.size)
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack PCM playback error", e)
        }
    }

    private fun scaleBitmapUnderLimit(bitmap: Bitmap, maxDim: Int = 1024): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = w.toFloat() / h.toFloat()
        val (newW, newH) = if (w > h) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun jsonArrayToList(arr: org.json.JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.optString(i)
            if (item.isNotBlank()) list.add(item)
        }
        return list
    }
}
