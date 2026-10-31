package com.example.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object OpenMeteoWeatherService {
    private const val TAG = "OpenMeteoWeather"
    private const val CACHE_TTL_MS = 15L * 60L * 1000L // 15 minutes TTL for recent responses

    const val VIJAYAWADA_LAT = 16.5062
    const val VIJAYAWADA_LON = 80.6480
    const val VIJAYAWADA_LABEL = "Vijayawada, Andhra Pradesh, India"

    private data class CachedWeatherEntry(
        val snapshot: RegionalWeatherSnapshot,
        val timestampMs: Long
    )

    private val responseCache = ConcurrentHashMap<String, CachedWeatherEntry>()
    @Volatile
    private var mostRecentSuccessfulSnapshot: RegionalWeatherSnapshot? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private fun coordinateCacheKey(latitude: Double, longitude: Double): String {
        val latRound = (latitude * 1000.0).roundToInt() / 1000.0
        val lonRound = (longitude * 1000.0).roundToInt() / 1000.0
        return String.format(Locale.US, "%.3f,%.3f", latRound, lonRound)
    }

    fun getCachedLocationsCount(): Int = responseCache.size

    /**
     * Queries Open-Meteo live weather, wind gusts, soil moisture (0-1cm), and 7-day forecast
     * for the specified GPS coordinates (or default Vijayawada).
     * Caches recent successful responses per coordinate and handles API errors gracefully.
     * Note: Weather data is regional environmental context, not a direct measurement of an individual tree.
     */
    suspend fun fetchRegionalWeather(
        latitude: Double = VIJAYAWADA_LAT,
        longitude: Double = VIJAYAWADA_LON,
        locationLabel: String = VIJAYAWADA_LABEL,
        forceRefresh: Boolean = false
    ): RegionalWeatherSnapshot = withContext(Dispatchers.IO) {
        val key = coordinateCacheKey(latitude, longitude)
        val nowMs = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("dd MMM HH:mm 'IST'", Locale.US)

        // Return cached recent response if within 15-minute TTL and not forcing refresh
        if (!forceRefresh) {
            val cached = responseCache[key]
            if (cached != null && (nowMs - cached.timestampMs) <= CACHE_TTL_MS) {
                val ageSec = ((nowMs - cached.timestampMs) / 1000L).coerceAtLeast(1L)
                return@withContext cached.snapshot.copy(
                    cacheStatus = "CACHED_RECENT",
                    cachedLocationsCount = responseCache.size,
                    fetchedAtLabel = "Cached Open-Meteo (${ageSec}s ago) • ${timeFmt.format(Date(cached.timestampMs))}"
                )
            }
        }

        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_gusts_10m" +
            "&hourly=soil_moisture_0_to_1cm" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_gusts_10m_max" +
            "&timezone=Asia%2FKolkata"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TreeGuardAI-Android/1.0")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Open-Meteo HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw IllegalStateException("Empty Open-Meteo response")
                val json = JSONObject(body)
                val current = json.getJSONObject("current")

                val temp = current.optDouble("temperature_2m", 33.8)
                val humidity = current.optDouble("relative_humidity_2m", 62.0)
                val precip = current.optDouble("precipitation", 0.0)
                val windSpeed = current.optDouble("wind_speed_10m", 14.2)
                val windGusts = current.optDouble("wind_gusts_10m", 26.5)

                // Extract soil moisture if available from hourly array
                var soilMoistureVal: Double? = null
                val hourly = json.optJSONObject("hourly")
                val soilArr = hourly?.optJSONArray("soil_moisture_0_to_1cm")
                if (soilArr != null && soilArr.length() > 0) {
                    for (i in 0 until soilArr.length()) {
                        if (!soilArr.isNull(i)) {
                            soilMoistureVal = soilArr.optDouble(i)
                            break
                        }
                    }
                }

                // Compute normalized Stress Debt (H_t, D_t) and Inspection Priority (W) inputs
                val engineInputs = TreeGuardEngines.computeWeatherEngineIndicators(
                    temperatureC = temp,
                    humidityPercent = humidity,
                    precipitationMm = precip,
                    windSpeedKmh = windSpeed,
                    windGustsKmh = windGusts,
                    soilMoistureM3 = soilMoistureVal
                )

                // Extract 7-day forecast with per-day stress & priority indicators
                val forecastList = mutableListOf<DailyForecastItem>()
                val daily = json.optJSONObject("daily")
                if (daily != null) {
                    val dates = daily.optJSONArray("time")
                    val maxTemps = daily.optJSONArray("temperature_2m_max")
                    val minTemps = daily.optJSONArray("temperature_2m_min")
                    val precipSums = daily.optJSONArray("precipitation_sum")
                    val maxGusts = daily.optJSONArray("wind_gusts_10m_max")
                    val count = dates?.length() ?: 0
                    for (i in 0 until minOf(count, 7)) {
                        val dMaxTemp = maxTemps?.optDouble(i, 34.0) ?: 34.0
                        val dMinTemp = minTemps?.optDouble(i, 25.0) ?: 25.0
                        val dPrecip = precipSums?.optDouble(i, 0.0) ?: 0.0
                        val dGusts = maxGusts?.optDouble(i, 24.0) ?: 24.0
                        val dayIndicators = TreeGuardEngines.computeWeatherEngineIndicators(
                            temperatureC = dMaxTemp,
                            humidityPercent = humidity,
                            precipitationMm = dPrecip,
                            windSpeedKmh = dGusts * 0.55,
                            windGustsKmh = dGusts,
                            soilMoistureM3 = soilMoistureVal
                        )
                        forecastList.add(
                            DailyForecastItem(
                                date = dates?.optString(i, "Day ${i + 1}") ?: "Day ${i + 1}",
                                maxTemp = dMaxTemp,
                                minTemp = dMinTemp,
                                precipitationSum = dPrecip,
                                maxWindGusts = dGusts,
                                estimatedDailyHeatStress = dayIndicators.heatIndicatorHt,
                                estimatedDailyWeatherRisk = dayIndicators.weatherPriorityScoreW
                            )
                        )
                    }
                }

                val snapshot = RegionalWeatherSnapshot(
                    latitude = latitude,
                    longitude = longitude,
                    locationLabel = locationLabel,
                    temperature = temp,
                    humidity = humidity,
                    precipitation = precip,
                    windSpeed = windSpeed,
                    windGusts = windGusts,
                    soilMoisture = soilMoistureVal,
                    isLiveApi = true,
                    fetchedAtLabel = "Live Open-Meteo • ${timeFmt.format(Date(nowMs))}",
                    forecast7Day = forecastList,
                    cacheStatus = "LIVE_API",
                    cachedLocationsCount = responseCache.size + 1,
                    lastErrorMessage = null,
                    derivedHeatIndicator = engineInputs.heatIndicatorHt,
                    derivedDrynessIndicator = engineInputs.drynessIndicatorDt,
                    derivedWeatherPriorityScore = engineInputs.weatherPriorityScoreW
                )
                responseCache[key] = CachedWeatherEntry(snapshot, nowMs)
                mostRecentSuccessfulSnapshot = snapshot
                snapshot.copy(cachedLocationsCount = responseCache.size)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Open-Meteo API error; falling back to cached or clearly labelled demo weather snapshot", e)
            val errSummary = e.localizedMessage ?: "Network unreachable"
            val staleForCoord = responseCache[key]?.snapshot ?: mostRecentSuccessfulSnapshot
            if (staleForCoord != null) {
                staleForCoord.copy(
                    cacheStatus = "CACHED_FALLBACK",
                    cachedLocationsCount = responseCache.size,
                    lastErrorMessage = "Serving cached Open-Meteo response ($errSummary)",
                    fetchedAtLabel = "Cached Fallback • ${staleForCoord.fetchedAtLabel}"
                )
            } else {
                val demoEngineInputs = TreeGuardEngines.computeWeatherEngineIndicators(
                    temperatureC = 35.4,
                    humidityPercent = 58.0,
                    precipitationMm = 0.0,
                    windSpeedKmh = 16.8,
                    windGustsKmh = 31.2,
                    soilMoistureM3 = 0.21
                )
                RegionalWeatherSnapshot(
                    latitude = latitude,
                    longitude = longitude,
                    locationLabel = "$locationLabel (Demo Regional Proxy)",
                    temperature = 35.4,
                    humidity = 58.0,
                    precipitation = 0.0,
                    windSpeed = 16.8,
                    windGusts = 31.2,
                    soilMoisture = 0.21,
                    isLiveApi = false,
                    fetchedAtLabel = "Demo Regional Fallback (Offline/Unreachable)",
                    forecast7Day = listOf(
                        DailyForecastItem("Day 1", 35.8, 26.2, 0.0, 29.0, 65.8, 52.4),
                        DailyForecastItem("Day 2", 36.4, 26.8, 0.0, 32.5, 68.3, 56.1),
                        DailyForecastItem("Day 3", 34.9, 25.9, 4.2, 38.0, 62.1, 55.8),
                        DailyForecastItem("Day 4", 33.2, 25.1, 12.5, 44.0, 55.0, 56.4),
                        DailyForecastItem("Day 5", 34.1, 25.4, 1.8, 27.0, 58.8, 48.2),
                        DailyForecastItem("Day 6", 35.0, 26.0, 0.0, 24.5, 62.5, 48.9),
                        DailyForecastItem("Day 7", 35.6, 26.3, 0.0, 25.0, 65.0, 50.1)
                    ),
                    cacheStatus = "DEMO_FALLBACK",
                    cachedLocationsCount = responseCache.size,
                    lastErrorMessage = "Open-Meteo unreachable ($errSummary) — using Vijayawada fallback",
                    derivedHeatIndicator = demoEngineInputs.heatIndicatorHt,
                    derivedDrynessIndicator = demoEngineInputs.drynessIndicatorDt,
                    derivedWeatherPriorityScore = demoEngineInputs.weatherPriorityScoreW
                )
            }
        }
    }
}
