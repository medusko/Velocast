package com.medusko.velocast

import com.medusko.velocast.model.*
import org.junit.Assert.*
import org.junit.Test

class CyclingMathTest {

    @Test
    fun defaultOptionsMatchUserRequirements() {
        assertEquals(MappingOption.OPEN_FREE_MAP, MappingOption.fromId(null))
        assertEquals(WeatherApiOption.OPEN_METEO, WeatherApiOption.fromId(null))
        assertEquals(CameraFeedOption.WINDY_WEBCAMS, CameraFeedOption.fromId(null))
        assertEquals(2, MappingOption.entries.size)
        assertEquals(4, WeatherApiOption.entries.size)
        assertEquals(2, CameraFeedOption.entries.size)
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
