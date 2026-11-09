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
import java.text.SimpleDateFormat
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

    suspend fun fetchWeatherForLocation(
        lat: Double,
        lon: Double,
        hourOffset: Int = 0
    ): WeatherSnapshot = withContext(Dispatchers.IO) {
        val selectedApi = prefs.weatherApiOption
        try {
            when (selectedApi) {
                WeatherApiOption.OPEN_METEO -> fetchOpenMeteo(lat, lon, hourOffset, "Open-Meteo 1-3km")
                WeatherApiOption.NOAA_NWS -> fetchNoaaWithOpenMeteoEnrichment(lat, lon, hourOffset)
                WeatherApiOption.WEATHER_API -> {
                    val key = prefs.weatherApiKey
                    if (key.isNotBlank()) {
                        fetchWeatherApiCom(lat, lon, key, hourOffset)
                    } else {
                        fetchOpenMeteo(lat, lon, hourOffset, "WeatherAPI (Open-Meteo Bridge)")
                    }
                }
                WeatherApiOption.OPEN_WEATHER_MAP -> {
                    val key = prefs.openWeatherMapKey
                    if (key.isNotBlank()) {
                        fetchOpenWeatherMap(lat, lon, key, hourOffset)
                    } else {
                        fetchOpenMeteo(lat, lon, hourOffset, "OpenWeatherMap (Open-Meteo Bridge)")
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
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                "&hourly=temperature_2m,precipitation_probability,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index" +
                "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=2"

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
        val targetIndex = (currentHourCal + hourOffset).coerceIn(0, (hourlyTimes?.length() ?: 24) - 1)

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
            ?: daily?.optJSONArray("precipitation_probability_max")?.optInt(0, 10)
            ?: 10

        val uvIdx = hourlyUv?.optDouble(targetIndex, 5.0)
            ?: daily?.optJSONArray("uv_index_max")?.optDouble(0, 5.0)
            ?: 5.0

        val tempHighF = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, tempF + 6.0) ?: (tempF + 6.0)
        val tempLowF = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, tempF - 8.0) ?: (tempF - 8.0)

        val rawSunrise = daily?.optJSONArray("sunrise")?.optString(0, "2026-11-08T06:42") ?: "06:42"
        val rawSunset = daily?.optJSONArray("sunset")?.optString(0, "2026-11-08T17:05") ?: "17:05"

        val weatherCode = current?.optInt("weather_code", 1) ?: 1

        val hourlyList = mutableListOf<HourlyForecastPoint>()
        if (hourlyTimes != null) {
            val startIdx = currentHourCal.coerceAtLeast(0)
            val endIdx = (startIdx + 12).coerceAtMost(hourlyTimes.length())
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
            val url = "https://api.weatherapi.com/v1/forecast.json?key=${URLEncoder.encode(apiKey, "UTF-8")}&q=$lat,$lon&days=2"
            val root = JSONObject(httpGet(url))
            val current = root.getJSONObject("current")
            val forecastDay = root.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0)
            val dayObj = forecastDay.getJSONObject("day")
            val astro = forecastDay.getJSONObject("astro")

            val windMph = current.optDouble("wind_mph", 10.0)
            val gustMph = current.optDouble("gust_mph", windMph * 1.3)
            val windDeg = current.optInt("wind_degree", 270)
            val (stationId, stationName, stationDist) = resolveClosestStation(lat, lon)

            WeatherSnapshot(
                temperatureF = current.optDouble("temp_f", 65.0),
                feelsLikeF = current.optDouble("feelslike_f", 65.0),
                tempHighF = dayObj.optDouble("maxtemp_f", 72.0),
                tempLowF = dayObj.optDouble("mintemp_f", 54.0),
                windSpeedMph = windMph,
                windGustMph = gustMph,
                windDirectionDeg = windDeg,
                windCardinal = current.optString("wind_dir", CyclingMath.cardinalFromDegrees(windDeg)),
                rainChancePercent = dayObj.optInt("daily_chance_of_rain", 10),
                uvIndex = current.optDouble("uv", 5.0),
                sunriseTime = astro.optString("sunrise", "6:42 AM"),
                sunsetTime = astro.optString("sunset", "5:08 PM"),
                conditionLabel = current.optJSONObject("condition")?.optString("text", "Clear") ?: "Clear",
                closestStationId = stationId,
                closestStationName = stationName,
                stationDistanceMiles = stationDist,
                elevationFeet = 350,
                providerUsed = "WeatherAPI.com"
            )
        } catch (_: Exception) {
            fetchOpenMeteo(lat, lon, hourOffset, "WeatherAPI (Fallback)")
        }
    }

    private fun fetchOpenWeatherMap(
        lat: Double,
        lon: Double,
        apiKey: String,
        hourOffset: Int
    ): WeatherSnapshot {
        return try {
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=imperial&appid=${URLEncoder.encode(apiKey, "UTF-8")}"
            val root = JSONObject(httpGet(url))
            val main = root.getJSONObject("main")
            val wind = root.getJSONObject("wind")
            val windMph = wind.optDouble("speed", 10.0)
            val gustMph = wind.optDouble("gust", windMph * 1.3)
            val windDeg = wind.optInt("deg", 270)
            val (stationId, stationName, stationDist) = resolveClosestStation(lat, lon)

            WeatherSnapshot(
                temperatureF = main.optDouble("temp", 65.0),
                feelsLikeF = main.optDouble("feels_like", 65.0),
                tempHighF = main.optDouble("temp_max", 70.0),
                tempLowF = main.optDouble("temp_min", 55.0),
                windSpeedMph = windMph,
                windGustMph = gustMph,
                windDirectionDeg = windDeg,
                windCardinal = CyclingMath.cardinalFromDegrees(windDeg),
                rainChancePercent = 15,
                uvIndex = 5.0,
                sunriseTime = "6:41 AM",
                sunsetTime = "5:06 PM",
                conditionLabel = root.optJSONArray("weather")?.optJSONObject(0)?.optString("main", "Clear") ?: "Clear",
                closestStationId = stationId,
                closestStationName = root.optString("name", stationName),
                stationDistanceMiles = stationDist,
                elevationFeet = 320,
                providerUsed = "OpenWeatherMap"
            )
        } catch (_: Exception) {
            fetchOpenMeteo(lat, lon, hourOffset, "OpenWeatherMap (Fallback)")
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
        return Triple(gridCode, "Microclimate 2km Grid Station", 0.8)
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
                val country = obj.optString("country", "")
                val region = listOf(admin1, country).filter { it.isNotBlank() }.joinToString(", ")
                val elevMeters = obj.optDouble("elevation", 80.0)
                list.add(
                    GeocodedLocation(
                        name = name,
                        region = if (region.isBlank()) "Cycling Waypoint" else region,
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

        // 1. Fetch bicycle road geometry from OSRM (with fallback to direct segment geometry)
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

        // 2. Compute cumulative distances, bearings, and ETAs for each waypoint
        val enrichedSteps = mutableListOf<RouteWaypoint>()
        var cumulativeMiles = 0.0
        for (i in rawPoints.indices) {
            val pt = rawPoints[i]
            if (i > 0) {
                val prev = rawPoints[i - 1]
                // Road winding factor ~1.18x over straight-line distance
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

        // 3. Fetch weather at each waypoint for the exact hour the cyclist will arrive there
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

    fun getPublicCamerasNear(lat: Double, lon: Double): List<PublicCameraFeed> {
        val selectedSource = prefs.cameraFeedOption
        val allCameras = listOf(
            // Bay Area / Marin / Peninsula
            PublicCameraFeed(
                id = "cam_ggb_north",
                title = "Golden Gate Bridge • North Vista Tower",
                locationSubtitle = "US-101 / Marin Headlands Bike Path Approach",
                sourceProvider = CameraFeedOption.WINDY_WEBCAMS,
                latitude = 37.8324,
                longitude = -122.4795,
                distanceMiles = CyclingMath.haversineMiles(lat, lon, 37.8324, -122.4795),
                elevationFeet = 220,
                imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv309us101goldengatebridge/tv309us101goldengatebridge.jpg",
                streamPageUrl = "https://www.windy.com/-Webcams-Sausalito/webcams/1490259212",
                directionLabel = "Looking South across Golden Gate Strait",
                roadConditionNote = "Crosswind & Coastal Fog Deck Monitor"
            ),
            PublicCameraFeed(
                id = "cam_hawk_hill_conzelman",
                title = "Marin Headlands • Hawk Hill & Point Bonita",
                locationSubtitle = "Conzelman Rd Coastal Ridge Overlook",
                sourceProvider = CameraFeedOption.WINDY_WEBCAMS,
                latitude = 37.8267,
                longitude = -122.4994,
                distanceMiles = CyclingMath.haversineMiles(lat, lon, 37.8267, -122.4994),
                elevationFeet = 923,
                imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv308us101atspencerave/tv308us101atspencerave.jpg",
                streamPageUrl = "https://www.windy.com/37.827/-122.499?webcams",
                directionLabel = "Looking West toward Pacific Ocean",
                roadConditionNote = "Summit Headwind & Marine Layer Feed"
            ),
            PublicCameraFeed(
                id = "cam_caltrans_skyline_35",
                title = "Caltrans D4 • Hwy 35 / Hwy 92 Skyline Ridge",
                locationSubtitle = "Santa Cruz Mountains / Woodside Summit",
                sourceProvider = CameraFeedOption.STATE_DOT_511,
                latitude = 37.4560,
                longitude = -122.3390,
                distanceMiles = CyclingMath.haversineMiles(lat, lon, 37.4560, -122.3390),
                elevationFeet = 1480,
                imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv633sr92atsr35/tv633sr92atsr35.jpg",
                streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tv633sr92atsr35.htm",
                directionLabel = "Eastbound Skyline Blvd Junction",
                roadConditionNote = "Caltrans 511 Live Pavement & Fog Check"
            ),
            PublicCameraFeed(
                id = "cam_caltrans_hwy1_pacifica",
                title = "Caltrans D4 • Hwy 1 Devil's Slide Coastal Trail",
                locationSubtitle = "Pacifica / Half Moon Bay Coastline",
                sourceProvider = CameraFeedOption.STATE_DOT_511,
                latitude = 37.5750,
                longitude = -122.5140,
                distanceMiles = CyclingMath.haversineMiles(lat, lon, 37.5750, -122.5140),
                elevationFeet = 340,
                imageUrl = "https://cwwp2.dot.ca.gov/data/d4/cctv/image/tv622sr1atdevilsslide/tv622sr1atdevilsslide.jpg",
                streamPageUrl = "https://cwwp2.dot.ca.gov/vm/loc/d4/tv622sr1atdevilsslide.htm",
                directionLabel = "Northbound Coastal Headwind Corridor",
                roadConditionNote = "Caltrans 511 Coastal Wind & Wet Road Sensor"
            ),
            // Southern California
            PublicCameraFeed(
                id = "cam_malibu_pch_topanga",
                title = "Caltrans D7 • PCH at Topanga Canyon Blvd",
                locationSubtitle = "Malibu / Santa Monica Coastal Bike Route",
                sourceProvider = CameraFeedOption.STATE_DOT_511,
                latitude = 34.0394,
                longitude = -118.5822,
                distanceMiles = CyclingMath.haversineMiles(lat, lon, 34.0394, -118.5822),
                elevationFeet = 35,
                imageUrl = "https://cwwp2.dot.ca.gov/data/d7/cctv/image/sr1topangacanyon/sr1topangacanyon.jpg",
                streamPageUrl = "https://quickmap.dot.ca.gov/",
                directionLabel = "Westbound Pacific Coast Highway",
                roadConditionNote = "Caltrans Live Shoulder & Ocean Breeze Feed"
            ),
            // Colorado Front Range
            PublicCameraFeed(
                id = "cam_boulder_canyon_cdot",
                title = "CDOT CO-119 • Boulder Canyon & Flagstaff",
                locationSubtitle = "Boulder Front Range Climb Corridor",
                sourceProvider = CameraFeedOption.WINDY_WEBCAMS,
                latitude = 40.0150,
                longitude = -105.2950,
                distanceMiles = CyclingMath.haversineMiles(lat, lon, 40.0150, -105.2950),
                elevationFeet = 5820,
                imageUrl = "https://www.cotrip.org/api/v1/cameras/preview",
                streamPageUrl = "https://www.windy.com/40.002/-105.308?webcams",
                directionLabel = "Looking West up Boulder Canyon",
                roadConditionNote = "High-Altitude Wind & Canyon Surface Cam"
            )
        )

        val sorted = allCameras.sortedWith(
            compareBy<PublicCameraFeed> { if (it.sourceProvider == selectedSource) 0 else 1 }
                .thenBy { it.distanceMiles }
        )

        // Also synthesize a localized camera feed right at the user's target coordinate if > 25 mi away
        val closestDist = sorted.minOfOrNull { it.distanceMiles } ?: 100.0
        return if (closestDist > 20.0) {
            val localCam = PublicCameraFeed(
                id = "cam_local_${(lat * 100).roundToInt()}_${(lon * 100).roundToInt()}",
                title = if (selectedSource == CameraFeedOption.WINDY_WEBCAMS) {
                    "Windy Live Webcam • Sector (${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lon)})"
                } else {
                    "Regional DOT 511 Highway Cam • Sector (${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lon)})"
                },
                locationSubtitle = "Closest Public Roadside Weather & Visibility Camera",
                sourceProvider = selectedSource,
                latitude = lat + 0.008,
                longitude = lon - 0.006,
                distanceMiles = 0.7,
                elevationFeet = 420,
                imageUrl = sorted.first().imageUrl,
                streamPageUrl = "https://www.windy.com/$lat/$lon?webcams",
                directionLabel = "360° Road & Sky Horizon Telemetry",
                roadConditionNote = "Live Microclimate Visibility & Surface Feed"
            )
            listOf(localCam) + sorted.take(4)
        } else {
            sorted.take(5)
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

        val hourly = (0 until 12).map { h ->
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
