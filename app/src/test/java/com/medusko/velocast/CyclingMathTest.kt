package com.medusko.velocast

import com.medusko.velocast.data.CyclingSuburbsDatabase
import com.medusko.velocast.model.*
import org.junit.Assert.*
import org.junit.Test

class CyclingMathTest {

    @Test
    fun defaultOptionsMatchUserRequirements() {
        assertEquals(MappingOption.OPEN_FREE_MAP, MappingOption.fromId(null))
        assertEquals(MapStyleOption.REGULAR, MapStyleOption.fromId(null))
        assertEquals(WeatherApiOption.OPEN_METEO, WeatherApiOption.fromId(null))
        assertEquals(CameraFeedOption.WINDY_WEBCAMS, CameraFeedOption.fromId(null))
        assertEquals(2, MappingOption.entries.size)
        assertEquals(3, MapStyleOption.entries.size)
        assertEquals(4, WeatherApiOption.entries.size)
        assertEquals(2, CameraFeedOption.entries.size)
    }

    @Test
    fun top50SuburbsDatabaseAndFallbackWorkCorrectly() {
        assertEquals(50, CyclingSuburbsDatabase.top50Suburbs.size)
        val totalPlaces = CyclingSuburbsDatabase.top50Suburbs.sumOf { it.spots.size }
        assertEquals(150, totalPlaces)

        // All places in database include country ", USA"
        assertTrue(CyclingSuburbsDatabase.top50Suburbs.all { cluster ->
            cluster.spots.all { spot -> spot.region.endsWith("USA") }
        })

        // Fallback when outside all 50 suburbs (e.g., middle of the Atlantic) returns Skyline Blvd & Hawk Hill
        val fallback = CyclingSuburbsDatabase.resolvePlacesForLocation(0.0, 0.0)
        assertEquals(2, fallback.size)
        assertTrue(fallback.any { it.name.contains("Old La Honda") })
        assertTrue(fallback.any { it.name.contains("Hawk Hill") })

        // Near Irvine, CA returns Irvine's 3 cycling places
        val irvinePlaces = CyclingSuburbsDatabase.resolvePlacesForLocation(33.6846, -117.8265)
        assertEquals(3, irvinePlaces.size)
    }

    @Test
    fun granularUnitPreferencesFormatCorrectly() {
        val mixedUnits = UnitPreferences(
            tempUnit = TempUnit.FAHRENHEIT,
            speedUnit = SpeedUnit.KMH,
            distanceUnit = DistanceUnit.KILOMETERS,
            elevationUnit = ElevationUnit.METERS
        )
        assertEquals("72°F", CyclingMath.formatTemp(72.0, mixedUnits))
        assertEquals("24 km/h", CyclingMath.formatSpeed(15.0, mixedUnits))
        assertEquals("16.1 km", CyclingMath.formatDistance(10.0, mixedUnits))
        assertEquals("305 m", CyclingMath.formatElevation(1000, mixedUnits))

        // Dynamic resolution formatting in Info/Options
        assertEquals("0.6–1.9 mi", CyclingMath.formatResolutionSpan(DistanceUnit.MILES))
        assertEquals("1–3 km", CyclingMath.formatResolutionSpan(DistanceUnit.KILOMETERS))
    }

    @Test
    fun analyzeWindDetectsHeadwindAndTailwind() {
        // Rider going North (0 deg), Wind coming FROM North (0 deg) at 15 mph -> pure headwind
        val (headImpact, headComp, _) = CyclingMath.analyzeWindOnSegment(
            riderBearingDeg = 0,
            windFromDeg = 0,
            windSpeedMph = 15.0
        )
        assertEquals(WindImpact.HEADWIND, headImpact)
        assertTrue(headComp > 14.0)

        // Rider going North (0 deg), Wind coming FROM South (180 deg) at 15 mph -> pure tailwind
        val (tailImpact, tailComp, _) = CyclingMath.analyzeWindOnSegment(
            riderBearingDeg = 0,
            windFromDeg = 180,
            windSpeedMph = 15.0
        )
        assertEquals(WindImpact.TAILWIND, tailImpact)
        assertTrue(tailComp < -14.0)

        // Rider going North (0 deg), Wind coming FROM West (270 deg) at 15 mph -> pure crosswind
        val (crossImpact, _, crossComp) = CyclingMath.analyzeWindOnSegment(
            riderBearingDeg = 0,
            windFromDeg = 270,
            windSpeedMph = 15.0
        )
        assertEquals(WindImpact.CROSSWIND, crossImpact)
        assertTrue(crossComp > 14.0)
    }

    @Test
    fun cardinalDirectionsAndDistanceAreAccurate() {
        assertEquals("N", CyclingMath.cardinalFromDegrees(0))
        assertEquals("E", CyclingMath.cardinalFromDegrees(90))
        assertEquals("S", CyclingMath.cardinalFromDegrees(180))
        assertEquals("W", CyclingMath.cardinalFromDegrees(270))

        val dist = CyclingMath.haversineMiles(37.8039, -122.4648, 37.8267, -122.4994)
        assertTrue(dist in 2.0..3.5)
    }
}
