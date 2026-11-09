package com.medusko.velocast.model

import java.util.Locale
import kotlin.math.*

enum class MappingOption(
    val id: String,
    val title: String,
    val badge: String,
    val description: String
) {
    OPEN_FREE_MAP(
        id = "openfreemap",
        title = "OpenFreeMap",
        badge = "Default • Vector",
        description = "High-contrast cartographic basemaps with Street, Topograph, and Regular layer styles."
    ),
    OSMDROID(
        id = "osmdroid",
        title = "OSMDroid (OpenStreetMap)",
        badge = "Standard • Cycling",
        description = "Native OpenStreetMap, CyclOSM, and OpenTopoMap tile renderer with device caching."
    );

    companion object {
        fun fromId(id: String?): MappingOption =
            entries.firstOrNull { it.id == id } ?: OPEN_FREE_MAP
    }
}

enum class MapStyleOption(
    val id: String,
    val label: String
) {
    REGULAR("regular", "Regular"),
    STREET("street", "Street"),
    TOPOGRAPHIC("topograph", "Topograph");

    companion object {
        fun fromId(id: String?): MapStyleOption =
            entries.firstOrNull { it.id == id } ?: REGULAR
    }
}

enum class WeatherApiOption(
    val id: String,
    val title: String,
    val badge: String,
    val description: String,
    val requiresOptionalKey: Boolean
) {
    OPEN_METEO(
        id = "open_meteo",
        title = "Open-Meteo Microclimate",
        badge = "Default • Best",
        description = "Combines NOAA HRRR, DWD ICON & ECMWF models for high-resolution microclimate accuracy, hourly wind vectors, UV, and sun times.",
        requiresOptionalKey = false
    ),
    NOAA_NWS(
        id = "noaa_nws",
        title = "NOAA / NWS Station API",
        badge = "Physical Station",
        description = "Pinpoints the exact closest physical observation station (METAR/Mesonet) & official NWS gridpoint forecasts across the US.",
        requiresOptionalKey = false
    ),
    WEATHER_API(
        id = "weather_api",
        title = "WeatherAPI.com",
        badge = "Astronomy & UV",
        description = "Includes astronomy (sunrise/sunset), UV index, and hourly wind telemetry.",
        requiresOptionalKey = true
    ),
    OPEN_WEATHER_MAP(
        id = "open_weather_map",
        title = "OpenWeatherMap",
        badge = "One Call 3.0 / 2.5",
        description = "Global weather, wind vectors, and UV index forecast engine.",
        requiresOptionalKey = true
    );

    fun localizedTitle(distUnit: DistanceUnit): String {
        return if (this == OPEN_METEO) {
            val res = CyclingMath.formatResolutionSpan(distUnit)
            "Open-Meteo ($res Microclimate)"
        } else {
            title
        }
    }

    fun localizedDescription(distUnit: DistanceUnit): String {
        return if (this == OPEN_METEO) {
            val hrrr = if (distUnit == DistanceUnit.MILES) "1.9 mi" else "3 km"
            val icon = if (distUnit == DistanceUnit.MILES) "1.2 mi" else "2 km"
            "Combines NOAA HRRR $hrrr, DWD ICON $icon & ECMWF models for high-resolution microclimate accuracy."
        } else {
            description
        }
    }

    companion object {
        fun fromId(id: String?): WeatherApiOption =
            entries.firstOrNull { it.id == id } ?: OPEN_METEO
    }
}

enum class CameraFeedOption(
    val id: String,
    val title: String,
    val badge: String,
    val description: String
) {
    WINDY_WEBCAMS(
        id = "windy_webcams",
        title = "Windy Webcams API",
        badge = "Default • Global",
        description = "Global scenic, coastal, and mountain pass cycling webcams with live daylight/weather snapshots."
    ),
    STATE_DOT_511(
        id = "state_dot_511",
        title = "State & Regional DOT 511 Feeds",
        badge = "Highway & Pass Cams",
        description = "Live Caltrans, WSDOT, CDOT, NYSDOT & 511 roadside traffic and mountain pass camera snapshots."
    );

    companion object {
        fun fromId(id: String?): CameraFeedOption =
            entries.firstOrNull { it.id == id } ?: WINDY_WEBCAMS
    }
}

enum class TempUnit(val id: String, val label: String, val shortLabel: String) {
    FAHRENHEIT("f", "Fahrenheit (°F)", "°F"),
    CELSIUS("c", "Celsius (°C)", "°C");

    companion object {
        fun fromId(id: String?): TempUnit = entries.firstOrNull { it.id == id } ?: FAHRENHEIT
    }
}

enum class SpeedUnit(val id: String, val label: String, val shortLabel: String) {
    MPH("mph", "Miles/hr (mph)", "mph"),
    KMH("kmh", "Kilometers/hr (km/h)", "km/h");

    companion object {
        fun fromId(id: String?): SpeedUnit = entries.firstOrNull { it.id == id } ?: MPH
    }
}

enum class DistanceUnit(val id: String, val label: String, val shortLabel: String) {
    MILES("mi", "Miles (mi)", "mi"),
    KILOMETERS("km", "Kilometers (km)", "km");

    companion object {
        fun fromId(id: String?): DistanceUnit = entries.firstOrNull { it.id == id } ?: MILES
    }
}

enum class ElevationUnit(val id: String, val label: String, val shortLabel: String) {
    FEET("ft", "Feet (ft)", "ft"),
    METERS("m", "Meters (m)", "m");

    companion object {
        fun fromId(id: String?): ElevationUnit = entries.firstOrNull { it.id == id } ?: FEET
    }
}

data class UnitPreferences(
    val tempUnit: TempUnit = TempUnit.FAHRENHEIT,
    val speedUnit: SpeedUnit = SpeedUnit.MPH,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val elevationUnit: ElevationUnit = ElevationUnit.FEET
)

enum class WindImpact(val label: String) {
    TAILWIND("Tailwind"),
    HEADWIND("Headwind"),
    CROSSWIND("Crosswind"),
    CALM("Calm Wind")
}

data class HourlyForecastPoint(
    val timeLabel: String,
    val hourOfDay: Int,
    val tempF: Double,
    val windSpeedMph: Double,
    val windGustMph: Double,
    val windDirectionDeg: Int,
    val precipProbPercent: Int,
    val uvIndex: Double
)

data class WeatherSnapshot(
    val temperatureF: Double,
    val feelsLikeF: Double,
    val tempHighF: Double,
    val tempLowF: Double,
    val windSpeedMph: Double,
    val windGustMph: Double,
    val windDirectionDeg: Int,
    val windCardinal: String,
    val rainChancePercent: Int,
    val uvIndex: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val conditionLabel: String,
    val closestStationId: String,
    val closestStationName: String,
    val stationDistanceMiles: Double,
    val elevationFeet: Int,
    val providerUsed: String,
    val hourly: List<HourlyForecastPoint> = emptyList(),
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

data class CyclingPlace(
    val id: String,
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val elevationFeet: Int,
    val tags: List<String> = emptyList(),
    val weather: WeatherSnapshot? = null
)

data class RouteWaypoint(
    val index: Int,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val distanceFromStartMiles: Double = 0.0,
    val etaLabel: String = "Start",
    val bearingDeg: Int = 0,
    val windImpact: WindImpact = WindImpact.CALM,
    val headwindComponentMph: Double = 0.0,
    val crosswindComponentMph: Double = 0.0,
    val weather: WeatherSnapshot? = null
)

data class SavedRide(
    val id: String,
    val name: String,
    val closestPlaceName: String,
    val region: String,
    val totalDistanceMiles: Double,
    val estimatedMinutes: Int,
    val averageSpeedMph: Double,
    val waypoints: List<RouteWaypoint>,
    val routeGeometry: List<Pair<Double, Double>> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class PublicCameraFeed(
    val id: String,
    val title: String,
    val locationSubtitle: String,
    val sourceProvider: CameraFeedOption,
    val latitude: Double,
    val longitude: Double,
    val distanceMiles: Double,
    val elevationFeet: Int,
    val imageUrl: String,
    val streamPageUrl: String,
    val hlsStreamUrl: String = "",
    val directionLabel: String,
    val refreshIntervalSec: Int = 30,
    val roadConditionNote: String = "Live Road Surface & Wind Feed"
)

object CyclingMath {

    fun cardinalFromDegrees(deg: Int): String {
        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val normalized = ((deg % 360) + 360) % 360
        val idx = ((normalized + 11.25) / 22.5).toInt() % 16
        return dirs[idx]
    }

    fun haversineMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rMiles = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        return rMiles * c
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLambda = Math.toRadians(lon2 - lon1)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val theta = Math.toDegrees(atan2(y, x))
        return (((theta + 360.0) % 360.0).roundToInt()) % 360
    }

    fun analyzeWindOnSegment(
        riderBearingDeg: Int,
        windFromDeg: Int,
        windSpeedMph: Double
    ): Triple<WindImpact, Double, Double> {
        if (windSpeedMph < 3.5) {
            return Triple(WindImpact.CALM, 0.0, 0.0)
        }
        val angleDiffRad = Math.toRadians((windFromDeg - riderBearingDeg).toDouble())
        val headwind = windSpeedMph * cos(angleDiffRad)
        val crosswind = abs(windSpeedMph * sin(angleDiffRad))

        val impact = when {
            headwind >= 4.0 && headwind >= crosswind * 0.65 -> WindImpact.HEADWIND
            headwind <= -4.0 && abs(headwind) >= crosswind * 0.65 -> WindImpact.TAILWIND
            crosswind >= 4.5 -> WindImpact.CROSSWIND
            headwind > 0 -> WindImpact.HEADWIND
            else -> WindImpact.TAILWIND
        }
        return Triple(impact, headwind, crosswind)
    }

    fun formatTemp(tempF: Double, units: UnitPreferences): String {
        return if (units.tempUnit == TempUnit.FAHRENHEIT) {
            "${tempF.roundToInt()}°F"
        } else {
            val c = (tempF - 32.0) * 5.0 / 9.0
            "${c.roundToInt()}°C"
        }
    }

    fun formatSpeed(mph: Double, units: UnitPreferences): String {
        return if (units.speedUnit == SpeedUnit.MPH) {
            "${mph.roundToInt()} mph"
        } else {
            val kmh = mph * 1.60934
            "${kmh.roundToInt()} km/h"
        }
    }

    fun formatDistance(miles: Double, units: UnitPreferences): String {
        return if (units.distanceUnit == DistanceUnit.MILES) {
            String.format(Locale.US, "%.1f mi", miles)
        } else {
            String.format(Locale.US, "%.1f km", miles * 1.60934)
        }
    }

    fun formatElevation(feet: Int, units: UnitPreferences): String {
        return if (units.elevationUnit == ElevationUnit.FEET) {
            "$feet ft"
        } else {
            "${(feet * 0.3048).roundToInt()} m"
        }
    }

    fun formatResolutionSpan(distUnit: DistanceUnit): String {
        return if (distUnit == DistanceUnit.MILES) {
            "0.6–1.9 mi"
        } else {
            "1–3 km"
        }
    }

    fun ensureCountryInRegion(region: String): String {
        val trimmed = region.trim()
        if (trimmed.isEmpty()) return "USA"
        if (trimmed.endsWith("USA", ignoreCase = true) ||
            trimmed.endsWith("United States", ignoreCase = true) ||
            trimmed.contains(", ") && trimmed.substringAfterLast(", ").length > 3
        ) {
            return trimmed.replace("United States", "USA")
        }
        return "$trimmed, USA"
    }

    fun uvSeverityLabel(uv: Double): String = when {
        uv < 3.0 -> "Low"
        uv < 6.0 -> "Moderate"
        uv < 8.0 -> "High"
        uv < 11.0 -> "Very High"
        else -> "Extreme"
    }
}
