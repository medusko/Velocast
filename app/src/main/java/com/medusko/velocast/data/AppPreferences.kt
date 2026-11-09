package com.medusko.velocast.data

import android.content.Context
import android.content.SharedPreferences
import com.medusko.velocast.model.*
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("velocast_prefs", Context.MODE_PRIVATE)

    var mappingOption: MappingOption
        get() = MappingOption.fromId(prefs.getString("mapping_option", MappingOption.OPEN_FREE_MAP.id))
        set(value) = prefs.edit().putString("mapping_option", value.id).apply()

    var mapStyleOption: MapStyleOption
        get() = MapStyleOption.fromId(prefs.getString("map_style_option", MapStyleOption.REGULAR.id))
        set(value) = prefs.edit().putString("map_style_option", value.id).apply()

    var weatherApiOption: WeatherApiOption
        get() = WeatherApiOption.fromId(prefs.getString("weather_api_option", WeatherApiOption.OPEN_METEO.id))
        set(value) = prefs.edit().putString("weather_api_option", value.id).apply()

    var cameraFeedOption: CameraFeedOption
        get() = CameraFeedOption.fromId(prefs.getString("camera_feed_option", CameraFeedOption.WINDY_WEBCAMS.id))
        set(value) = prefs.edit().putString("camera_feed_option", value.id).apply()

    var tempUnit: TempUnit
        get() = TempUnit.fromId(prefs.getString("unit_temp", TempUnit.FAHRENHEIT.id))
        set(value) = prefs.edit().putString("unit_temp", value.id).apply()

    var speedUnit: SpeedUnit
        get() = SpeedUnit.fromId(prefs.getString("unit_speed", SpeedUnit.MPH.id))
        set(value) = prefs.edit().putString("unit_speed", value.id).apply()

    var distanceUnit: DistanceUnit
        get() = DistanceUnit.fromId(prefs.getString("unit_distance", DistanceUnit.MILES.id))
        set(value) = prefs.edit().putString("unit_distance", value.id).apply()

    var elevationUnit: ElevationUnit
        get() = ElevationUnit.fromId(prefs.getString("unit_elevation", ElevationUnit.FEET.id))
        set(value) = prefs.edit().putString("unit_elevation", value.id).apply()

    var unitPreferences: UnitPreferences
        get() = UnitPreferences(
            tempUnit = tempUnit,
            speedUnit = speedUnit,
            distanceUnit = distanceUnit,
            elevationUnit = elevationUnit
        )
        set(value) {
            tempUnit = value.tempUnit
            speedUnit = value.speedUnit
            distanceUnit = value.distanceUnit
            elevationUnit = value.elevationUnit
        }

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", true)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    var confirmWaypointDelete: Boolean
        get() = prefs.getBoolean("confirm_waypoint_delete", false)
        set(value) = prefs.edit().putBoolean("confirm_waypoint_delete", value).apply()

    var hasResolvedInitialGpsPlaces: Boolean
        get() = prefs.getBoolean("has_resolved_gps_places_v2", false)
        set(value) = prefs.edit().putBoolean("has_resolved_gps_places_v2", value).apply()

    var weatherApiKey: String
        get() = prefs.getString("weather_api_key", "") ?: ""
        set(value) = prefs.edit().putString("weather_api_key", value.trim()).apply()

    var openWeatherMapKey: String
        get() = prefs.getString("open_weather_map_key", "") ?: ""
        set(value) = prefs.edit().putString("open_weather_map_key", value.trim()).apply()

    var windyApiKey: String
        get() = prefs.getString("windy_api_key", "") ?: ""
        set(value) = prefs.edit().putString("windy_api_key", value.trim()).apply()

    fun loadSavedPlaces(): List<CyclingPlace> {
        val raw = prefs.getString("saved_places_json_v2", null)
        if (raw.isNullOrBlank()) {
            val defaults = CyclingSuburbsDatabase.fallbackDefaultPlaces
            savePlaces(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CyclingPlace>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tagsArr = obj.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        tags.add(tagsArr.getString(t))
                    }
                }
                list.add(
                    CyclingPlace(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        region = CyclingMath.ensureCountryInRegion(obj.optString("region", "CA, USA")),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        elevationFeet = obj.optInt("elevationFeet", 150),
                        tags = tags
                    )
                )
            }
            if (list.isEmpty()) CyclingSuburbsDatabase.fallbackDefaultPlaces else list
        } catch (_: Exception) {
            CyclingSuburbsDatabase.fallbackDefaultPlaces
        }
    }

    fun savePlaces(places: List<CyclingPlace>) {
        val arr = JSONArray()
        for (p in places) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("region", CyclingMath.ensureCountryInRegion(p.region))
                put("latitude", p.latitude)
                put("longitude", p.longitude)
                put("elevationFeet", p.elevationFeet)
                val tArr = JSONArray()
                p.tags.forEach { tArr.put(it) }
                put("tags", tArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_places_json_v2", arr.toString()).apply()
    }

    fun loadSavedRides(): List<SavedRide> {
        val raw = prefs.getString("saved_rides_json", null)
        if (raw.isNullOrBlank()) {
            val defaults = defaultSavedRides()
            saveRides(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SavedRide>()
            val currentPlaces = loadSavedPlaces()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                // Remove PCH Coastal & Latigo Canyon Climb per user request
                if (id == "ride_malibu_latigo" || name.contains("Latigo", ignoreCase = true)) {
                    continue
                }
                val wpArr = obj.getJSONArray("waypoints")
                val waypoints = mutableListOf<RouteWaypoint>()
                for (w in 0 until wpArr.length()) {
                    val wObj = wpArr.getJSONObject(w)
                    waypoints.add(
                        RouteWaypoint(
                            index = wObj.optInt("index", w),
                            label = wObj.getString("label"),
                            latitude = wObj.getDouble("latitude"),
                            longitude = wObj.getDouble("longitude"),
                            distanceFromStartMiles = wObj.optDouble("distanceFromStartMiles", 0.0),
                            etaLabel = wObj.optString("etaLabel", ""),
                            bearingDeg = wObj.optInt("bearingDeg", 0)
                        )
                    )
                }
                val firstPt = waypoints.firstOrNull()
                val closestPlace = obj.optString("closestPlaceName", "").ifBlank {
                    if (firstPt != null) {
                        CyclingSuburbsDatabase.findClosestPlaceName(firstPt.latitude, firstPt.longitude, currentPlaces)
                    } else {
                        "Marin Headlands • Hawk Hill (Sausalito, CA, USA)"
                    }
                }
                list.add(
                    SavedRide(
                        id = id,
                        name = name,
                        closestPlaceName = closestPlace,
                        region = obj.optString("region", "Custom Cycling Route"),
                        totalDistanceMiles = obj.optDouble("totalDistanceMiles", 15.0),
                        estimatedMinutes = obj.optInt("estimatedMinutes", 60),
                        averageSpeedMph = obj.optDouble("averageSpeedMph", 15.0),
                        waypoints = waypoints,
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) defaultSavedRides() else list
        } catch (_: Exception) {
            defaultSavedRides()
        }
    }

    fun saveRides(rides: List<SavedRide>) {
        val arr = JSONArray()
        for (r in rides) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("closestPlaceName", r.closestPlaceName)
                put("region", r.region)
                put("totalDistanceMiles", r.totalDistanceMiles)
                put("estimatedMinutes", r.estimatedMinutes)
                put("averageSpeedMph", r.averageSpeedMph)
                put("createdAtMillis", r.createdAtMillis)
                val wpArr = JSONArray()
                r.waypoints.forEach { wp ->
                    wpArr.put(
                        JSONObject().apply {
                            put("index", wp.index)
                            put("label", wp.label)
                            put("latitude", wp.latitude)
                            put("longitude", wp.longitude)
                            put("distanceFromStartMiles", wp.distanceFromStartMiles)
                            put("etaLabel", wp.etaLabel)
                            put("bearingDeg", wp.bearingDeg)
                        }
                    )
                }
                put("waypoints", wpArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_rides_json", arr.toString()).apply()
    }

    companion object {
        fun defaultSavedRides(): List<SavedRide> = listOf(
            SavedRide(
                id = "ride_hawk_hill_loop",
                name = "Golden Gate & Hawk Hill Coastal Loop",
                closestPlaceName = "Marin Headlands • Hawk Hill (Sausalito, CA, USA)",
                region = "San Francisco, CA, USA → Sausalito, CA, USA",
                totalDistanceMiles = 18.4,
                estimatedMinutes = 74,
                averageSpeedMph = 15.0,
                waypoints = listOf(
                    RouteWaypoint(0, "Crissy Field Start", 37.8039, -122.4648, 0.0, "Start", 335),
                    RouteWaypoint(1, "Golden Gate North Tower", 37.8324, -122.4795, 3.2, "+13m", 290),
                    RouteWaypoint(2, "Hawk Hill Summit", 37.8267, -122.4994, 6.8, "+27m", 245),
                    RouteWaypoint(3, "Point Bonita Lighthouse", 37.8156, -122.5295, 11.4, "+46m", 20),
                    RouteWaypoint(4, "Sausalito Ferry Finish", 37.8591, -122.4853, 18.4, "+74m", 85)
                )
            ),
            SavedRide(
                id = "ride_skyline_coastal",
                name = "Old La Honda to Pescadero Coast",
                closestPlaceName = "Skyline Blvd • Old La Honda (Woodside, CA, USA)",
                region = "Woodside, CA, USA → Pescadero, CA, USA",
                totalDistanceMiles = 34.6,
                estimatedMinutes = 138,
                averageSpeedMph = 15.0,
                waypoints = listOf(
                    RouteWaypoint(0, "Woodside Bakery Start", 37.4299, -122.2539, 0.0, "Start", 215),
                    RouteWaypoint(1, "Old La Honda Summit", 37.3728, -122.2525, 6.4, "+26m", 230),
                    RouteWaypoint(2, "La Honda Redwood Grove", 37.3190, -122.2742, 13.8, "+55m", 245),
                    RouteWaypoint(3, "San Gregorio Beach Hwy 1", 37.3263, -122.4008, 23.5, "+94m", 175),
                    RouteWaypoint(4, "Pescadero Downtown", 37.2552, -122.3830, 34.6, "+138m", 110)
                )
            )
        )
    }
}
