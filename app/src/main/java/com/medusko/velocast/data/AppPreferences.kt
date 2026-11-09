package com.medusko.velocast.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.medusko.velocast.model.*
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("velocast_prefs", Context.MODE_PRIVATE)

    var mappingOption: MappingOption
        get() = MappingOption.fromId(prefs.getString("mapping_option", MappingOption.OPEN_FREE_MAP.id))
        set(value) = prefs.edit().putString("mapping_option", value.id).apply()

    var weatherApiOption: WeatherApiOption
        get() = WeatherApiOption.fromId(prefs.getString("weather_api_option", WeatherApiOption.OPEN_METEO.id))
        set(value) = prefs.edit().putString("weather_api_option", value.id).apply()

    var cameraFeedOption: CameraFeedOption
        get() = CameraFeedOption.fromId(prefs.getString("camera_feed_option", CameraFeedOption.WINDY_WEBCAMS.id))
        set(value) = prefs.edit().putString("camera_feed_option", value.id).apply()

    var unitSystem: UnitSystem
        get() = UnitSystem.fromId(prefs.getString("unit_system", UnitSystem.IMPERIAL.id))
        set(value) = prefs.edit().putString("unit_system", value.id).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", true)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    var weatherApiKey: String
        get() = prefs.getString("weather_api_key", "") ?: ""
        set(value) = prefs.edit().putString("weather_api_key", value.trim()).apply()

    var openWeatherMapKey: String
        get() = prefs.getString("open_weather_map_key", "") ?: ""
        set(value) = prefs.edit().putString("open_weather_map_key", value.trim()).apply()

    var windyApiKey: String
        get() = prefs.getString("windy_api_key", "") ?: ""
        set(value) = prefs.edit().putString("windy_api_key", value.trim()).apply()

    var appIconStyle: AppIconStyle
        get() = AppIconStyle.fromId(prefs.getString("app_icon_style", AppIconStyle.GOLD.id))
        set(value) {
            prefs.edit().putString("app_icon_style", value.id).apply()
            applyLauncherIconAlias(value)
        }

    private fun applyLauncherIconAlias(style: AppIconStyle) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val goldComp = ComponentName(pkg, "com.medusko.velocast.MainActivityDefault")
            val classicComp = ComponentName(pkg, "com.medusko.velocast.MainActivityClassic")

            if (style == AppIconStyle.GOLD) {
                pm.setComponentEnabledSetting(
                    goldComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    classicComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    classicComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    goldComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {
        }
    }

    fun loadSavedPlaces(): List<CyclingPlace> {
        val raw = prefs.getString("saved_places_json", null)
        if (raw.isNullOrBlank()) {
            val defaults = defaultCyclingPlaces()
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
                        region = obj.optString("region", "Cycling Route"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        elevationFeet = obj.optInt("elevationFeet", 150),
                        tags = tags
                    )
                )
            }
            if (list.isEmpty()) defaultCyclingPlaces() else list
        } catch (_: Exception) {
            defaultCyclingPlaces()
        }
    }

    fun savePlaces(places: List<CyclingPlace>) {
        val arr = JSONArray()
        for (p in places) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("region", p.region)
                put("latitude", p.latitude)
                put("longitude", p.longitude)
                put("elevationFeet", p.elevationFeet)
                val tArr = JSONArray()
                p.tags.forEach { tArr.put(it) }
                put("tags", tArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_places_json", arr.toString()).apply()
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
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
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
                list.add(
                    SavedRide(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
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
        fun defaultCyclingPlaces(): List<CyclingPlace> = listOf(
            CyclingPlace(
                id = "marin_hawk_hill",
                name = "Marin Headlands • Hawk Hill",
                region = "Golden Gate National Recreation Area, CA",
                latitude = 37.8267,
                longitude = -122.4994,
                elevationFeet = 923,
                tags = listOf("Coastal Headwinds", "Fog Microclimate", "923 ft Summit")
            ),
            CyclingPlace(
                id = "skyline_old_la_honda",
                name = "Skyline Blvd • Old La Honda",
                region = "Woodside / Santa Cruz Mountains, CA",
                latitude = 37.3728,
                longitude = -122.2525,
                elevationFeet = 1790,
                tags = listOf("Redwood Climb", "Valley Inversion", "1,790 ft Ridge")
            ),
            CyclingPlace(
                id = "mt_tam_east_peak",
                name = "Mt. Tamalpais • Seven Sisters",
                region = "Mill Valley / Marin County, CA",
                latitude = 37.9235,
                longitude = -122.5965,
                elevationFeet = 2571,
                tags = listOf("Alpine Ridge", "High UV Exposure", "Gusty Summit")
            ),
            CyclingPlace(
                id = "griffith_observatory",
                name = "Griffith Park • Mt. Hollywood Dr",
                region = "Los Angeles, CA",
                latitude = 34.1184,
                longitude = -118.3004,
                elevationFeet = 1134,
                tags = listOf("Basin Thermal Wind", "Canyon Climb", "Sunrise Loop")
            ),
            CyclingPlace(
                id = "boulder_flagstaff",
                name = "Flagstaff Mountain • Super Flag",
                region = "Boulder Front Range, CO",
                latitude = 40.0017,
                longitude = -105.3075,
                elevationFeet = 6983,
                tags = listOf("6,983 ft Altitude", "Chinook Gusts", "High UV")
            )
        )

        fun defaultSavedRides(): List<SavedRide> = listOf(
            SavedRide(
                id = "ride_hawk_hill_loop",
                name = "Golden Gate & Hawk Hill Coastal Loop",
                region = "SF Crissy Field → Marin Headlands → Rodeo Beach",
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
                region = "Woodside → Skyline Ridge → Stage Rd Coastal Run",
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
            ),
            SavedRide(
                id = "ride_malibu_latigo",
                name = "PCH Coastal & Latigo Canyon Climb",
                region = "Santa Monica → Malibu Lagoon → Latigo Summit",
                totalDistanceMiles = 29.2,
                estimatedMinutes = 110,
                averageSpeedMph = 16.0,
                waypoints = listOf(
                    RouteWaypoint(0, "Santa Monica Palisades", 34.0195, -118.5011, 0.0, "Start", 280),
                    RouteWaypoint(1, "Topanga Beach PCH", 34.0394, -118.5822, 6.2, "+23m", 275),
                    RouteWaypoint(2, "Malibu Pier", 34.0368, -118.6778, 13.5, "+51m", 285),
                    RouteWaypoint(3, "Latigo Canyon Mid-Climb", 34.0680, -118.7580, 21.8, "+82m", 340),
                    RouteWaypoint(4, "Latigo Summit Overlook", 34.0985, -118.7862, 29.2, "+110m", 355)
                )
            )
        )
    }
}
