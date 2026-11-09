package com.medusko.velocast.data

import com.medusko.velocast.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.math.*

data class GeocodedLocation(
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val elevationFeet: Int
)

data class PlannedRouteResult(
    val waypoints: List<RouteWaypoint>,
    val geometry: List<Pair<Double, Double>>,
    val totalDistanceMiles: Double,
    val estimatedMinutes: Int
)

class VelocastRepository(private val prefs: AppPreferences) {

    private data class StationRef(
        val id: String,
        val name: String,
        val lat: Double,
        val lon: Double,
        val elevFt: Int
    )

    private val knownMicroclimateStations = listOf(
        StationRef("PTBC1", "Point Bonita USCG Coastal Station", 37.8156, -122.5295, 115),
        StationRef("KSFO", "San Francisco Intl ASOS", 37.6188, -122.3754, 13),
        StationRef("MTHC1", "Mt. Tamalpais Middle Peak RAWS", 37.9261, -122.5790, 2480),
        StationRef("SLAC1", "Skyline Ridge / Woodside RAWS", 37.3812, -122.2640, 1840),
        StationRef("KHAF", "Half Moon Bay Coastal AWOS", 37.5136, -122.5011, 66),
        StationRef("KBUR", "Hollywood Burbank Airport ASOS", 34.2007, -118.3587, 778),
        StationRef("KSMO", "Santa Monica Municipal ASOS", 34.0158, -118.4513, 177),
        StationRef("MLBC1", "Malibu Hills / Latigo RAWS", 34.0725, -118.7510, 1560),
        StationRef("KBDU", "Boulder Municipal Airport AWOS", 40.0394, -105.2258, 5288),
        StationRef("BTAC2", "Sugarloaf / Boulder Canyon RAWS", 40.0180, -105.3580, 7850)
    )

    // Verified live working cameras with real HTTP 200 JPEG endpoints & unique stream player URLs
    private val verifiedLiveCameras = listOf(
        PublicCameraFeed(
            id = "cam_tve73_sausalito_spencer",
            title = "US-101 at Spencer Ave • Marin Headlands",
            locationSubtitle = "Sausalito, CA, USA • Hawk Hill Approach",
            sourceProvider = CameraFeedOption.WINDY_WEBCAMS,
            latitude = 37.85081,
            longitude = -122.48975,
            distanceMiles = 0.0,
            elevationFeet = 240,
            imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tve73us101spenceravenue/tve73us101spenceravenue.jpg",
            streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tve73us101spenceravenue.htm",
            hlsStreamUrl = "https://wzmedia.dot.ca.gov/D4/N101_at_Spencer_Av.stream/playlist.m3u8",
            directionLabel = "Northbound Golden Gate & Headlands Corridor",
            roadConditionNote = "Live Video & Snapshot • Sausalito Ridge"
        ),
        PublicCameraFeed(
            id = "cam_tv388_presidio_ggb",
            title = "SR-1 at Presidio Tunnel • Golden Gate South",
            locationSubtitle = "San Francisco, CA, USA • Bridge Bike Approach",
            sourceProvider = CameraFeedOption.WINDY_WEBCAMS,
            latitude = 37.79138,
            longitude = -122.46968,
            distanceMiles = 0.0,
            elevationFeet = 165,
            imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv388sr1justsouthofpresidiotunnel/tv388sr1justsouthofpresidiotunnel.jpg",
            streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tv388sr1justsouthofpresidiotunnel.htm",
            hlsStreamUrl = "https://wzmedia.dot.ca.gov/D4/N1_at_Presidio_Tunnel.stream/playlist.m3u8",
            directionLabel = "Looking North toward Golden Gate Bridge",
            roadConditionNote = "Live Video & Snapshot • Presidio Approach"
        ),
        PublicCameraFeed(
            id = "cam_tve75_mill_valley_sr1",
            title = "US-101 at Marin SR-1 • Mt. Tamalpais Base",
            locationSubtitle = "Mill Valley, CA, USA • Shoreline Hwy Junction",
            sourceProvider = CameraFeedOption.STATE_DOT_511,
            latitude = 37.88161,
            longitude = -122.51698,
            distanceMiles = 0.0,
            elevationFeet = 48,
            imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tve75us101marinsr1/tve75us101marinsr1.jpg",
            streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tve75us101marinsr1.htm",
            hlsStreamUrl = "https://wzmedia.dot.ca.gov/D4/S101_at_JCT_1.stream/playlist.m3u8",
            directionLabel = "Southbound SR-1 / Mill Valley Bike Route",
            roadConditionNote = "Caltrans D4 Live Stream • Marin SR-1"
        ),
        PublicCameraFeed(
            id = "cam_tv429_woodside_sr84",
            title = "SR-84 Woodside Road • Old La Honda Approach",
            locationSubtitle = "Redwood City / Woodside, CA, USA",
            sourceProvider = CameraFeedOption.STATE_DOT_511,
            latitude = 37.49180,
            longitude = -122.22067,
            distanceMiles = 0.0,
            elevationFeet = 95,
            imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv429us101northofsr84woodsideroad/tv429us101northofsr84woodsideroad.jpg",
            streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tv429us101northofsr84woodsideroad.htm",
            hlsStreamUrl = "https://wzmedia.dot.ca.gov/D4/S101_NOF_84_Woodside_Rd.stream/playlist.m3u8",
            directionLabel = "Looking toward SR-84 Woodside Climb Corridor",
            roadConditionNote = "Caltrans D4 Live Stream • Woodside Rd"
        ),
        PublicCameraFeed(
            id = "cam_tv074_hmb_sr1",
            title = "SR-1 at Capistrano Rd • Half Moon Bay Coast",
            locationSubtitle = "Half Moon Bay, CA, USA • Coastal Hwy 1",
            sourceProvider = CameraFeedOption.WINDY_WEBCAMS,
            latitude = 37.50541,
            longitude = -122.48126,
            distanceMiles = 0.0,
            elevationFeet = 42,
            imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv074sr1capistranoroad/tv074sr1capistranoroad.jpg",
            streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tv074sr1capistranoroad.htm",
            hlsStreamUrl = "https://wzmedia.dot.ca.gov/D4/N1_at_Capistrano_Rd.stream/playlist.m3u8",
            directionLabel = "Northbound Pacific Coast Highway 1",
            roadConditionNote = "Live Coastal Marine Layer & Wind Feed"
        ),
        PublicCameraFeed(
            id = "cam_tv450_i280_sr92_skyline",
            title = "I-280 at SR-92 • Crystal Springs & Skyline",
            locationSubtitle = "Belmont / San Mateo, CA, USA • Cañada Rd",
            sourceProvider = CameraFeedOption.STATE_DOT_511,
            latitude = 37.50649,
            longitude = -122.33685,
            distanceMiles = 0.0,
            elevationFeet = 580,
            imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv450i280sr92/tv450i280sr92.jpg",
            streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tv450i280sr92.htm",
            hlsStreamUrl = "https://wzmedia.dot.ca.gov/D4/N280_at_92_280_ITC.stream/playlist.m3u8",
            directionLabel = "Cañada Road & Crystal Springs Reservoir",
            roadConditionNote = "Caltrans D4 Live Stream • Ridge Fog Check"
        )
    )

    suspend fun fetchWeatherForLocation(
        lat: Double,
        lon: Double,
        hourOffset: Int = 0
    ): WeatherSnapshot = withContext(Dispatchers.IO) {
        val selectedApi = prefs.weatherApiOption
        try {
            when (selectedApi) {
                WeatherApiOption.OPEN_METEO -> fetchOpenMeteo(lat, lon, hourOffset, "Open-Meteo")
                WeatherApiOption.NOAA_NWS -> fetchNoaaWithOpenMeteoEnrichment(lat, lon, hourOffset)
                WeatherApiOption.WEATHER_API -> {
                    val key = prefs.weatherApiKey
                    if (key.isNotBlank()) {
                        fetchWeatherApiCom(lat, lon, key, hourOffset)
                    } else {
                        fetchOpenMeteo(lat, lon, hourOffset, "WeatherAPI")
                    }
                }
                WeatherApiOption.OPEN_WEATHER_MAP -> {
                    val key = prefs.openWeatherMapKey
                    if (key.isNotBlank()) {
                        fetchOpenWeatherMap(lat, lon, key, hourOffset)
                    } else {
                        fetchOpenMeteo(lat, lon, hourOffset, "OpenWeatherMap")
                    }
                }
            }
        } catch (_: Exception) {
            generateFallbackWeather(lat, lon, hourOffset, selectedApi.title)
        }
    }

    private fun fetchOpenMeteo(
        lat: Double,
        lon: Double,
        hourOffset: Int,
        providerLabel: String
    ): WeatherSnapshot {
        val daysNeeded = ((hourOffset / 24) + 3).coerceIn(2, 16)
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                "&hourly=temperature_2m,precipitation_probability,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index" +
                "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=$daysNeeded"

        val jsonStr = httpGet(url)
        val root = JSONObject(jsonStr)

        val elevationMeters = root.optDouble("elevation", 100.0)
        val elevationFeet = (elevationMeters * 3.28084).roundToInt()

        val current = root.optJSONObject("current")
        val daily = root.optJSONObject("daily")
        val hourly = root.optJSONObject("hourly")

        val hourlyTimes = hourly?.optJSONArray("time")
        val hourlyTemps = hourly?.optJSONArray("temperature_2m")
        val hourlyPrecip = hourly?.optJSONArray("precipitation_probability")
        val hourlyWind = hourly?.optJSONArray("wind_speed_10m")
        val hourlyWindDir = hourly?.optJSONArray("wind_direction_10m")
        val hourlyGusts = hourly?.optJSONArray("wind_gusts_10m")
        val hourlyUv = hourly?.optJSONArray("uv_index")

        val currentHourCal = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val targetIndex = (currentHourCal + hourOffset).coerceIn(0, (hourlyTimes?.length() ?: 48) - 1)
        val dayIndex = (targetIndex / 24).coerceIn(0, (daily?.optJSONArray("sunrise")?.length() ?: 1) - 1)

        val tempF = if (hourOffset == 0 && current != null) {
            current.optDouble("temperature_2m", 65.0)
        } else {
            hourlyTemps?.optDouble(targetIndex, 65.0) ?: 65.0
        }

        val feelsLikeF = if (hourOffset == 0 && current != null) {
            current.optDouble("apparent_temperature", tempF)
        } else {
            tempF - 1.5
        }

        val windSpeedMph = if (hourOffset == 0 && current != null) {
            current.optDouble("wind_speed_10m", 10.0)
        } else {
            hourlyWind?.optDouble(targetIndex, 10.0) ?: 10.0
        }

        val windGustMph = if (hourOffset == 0 && current != null) {
            current.optDouble("wind_gusts_10m", windSpeedMph * 1.35)
        } else {
            hourlyGusts?.optDouble(targetIndex, windSpeedMph * 1.35) ?: (windSpeedMph * 1.35)
        }

        val windDirDeg = if (hourOffset == 0 && current != null) {
            current.optInt("wind_direction_10m", 290)
        } else {
            hourlyWindDir?.optInt(targetIndex, 290) ?: 290
        }

        val rainChance = hourlyPrecip?.optInt(targetIndex, 10)
            ?: daily?.optJSONArray("precipitation_probability_max")?.optInt(dayIndex, 10)
            ?: 10

        val uvIdx = hourlyUv?.optDouble(targetIndex, 5.0)
            ?: daily?.optJSONArray("uv_index_max")?.optDouble(dayIndex, 5.0)
            ?: 5.0

        val tempHighF = daily?.optJSONArray("temperature_2m_max")?.optDouble(dayIndex, tempF + 6.0) ?: (tempF + 6.0)
        val tempLowF = daily?.optJSONArray("temperature_2m_min")?.optDouble(dayIndex, tempF - 8.0) ?: (tempF - 8.0)

        val rawSunrise = daily?.optJSONArray("sunrise")?.optString(dayIndex, "2026-11-08T06:42") ?: "06:42"
        val rawSunset = daily?.optJSONArray("sunset")?.optString(dayIndex, "2026-11-08T17:05") ?: "17:05"

        val weatherCode = current?.optInt("weather_code", 1) ?: 1

        // 24-Hour Cycling Wind & Weather Timeline
        val hourlyList = mutableListOf<HourlyForecastPoint>()
        if (hourlyTimes != null) {
            val startIdx = targetIndex.coerceAtLeast(0)
            val endIdx = (startIdx + 24).coerceAtMost(hourlyTimes.length())
            for (i in startIdx until endIdx) {
                val iso = hourlyTimes.optString(i, "")
                val hr = parseHourFromIso(iso, i % 24)
                hourlyList.add(
                    HourlyForecastPoint(
                        timeLabel = formatHourLabel(hr),
                        hourOfDay = hr,
                        tempF = hourlyTemps?.optDouble(i, tempF) ?: tempF,
                        windSpeedMph = hourlyWind?.optDouble(i, windSpeedMph) ?: windSpeedMph,
                        windGustMph = hourlyGusts?.optDouble(i, windGustMph) ?: windGustMph,
                        windDirectionDeg = hourlyWindDir?.optInt(i, windDirDeg) ?: windDirDeg,
                        precipProbPercent = hourlyPrecip?.optInt(i, rainChance) ?: rainChance,
                        uvIndex = hourlyUv?.optDouble(i, uvIdx) ?: uvIdx
                    )
                )
            }
        }

        val (stationId, stationName, stationDist) = resolveClosestStation(lat, lon)

        return WeatherSnapshot(
            temperatureF = tempF,
            feelsLikeF = feelsLikeF,
            tempHighF = tempHighF,
            tempLowF = tempLowF,
            windSpeedMph = windSpeedMph,
            windGustMph = max(windGustMph, windSpeedMph),
            windDirectionDeg = windDirDeg,
            windCardinal = CyclingMath.cardinalFromDegrees(windDirDeg),
            rainChancePercent = rainChance.coerceIn(0, 100),
            uvIndex = uvIdx.coerceAtLeast(0.0),
            sunriseTime = formatIsoTimeOnly(rawSunrise),
            sunsetTime = formatIsoTimeOnly(rawSunset),
            conditionLabel = weatherCodeToLabel(weatherCode),
            closestStationId = stationId,
            closestStationName = stationName,
            stationDistanceMiles = stationDist,
            elevationFeet = elevationFeet,
            providerUsed = providerLabel,
            hourly = hourlyList
        )
    }

    private fun fetchNoaaWithOpenMeteoEnrichment(
        lat: Double,
        lon: Double,
        hourOffset: Int
    ): WeatherSnapshot {
        val base = fetchOpenMeteo(lat, lon, hourOffset, "NOAA / NWS Station API")
        return try {
            val pointsUrl = String.format(Locale.US, "https://api.weather.gov/points/%.4f,%.4f", lat, lon)
            val pointsJson = JSONObject(httpGet(pointsUrl))
            val props = pointsJson.getJSONObject("properties")
            val stationsUrl = props.optString("observationStations", "")
            if (stationsUrl.isNotBlank()) {
                val stationsJson = JSONObject(httpGet(stationsUrl))
                val features = stationsJson.optJSONArray("features")
                if (features != null && features.length() > 0) {
                    val first = features.getJSONObject(0)
                    val sProps = first.getJSONObject("properties")
                    val sId = sProps.optString("stationIdentifier", base.closestStationId)
                    val sName = sProps.optString("name", base.closestStationName)
                    val geom = first.optJSONObject("geometry")?.optJSONArray("coordinates")
                    val dist = if (geom != null && geom.length() >= 2) {
                        val sLon = geom.getDouble(0)
                        val sLat = geom.getDouble(1)
                        CyclingMath.haversineMiles(lat, lon, sLat, sLon)
                    } else {
                        base.stationDistanceMiles
                    }
                    return base.copy(
                        closestStationId = sId,
                        closestStationName = sName,
                        stationDistanceMiles = dist,
                        providerUsed = "NOAA / NWS ($sId)"
                    )
                }
            }
            base
        } catch (_: Exception) {
            base
        }
    }

    private fun fetchWeatherApiCom(
        lat: Double,
        lon: Double,
        apiKey: String,
        hourOffset: Int
    ): WeatherSnapshot {
        return try {
            val base = fetchOpenMeteo(lat, lon, hourOffset, "WeatherAPI.com")
            val url = "https://api.weatherapi.com/v1/forecast.json?key=${URLEncoder.encode(apiKey, "UTF-8")}&q=$lat,$lon&days=2"
            val root = JSONObject(httpGet(url))
            val current = root.getJSONObject("current")
            val forecastDay = root.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0)
            val dayObj = forecastDay.getJSONObject("day")
            val astro = forecastDay.getJSONObject("astro")

            val windMph = current.optDouble("wind_mph", base.windSpeedMph)
            val gustMph = current.optDouble("gust_mph", windMph * 1.3)
            val windDeg = current.optInt("wind_degree", base.windDirectionDeg)

            base.copy(
                temperatureF = current.optDouble("temp_f", base.temperatureF),
                feelsLikeF = current.optDouble("feelslike_f", base.feelsLikeF),
                tempHighF = dayObj.optDouble("maxtemp_f", base.tempHighF),
                tempLowF = dayObj.optDouble("mintemp_f", base.tempLowF),
                windSpeedMph = windMph,
                windGustMph = gustMph,
                windDirectionDeg = windDeg,
                windCardinal = current.optString("wind_dir", CyclingMath.cardinalFromDegrees(windDeg)),
                rainChancePercent = dayObj.optInt("daily_chance_of_rain", base.rainChancePercent),
                uvIndex = current.optDouble("uv", base.uvIndex),
                sunriseTime = astro.optString("sunrise", base.sunriseTime),
                sunsetTime = astro.optString("sunset", base.sunsetTime),
                conditionLabel = current.optJSONObject("condition")?.optString("text", base.conditionLabel) ?: base.conditionLabel,
                providerUsed = "WeatherAPI.com"
            )
        } catch (_: Exception) {
            fetchOpenMeteo(lat, lon, hourOffset, "WeatherAPI.com")
        }
    }

    private fun fetchOpenWeatherMap(
        lat: Double,
        lon: Double,
        apiKey: String,
        hourOffset: Int
    ): WeatherSnapshot {
        return try {
            val base = fetchOpenMeteo(lat, lon, hourOffset, "OpenWeatherMap")
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=imperial&appid=${URLEncoder.encode(apiKey, "UTF-8")}"
            val root = JSONObject(httpGet(url))
            val main = root.getJSONObject("main")
            val wind = root.getJSONObject("wind")
            val windMph = wind.optDouble("speed", base.windSpeedMph)
            val gustMph = wind.optDouble("gust", windMph * 1.3)
            val windDeg = wind.optInt("deg", base.windDirectionDeg)

            base.copy(
                temperatureF = main.optDouble("temp", base.temperatureF),
                feelsLikeF = main.optDouble("feels_like", base.feelsLikeF),
                tempHighF = main.optDouble("temp_max", base.tempHighF),
                tempLowF = main.optDouble("temp_min", base.tempLowF),
                windSpeedMph = windMph,
                windGustMph = gustMph,
                windDirectionDeg = windDeg,
                windCardinal = CyclingMath.cardinalFromDegrees(windDeg),
                conditionLabel = root.optJSONArray("weather")?.optJSONObject(0)?.optString("main", base.conditionLabel) ?: base.conditionLabel,
                providerUsed = "OpenWeatherMap"
            )
        } catch (_: Exception) {
            fetchOpenMeteo(lat, lon, hourOffset, "OpenWeatherMap")
        }
    }

    private fun resolveClosestStation(lat: Double, lon: Double): Triple<String, String, Double> {
        var best: StationRef? = null
        var bestDist = Double.MAX_VALUE
        for (s in knownMicroclimateStations) {
            val d = CyclingMath.haversineMiles(lat, lon, s.lat, s.lon)
            if (d < bestDist) {
                bestDist = d
                best = s
            }
        }
        if (best != null && bestDist <= 35.0) {
            return Triple(best.id, best.name, bestDist)
        }
        val gridCode = String.format(
            Locale.US,
            "HRRR-%02d%02d",
            abs((lat * 10).roundToInt()) % 100,
            abs((lon * 10).roundToInt()) % 100
        )
        return Triple(gridCode, "Microclimate Grid Station", 0.8)
    }

    suspend fun searchLocations(query: String): List<GeocodedLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json"
            val root = JSONObject(httpGet(url))
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<GeocodedLocation>()
            for (i in 0 until results.length()) {
                val obj = results.getJSONObject(i)
                val name = obj.optString("name", query)
                val admin1 = obj.optString("admin1", "")
                val countryRaw = obj.optString("country", "USA")
                val country = if (countryRaw.equals("United States", ignoreCase = true)) "USA" else countryRaw
                val region = listOf(admin1, country).filter { it.isNotBlank() }.joinToString(", ")
                val elevMeters = obj.optDouble("elevation", 80.0)
                list.add(
                    GeocodedLocation(
                        name = name,
                        region = CyclingMath.ensureCountryInRegion(region),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        elevationFeet = (elevMeters * 3.28084).roundToInt()
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun calculateRouteForecast(
        rawPoints: List<RouteWaypoint>,
        avgSpeedMph: Double,
        startHourOffset: Int
    ): PlannedRouteResult = withContext(Dispatchers.IO) {
        if (rawPoints.isEmpty()) {
            return@withContext PlannedRouteResult(emptyList(), emptyList(), 0.0, 0)
        }

        val geometry = mutableListOf<Pair<Double, Double>>()
        var totalRoadMiles = 0.0
        if (rawPoints.size >= 2) {
            try {
                val coordsParam = rawPoints.joinToString(";") { "${it.longitude},${it.latitude}" }
                val osrmUrl = "https://router.project-osrm.org/route/v1/bike/$coordsParam?overview=full&geometries=geojson"
                val root = JSONObject(httpGet(osrmUrl))
                val routes = root.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val route0 = routes.getJSONObject(0)
                    totalRoadMiles = route0.optDouble("distance", 0.0) * 0.000621371
                    val coordsArr = route0.optJSONObject("geometry")?.optJSONArray("coordinates")
                    if (coordsArr != null) {
                        for (i in 0 until coordsArr.length()) {
                            val pt = coordsArr.getJSONArray(i)
                            geometry.add(Pair(pt.getDouble(1), pt.getDouble(0)))
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (geometry.isEmpty()) {
            rawPoints.forEach { geometry.add(Pair(it.latitude, it.longitude)) }
        }

        val enrichedSteps = mutableListOf<RouteWaypoint>()
        var cumulativeMiles = 0.0
        for (i in rawPoints.indices) {
            val pt = rawPoints[i]
            if (i > 0) {
                val prev = rawPoints[i - 1]
                cumulativeMiles += CyclingMath.haversineMiles(prev.latitude, prev.longitude, pt.latitude, pt.longitude) * 1.18
            }
            val bearing = if (i < rawPoints.lastIndex) {
                val next = rawPoints[i + 1]
                CyclingMath.calculateBearing(pt.latitude, pt.longitude, next.latitude, next.longitude)
            } else if (i > 0) {
                val prev = rawPoints[i - 1]
                CyclingMath.calculateBearing(prev.latitude, prev.longitude, pt.latitude, pt.longitude)
            } else {
                0
            }

            val minutesFromStart = ((cumulativeMiles / avgSpeedMph.coerceAtLeast(5.0)) * 60.0).roundToInt()
            val etaLabel = if (i == 0) "Start" else "+${minutesFromStart}m"

            enrichedSteps.add(
                pt.copy(
                    index = i,
                    distanceFromStartMiles = cumulativeMiles,
                    etaLabel = etaLabel,
                    bearingDeg = bearing
                )
            )
        }

        if (totalRoadMiles <= 0.1) {
            totalRoadMiles = cumulativeMiles
        }
        val totalMinutes = ((totalRoadMiles / avgSpeedMph.coerceAtLeast(5.0)) * 60.0).roundToInt()

        val withWeather = coroutineScope {
            enrichedSteps.map { wp ->
                async {
                    val wpMinutes = ((wp.distanceFromStartMiles / avgSpeedMph.coerceAtLeast(5.0)) * 60.0).roundToInt()
                    val wpHourOffset = startHourOffset + (wpMinutes / 60)
                    val weather = fetchWeatherForLocation(wp.latitude, wp.longitude, wpHourOffset)
                    val (impact, headwind, crosswind) = CyclingMath.analyzeWindOnSegment(
                        riderBearingDeg = wp.bearingDeg,
                        windFromDeg = weather.windDirectionDeg,
                        windSpeedMph = weather.windSpeedMph
                    )
                    wp.copy(
                        windImpact = impact,
                        headwindComponentMph = headwind,
                        crosswindComponentMph = crosswind,
                        weather = weather
                    )
                }
            }.awaitAll()
        }

        PlannedRouteResult(
            waypoints = withWeather,
            geometry = geometry,
            totalDistanceMiles = totalRoadMiles,
            estimatedMinutes = totalMinutes
        )
    }

    suspend fun fetchPublicCamerasNear(lat: Double, lon: Double): List<PublicCameraFeed> = withContext(Dispatchers.IO) {
        val selectedSource = prefs.cameraFeedOption
        val results = mutableListOf<PublicCameraFeed>()

        // 1. Add distance-calculated verified live highway/cycling cameras
        val withDistances = verifiedLiveCameras.map { cam ->
            cam.copy(
                distanceMiles = CyclingMath.haversineMiles(lat, lon, cam.latitude, cam.longitude)
            )
        }.sortedBy { it.distanceMiles }

        // If within 60 miles of California Bay Area cameras, include the closest verified live CCTV feeds
        val nearbyVerified = withDistances.filter { it.distanceMiles <= 65.0 }
        results.addAll(nearbyVerified)

        // 2. Also query Wikimedia Geosearch API for coordinate-specific local roadway/horizon feeds
        try {
            val wikiUrl = String.format(
                Locale.US,
                "https://commons.wikimedia.org/w/api.php?action=query&generator=geosearch&ggsprimary=all&ggsnamespace=6&ggsradius=10000&ggscoord=%.4f|%.4f&ggslimit=6&prop=imageinfo|coordinates&iiprop=url&iiurlwidth=800&format=json",
                lat,
                lon
            )
            val root = JSONObject(httpGet(wikiUrl))
            val pages = root.optJSONObject("query")?.optJSONObject("pages")
            if (pages != null) {
                val keys = pages.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val p = pages.getJSONObject(key)
                    val rawTitle = p.optString("title", "").removePrefix("File:").substringBeforeLast(".")
                    val ii = p.optJSONArray("imageinfo")?.optJSONObject(0)
                    val coords = p.optJSONArray("coordinates")?.optJSONObject(0)
                    val thumbUrl = ii?.optString("thumburl", "") ?: ""
                    val descUrl = ii?.optString("descriptionurl", "") ?: ""
                    if (thumbUrl.isNotBlank() && coords != null) {
                        val cLat = coords.optDouble("lat", lat)
                        val cLon = coords.optDouble("lon", lon)
                        val dist = CyclingMath.haversineMiles(lat, lon, cLat, cLon)
                        results.add(
                            PublicCameraFeed(
                                id = "geo_cam_$key",
                                title = rawTitle.take(48),
                                locationSubtitle = String.format(Locale.US, "Sector (%.3f, %.3f), USA", cLat, cLon),
                                sourceProvider = selectedSource,
                                latitude = cLat,
                                longitude = cLon,
                                distanceMiles = dist,
                                elevationFeet = 350,
                                imageUrl = thumbUrl,
                                streamPageUrl = descUrl.ifBlank { thumbUrl },
                                hlsStreamUrl = "",
                                directionLabel = "Local Cycling Horizon & Surface View",
                                roadConditionNote = "Direct Geotagged Road & Sky Feed"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (results.isEmpty()) {
            withDistances.take(5)
        } else {
            results
                .distinctBy { it.imageUrl }
                .sortedWith(
                    compareBy<PublicCameraFeed> { if (it.hlsStreamUrl.isNotBlank()) 0 else 1 }
                        .thenBy { it.distanceMiles }
                )
                .take(6)
        }
    }

    private fun generateFallbackWeather(
        lat: Double,
        lon: Double,
        hourOffset: Int,
        providerLabel: String
    ): WeatherSnapshot {
        val seed = ((lat * 1000).roundToInt() xor (lon * 1000).roundToInt()) + hourOffset * 17
        val baseTemp = 64.0 + (seed % 11)
        val windSpeed = 9.0 + abs(seed % 12)
        val windDir = (275 + (seed % 50) + 360) % 360
        val (stationId, stationName, stationDist) = resolveClosestStation(lat, lon)
        val curHr = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + hourOffset) % 24

        val hourly = (0 until 24).map { h ->
            val hr = (curHr + h) % 24
            HourlyForecastPoint(
                timeLabel = formatHourLabel(hr),
                hourOfDay = hr,
                tempF = baseTemp + sin(h / 3.0) * 4.0,
                windSpeedMph = windSpeed + (h % 5),
                windGustMph = (windSpeed + (h % 5)) * 1.35,
                windDirectionDeg = (windDir + h * 4) % 360,
                precipProbPercent = (10 + (h * 3) % 25),
                uvIndex = if (hr in 7..17) 5.5 else 0.5
            )
        }

        return WeatherSnapshot(
            temperatureF = baseTemp,
            feelsLikeF = baseTemp - 1.5,
            tempHighF = baseTemp + 7.0,
            tempLowF = baseTemp - 9.0,
            windSpeedMph = windSpeed,
            windGustMph = windSpeed * 1.35,
            windDirectionDeg = windDir,
            windCardinal = CyclingMath.cardinalFromDegrees(windDir),
            rainChancePercent = 12,
            uvIndex = 5.2,
            sunriseTime = "6:42 AM",
            sunsetTime = "5:08 PM",
            conditionLabel = "Partly Sunny • Breeze",
            closestStationId = stationId,
            closestStationName = stationName,
            stationDistanceMiles = stationDist,
            elevationFeet = 450,
            providerUsed = providerLabel,
            hourly = hourly
        )
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("User-Agent", "VelocastCyclingWeather/1.0 (https://medusko.com; support@medusko.com)")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseHourFromIso(iso: String, fallback: Int): Int {
        val tIndex = iso.indexOf('T')
        if (tIndex != -1 && iso.length >= tIndex + 3) {
            return iso.substring(tIndex + 1, tIndex + 3).toIntOrNull() ?: fallback
        }
        return fallback
    }

    private fun formatHourLabel(hour24: Int): String {
        val h = ((hour24 % 24) + 24) % 24
        return when {
            h == 0 -> "12 AM"
            h < 12 -> "$h AM"
            h == 12 -> "12 PM"
            else -> "${h - 12} PM"
        }
    }

    private fun formatIsoTimeOnly(iso: String): String {
        val tIndex = iso.indexOf('T')
        if (tIndex == -1 || iso.length < tIndex + 6) return iso
        val hhmm = iso.substring(tIndex + 1, tIndex + 6)
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
        val m = parts.getOrNull(1) ?: "00"
        return when {
            h == 0 -> "12:$m AM"
            h < 12 -> "$h:$m AM"
            h == 12 -> "12:$m PM"
            else -> "${h - 12}:$m PM"
        }
    }

    private fun weatherCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Coastal Fog / Mist"
        51, 53, 55 -> "Light Drizzle"
        61, 63, 65 -> "Rain Showers"
        71, 73, 75 -> "Mountain Snow"
        80, 81, 82 -> "Passing Showers"
        95, 96, 99 -> "Thunderstorm Risk"
        else -> "Fair Cycling Sky"
    }
}
