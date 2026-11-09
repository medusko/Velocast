package com.medusko.velocast.data

import com.medusko.velocast.model.CyclingMath
import com.medusko.velocast.model.CyclingPlace

data class SuburbCyclingHub(
    val suburbName: String,
    val stateCode: String,
    val centerLat: Double,
    val centerLon: Double,
    val spots: List<CyclingPlace>
)

object CyclingSuburbsDatabase {

    val fallbackDefaultPlaces: List<CyclingPlace> = listOf(
        CyclingPlace(
            id = "default_skyline_old_la_honda",
            name = "Skyline Blvd • Old La Honda",
            region = "Woodside, CA, USA",
            latitude = 37.3728,
            longitude = -122.2525,
            elevationFeet = 1790,
            tags = listOf("Redwood Climb", "Santa Cruz Mountains")
        ),
        CyclingPlace(
            id = "default_marin_hawk_hill",
            name = "Marin Headlands • Hawk Hill",
            region = "Sausalito, CA, USA",
            latitude = 37.8267,
            longitude = -122.4994,
            elevationFeet = 923,
            tags = listOf("Coastal Climb", "Golden Gate Overlook")
        )
    )

    private fun hub(
        idx: Int,
        suburb: String,
        state: String,
        lat: Double,
        lon: Double,
        s1: Triple<String, Pair<Double, Double>, Int>,
        s2: Triple<String, Pair<Double, Double>, Int>,
        s3: Triple<String, Pair<Double, Double>, Int>
    ): SuburbCyclingHub {
        val regionStr = "$suburb, $state, USA"
        return SuburbCyclingHub(
            suburbName = suburb,
            stateCode = state,
            centerLat = lat,
            centerLon = lon,
            spots = listOf(
                CyclingPlace("sub_${idx}_1", s1.first, regionStr, s1.second.first, s1.second.second, s1.third),
                CyclingPlace("sub_${idx}_2", s2.first, regionStr, s2.second.first, s2.second.second, s2.third),
                CyclingPlace("sub_${idx}_3", s3.first, regionStr, s3.second.first, s3.second.second, s3.third)
            )
        )
    }

    val top50Suburbs: List<SuburbCyclingHub> = listOf(
        hub(1, "Mesa", "AZ", 33.4152, -111.8315,
            Triple("Usery Mountain Pass Rd", Pair(33.4825, -111.6228), 2080),
            Triple("Bush Highway • Salt River Loop", Pair(33.5410, -111.5590), 1520),
            Triple("Las Sendas Mountain Climb", Pair(33.4790, -111.6780), 1850)
        ),
        hub(2, "Aurora", "CO", 39.7294, -104.8319,
            Triple("Cherry Creek State Park Loop", Pair(39.6380, -104.8320), 5610),
            Triple("Aurora Reservoir Trail", Pair(39.6075, -104.6710), 5980),
            Triple("High Line Canal Trailway", Pair(39.6890, -104.8120), 5480)
        ),
        hub(3, "Arlington", "TX", 32.7357, -97.1081,
            Triple("River Legacy Park Trail", Pair(32.7875, -97.1180), 515),
            Triple("Lake Arlington Loop", Pair(32.7080, -97.2040), 570),
            Triple("Joe Pool Lake Dam Road", Pair(32.6340, -97.0320), 590)
        ),
        hub(4, "Plano", "TX", 33.0198, -96.6989,
            Triple("Chisholm Trail Greenbelt", Pair(33.0460, -96.7160), 655),
            Triple("Oak Point Park & Nature Preserve", Pair(33.0595, -96.6690), 620),
            Triple("Bluebonnet Trail Corridor", Pair(33.0710, -96.7480), 680)
        ),
        hub(5, "Irvine", "CA", 33.6846, -117.8265,
            Triple("Santiago Canyon Road Loop", Pair(33.7480, -117.6890), 1420),
            Triple("Mountains to Sea Trail • Newport Back Bay", Pair(33.6510, -117.8780), 120),
            Triple("Shady Canyon & Turtle Rock Climb", Pair(33.6380, -117.8090), 690)
        ),
        hub(6, "Chula Vista", "CA", 32.6401, -117.0842,
            Triple("Otay Lakes Road & Olympic Training Loop", Pair(32.6290, -116.9380), 540),
            Triple("Bayshore Bikeway • Silver Strand", Pair(32.6350, -117.1180), 25),
            Triple("Honey Springs & Proctor Valley Climb", Pair(32.6680, -116.8650), 1650)
        ),
        hub(7, "Chandler", "AZ", 33.3062, -111.8413,
            Triple("Paseo Trail • Consolidated Canal", Pair(33.2920, -111.8050), 1240),
            Triple("South Mountain Dobbins Lookout Approach", Pair(33.3340, -112.0620), 2330),
            Triple("Veterans Oasis & Riggs Road Corridor", Pair(33.2180, -111.7710), 1290)
        ),
        hub(8, "Gilbert", "AZ", 33.3528, -111.7890,
            Triple("Western Powerline & Heritage Trail", Pair(33.3560, -111.7920), 1235),
            Triple("San Tan Mountain Regional Loop", Pair(33.1720, -111.6510), 1690),
            Triple("Riparian Preserve Canal Loop", Pair(33.3630, -111.7370), 1260)
        ),
        hub(9, "Henderson", "NV", 36.0395, -114.9817,
            Triple("River Mountains Loop Trail", Pair(36.0120, -114.8890), 2450),
            Triple("Lake Las Vegas & Lake Mead Pkwy Climb", Pair(36.0980, -114.9220), 1920),
            Triple("Anthem Hills & Sloan Canyon Road", Pair(35.9680, -115.0820), 2860)
        ),
        hub(10, "Scottsdale", "AZ", 33.4942, -111.9261,
            Triple("McDowell Mountain • Rio Verde Loop", Pair(33.6680, -111.8120), 2680),
            Triple("Indian Bend Wash Greenbelt", Pair(33.5120, -111.9150), 1290),
            Triple("Pinnacle Peak & Cave Creek Climb", Pair(33.7260, -111.8690), 2890)
        ),
        hub(11, "North Las Vegas", "NV", 36.1989, -115.1175,
            Triple("Upper Las Vegas Wash Trail", Pair(36.2650, -115.1580), 2190),
            Triple("Kyle Canyon & Mt. Charleston Approach", Pair(36.2890, -115.3820), 4850),
            Triple("Craig Ranch Regional Loop", Pair(36.2410, -115.1480), 2040)
        ),
        hub(12, "Fremont", "CA", 37.5485, -121.9886,
            Triple("Palomares Road Canyon Climb", Pair(37.6190, -121.9460), 1180),
            Triple("Alameda Creek Regional Trail", Pair(37.5680, -122.0490), 45),
            Triple("Calaveras Reservoir & Morrison Canyon", Pair(37.4890, -121.8420), 1320)
        ),
        hub(13, "Irving", "TX", 32.8140, -96.9489,
            Triple("Campion Trail • Trinity River Greenbelt", Pair(32.8780, -96.9320), 445),
            Triple("Las Colinas Lake Carolyn Loop", Pair(32.8690, -96.9410), 475),
            Triple("Northshore & Grapevine Lake Approach", Pair(32.9520, -97.0480), 560)
        ),
        hub(14, "Chesapeake", "VA", 36.7682, -76.2875,
            Triple("Dismal Swamp Canal Trail", Pair(36.6420, -76.3690), 22),
            Triple("Great Bridge Locks Waterway Loop", Pair(36.7180, -76.2390), 18),
            Triple("Back Bay & Pungo Rural Road Loop", Pair(36.6890, -76.0480), 15)
        ),
        hub(15, "Garland", "TX", 32.9126, -96.6389,
            Triple("Lake Ray Hubbard Shoreline Loop", Pair(32.8690, -96.5520), 485),
            Triple("Spring Creek Forest Preserve Trail", Pair(32.9610, -96.6620), 560),
            Triple("Duck Creek Greenbelt", Pair(32.8580, -96.6120), 510)
        ),
        hub(16, "Hialeah", "FL", 25.8576, -80.2781,
            Triple("Amelia Earhart Park Crit & Trail Loop", Pair(25.9010, -80.2860), 12),
            Triple("Shark Valley • Everglades Levee Ride", Pair(25.7580, -80.7650), 10),
            Triple("Rickenbacker Causeway & Key Biscayne", Pair(25.7360, -80.1720), 65)
        ),
        hub(17, "Frisco", "TX", 33.1507, -96.8236,
            Triple("Cottonwood Creek Greenbelt", Pair(33.1280, -96.8450), 660),
            Triple("Lake Lewisville Hidden Cove Loop", Pair(33.1540, -96.9210), 560),
            Triple("Celina & Prosper Country Road Loop", Pair(33.2680, -96.7840), 740)
        ),
        hub(18, "Tacoma", "WA", 47.2529, -122.4443,
            Triple("Point Defiance Five Mile Drive", Pair(47.3090, -122.5260), 315),
            Triple("Foothills National Recreation Trail", Pair(47.1520, -122.2140), 195),
            Triple("Ruston Way Waterfront & Brown's Point", Pair(47.2840, -122.4820), 85)
        ),
        hub(19, "Moreno Valley", "CA", 33.9425, -117.2297,
            Triple("Lake Perris State Recreation Loop", Pair(33.8590, -117.1750), 1610),
            Triple("Box Springs Mountain Road Climb", Pair(33.9890, -117.2920), 3020),
            Triple("San Timoteo Canyon Road", Pair(33.9810, -117.1140), 1950)
        ),
        hub(20, "Fontana", "CA", 34.0922, -117.4350,
            Triple("Pacific Electric Inland Empire Trail", Pair(34.1060, -117.4680), 1290),
            Triple("Lytle Creek Canyon Climb", Pair(34.2280, -117.4890), 3250),
            Triple("Glendora Mountain Road (GMR) Approach", Pair(34.1850, -117.7820), 3680)
        ),
        hub(21, "Modesto", "CA", 37.6391, -120.9969,
            Triple("Tuolumne River Regional Trail", Pair(37.6240, -120.9780), 92),
            Triple("Knights Ferry & Stanislaus Canyon Loop", Pair(37.8180, -120.6650), 540),
            Triple("La Grange & Turlock Lake Road", Pair(37.6480, -120.5420), 420)
        ),
        hub(22, "Glendale", "AZ", 33.5387, -112.1860,
            Triple("Thunderbird Conservation Park Climb", Pair(33.6810, -112.1850), 1680),
            Triple("New River Trailway", Pair(33.6120, -112.2380), 1190),
            Triple("White Tank Mountain Regional Loop", Pair(33.5680, -112.5120), 2150)
        ),
        hub(23, "Huntington Beach", "CA", 33.6595, -117.9988,
            Triple("Huntington Beach Ocean Strand Bike Path", Pair(33.6550, -118.0020), 18),
            Triple("Santa Ana River Trail (SART)", Pair(33.6940, -117.9320), 65),
            Triple("Bolsa Chica Coastal Wetlands Loop", Pair(33.7080, -118.0510), 24)
        ),
        hub(24, "Santa Clarita", "CA", 34.3917, -118.5426,
            Triple("Santa Clara River Trailway", Pair(34.4160, -118.5520), 1260),
            Triple("Sand Canyon & Little Tujunga Climb", Pair(34.3650, -118.4120), 3120),
            Triple("Castaic Lake Ridge Route", Pair(34.5280, -118.6090), 1980)
        ),
        hub(25, "Yonkers", "NY", 40.9312, -73.8987,
            Triple("South County Trailway", Pair(40.9580, -73.8690), 210),
            Triple("Old Croton Aqueduct & Hudson View", Pair(40.9720, -73.8840), 240),
            Triple("9W Palisades Interstate Cycling Corridor", Pair(40.9610, -73.9220), 410)
        ),
        hub(26, "Overland Park", "KS", 38.9822, -94.6708,
            Triple("Indian Creek Hike & Bike Trail", Pair(38.9320, -94.6680), 940),
            Triple("Tomahawk Creek Parkway Loop", Pair(38.9020, -94.6390), 980),
            Triple("Kill Creek & Mill Creek Streamway", Pair(38.9180, -94.8420), 890)
        ),
        hub(27, "Grand Prairie", "TX", 32.7459, -96.9978,
            Triple("Loyd Park & Joe Pool Lake Loop", Pair(32.6280, -97.0490), 560),
            Triple("Lone Star Levee Trail", Pair(32.7750, -96.9790), 460),
            Triple("Cedar Hill State Park Penn Farm Climb", Pair(32.6150, -96.9720), 780)
        ),
        hub(28, "McKinney", "TX", 33.1972, -96.6397,
            Triple("Erwin Park Cycling Loop", Pair(33.2420, -96.6520), 640),
            Triple("Wilson Creek Trailway", Pair(33.1780, -96.6580), 610),
            Triple("Weston & Melissa Rolling Country Loop", Pair(33.3290, -96.6490), 720)
        ),
        hub(29, "Tempe", "AZ", 33.4255, -111.9400,
            Triple("Tempe Town Lake & Rio Salado Path", Pair(33.4320, -111.9320), 1160),
            Triple("Papago Park Galvin Parkway Loop", Pair(33.4560, -111.9480), 1320),
            Triple("South Mountain Telegraph Pass Climb", Pair(33.3380, -112.0490), 2330)
        ),
        hub(30, "Ontario", "CA", 34.0633, -117.6509,
            Triple("Santa Ana River Trail • Prado Dam", Pair(33.9120, -117.6380), 560),
            Triple("Mt. Baldy Road & Glendora Ridge", Pair(34.2360, -117.6590), 4290),
            Triple("Euclid Avenue Historic Bridle Climb", Pair(34.1280, -117.6510), 1780)
        ),
        hub(31, "Vancouver", "WA", 45.6387, -122.6615,
            Triple("Columbia River Renaissance Trail", Pair(45.6190, -122.6520), 45),
            Triple("Burnt Bridge Creek Trail", Pair(45.6540, -122.6320), 210),
            Triple("Larch Mountain & Washougal River Climb", Pair(45.6320, -122.3120), 1480)
        ),
        hub(32, "Surprise", "AZ", 33.6292, -112.3679,
            Triple("Lake Pleasant Parkway Loop", Pair(33.8290, -112.2780), 1740),
            Triple("Sun Valley Parkway Desert Ride", Pair(33.6490, -112.5680), 1590),
            Triple("White Tank Waterfall Road Approach", Pair(33.5810, -112.5020), 1880)
        ),
        hub(33, "Cary", "NC", 35.7915, -78.7811,
            Triple("American Tobacco Trail (ATT)", Pair(35.7680, -78.8920), 340),
            Triple("Black Creek & Umstead Park Loop", Pair(35.8390, -78.7650), 420),
            Triple("Jordan Lake & Farrington Road Loop", Pair(35.7380, -79.0120), 310)
        ),
        hub(34, "Elk Grove", "CA", 38.4088, -121.3716,
            Triple("Cosumnes River Preserve Causeway", Pair(38.2680, -121.4380), 28),
            Triple("Sacramento River Levee & Clarksburg Loop", Pair(38.4190, -121.5280), 35),
            Triple("American River Jedediah Smith Trail", Pair(38.5890, -121.3850), 115)
        ),
        hub(35, "Oceanside", "CA", 33.1959, -117.3795,
            Triple("San Luis Rey River Bike Trail", Pair(33.2250, -117.3420), 65),
            Triple("Coast Highway 101 • Carlsbad to Torrey Pines", Pair(33.1180, -117.3260), 95),
            Triple("De Luz Canyon & Palomar Mountain Approach", Pair(33.3680, -117.2890), 2150)
        ),
        hub(36, "Pembroke Pines", "FL", 26.0078, -80.2963,
            Triple("Markham Park Levee & Crit Course", Pair(26.1290, -80.3560), 14),
            Triple("Everglades Conservation Levee L-35", Pair(26.0820, -80.4420), 12),
            Triple("Brian Piccolo Velodrome & Park Loop", Pair(26.0310, -80.2680), 11)
        ),
        hub(37, "Rancho Cucamonga", "CA", 34.1064, -117.5931,
            Triple("Pacific Electric Trail • Etiwanda Depot", Pair(34.1080, -117.5420), 1320),
            Triple("sapphire Street & Baldy Rd Foothill Climb", Pair(34.1620, -117.5980), 2340),
            Triple("Bonelli Park & Puddingstone Reservoir", Pair(34.0840, -117.8020), 1080)
        ),
        hub(38, "Garden Grove", "CA", 33.7743, -117.9380,
            Triple("Santa Ana River Trail • Anaheim to Coast", Pair(33.7820, -117.8820), 110),
            Triple("Coyote Creek Bikeway", Pair(33.8120, -118.0650), 85),
            Triple(" Villa Park Dam & Santiago Oaks Climb", Pair(33.8140, -117.7680), 740)
        ),
        hub(39, "Santa Rosa", "CA", 38.4404, -122.7141,
            Triple("Coleman Valley Road Coastal Climb", Pair(38.3720, -123.0120), 1180),
            Triple("Annadel & Bennett Valley Loop", Pair(38.4190, -122.6320), 680),
            Triple("King Ridge Road & Westside Wine Loop", Pair(38.5420, -123.0890), 1650)
        ),
        hub(40, "Peoria", "AZ", 33.5806, -112.2374,
            Triple("New River & Skunk Creek Trail", Pair(33.6380, -112.2260), 1240),
            Triple("Lake Pleasant Scorpion Bay Climb", Pair(33.8680, -112.2890), 1780),
            Triple("Vistancia & Carefree Highway Loop", Pair(33.7820, -112.3120), 1920)
        ),
        hub(41, "Corona", "CA", 33.8753, -117.5664,
            Triple("Skyline Drive Foothill Climb", Pair(33.8320, -117.6120), 1980),
            Triple("Santa Ana River Canyon • Green River", Pair(33.8820, -117.6680), 490),
            Triple("Temescal Canyon & Lake Mathews Loop", Pair(33.8120, -117.4680), 1420)
        ),
        hub(42, "Bellevue", "WA", 47.6101, -122.2015,
            Triple("Lake Washington Loop • Mercer Island", Pair(47.5680, -122.2220), 180),
            Triple("Zoo Hill • Cougar Mountain Climb", Pair(47.5390, -122.0980), 1420),
            Triple("Lake Sammamish River Trail", Pair(47.6180, -122.0890), 95)
        ),
        hub(43, "Naperville", "IL", 41.7508, -88.1535,
            Triple("DuPage River Trailway", Pair(41.7320, -88.1480), 690),
            Triple("Illinois Prairie Path • Aurora Branch", Pair(41.8020, -88.1890), 730),
            Triple("Greene Valley & Herrick Lake Loop", Pair(41.8240, -88.1320), 760)
        ),
        hub(44, "Pasadena", "TX", 29.6911, -95.2091,
            Triple("Armand Bayou Nature Trail & NASA Loop", Pair(29.5890, -95.0820), 24),
            Triple("San Jacinto Battleground Monument Loop", Pair(29.7490, -95.0810), 28),
            Triple("Seabrook & Kemah Bayfront Ride", Pair(29.5620, -95.0220), 18)
        ),
        hub(45, "Joliet", "IL", 41.5250, -88.0817,
            Triple("I&M Canal State Trail • Channahon", Pair(41.4320, -88.2180), 540),
            Triple("Wauponsee Glacial Trail", Pair(41.4480, -88.0420), 620),
            Triple("Old Plank Road Trail", Pair(41.5180, -87.9620), 680)
        ),
        hub(46, "Paterson", "NJ", 40.9168, -74.1718,
            Triple("Garret Mountain Reservation Climb", Pair(40.8960, -74.1780), 510),
            Triple("Saddle River County Bike Path", Pair(40.9320, -74.0920), 85),
            Triple("High Mountain & Skyline Drive Ramapo", Pair(41.0380, -74.2480), 1040)
        ),
        hub(47, "Torrance", "CA", 33.8358, -118.3406,
            Triple("Palos Verdes Donut Loop • Hawthorne & PV Dr", Pair(33.7580, -118.3890), 1180),
            Triple("Marvin Braude Strand Beach Bike Path", Pair(33.8290, -118.3920), 22),
            Triple(" Portugese Bend & Switchbacks Climb", Pair(33.7390, -118.3520), 940)
        ),
        hub(48, "Pasadena", "CA", 34.1478, -118.1445,
            Triple("Rose Bowl Aquatic & Loop Circuit", Pair(34.1610, -118.1670), 860),
            Triple("Angeles Crest Highway • Clear Creek Climb", Pair(34.2680, -118.1520), 4650),
            Triple("Mt. Wilson Toll Road & Red Box Summit", Pair(34.2280, -118.0620), 5710)
        ),
        hub(49, "Lakewood", "CO", 39.7047, -105.0814,
            Triple("Lookout Mountain Road • Golden Climb", Pair(39.7360, -105.2390), 7375),
            Triple("Bear Creek Greenbelt & Red Rocks Loop", Pair(39.6650, -105.2050), 6450),
            Triple("Green Mountain Hayden Park Loop", Pair(39.6980, -105.1780), 6800)
        ),
        hub(50, "Sunnyvale", "CA", 37.3688, -122.0363,
            Triple("Stevens Canyon & Montebello Road Climb", Pair(37.3020, -122.0890), 2620),
            Triple("Page Mill Road to Skyline Summit", Pair(37.3260, -122.1780), 2310),
            Triple("San Francisco Bay Trail • Moffett Channel", Pair(37.4190, -122.0250), 15)
        )
    )

    fun resolvePlacesForLocation(lat: Double?, lon: Double?): List<CyclingPlace> {
        if (lat == null || lon == null) {
            return fallbackDefaultPlaces
        }
        var closestHub: SuburbCyclingHub? = null
        var minDistance = Double.MAX_VALUE
        for (hub in top50Suburbs) {
            val d = CyclingMath.haversineMiles(lat, lon, hub.centerLat, hub.centerLon)
            if (d < minDistance) {
                minDistance = d
                closestHub = hub
            }
        }
        return if (closestHub != null && minDistance <= 42.0) {
            closestHub.spots
        } else {
            fallbackDefaultPlaces
        }
    }

    fun findClosestCyclingSpotTo(lat: Double, lon: Double): CyclingPlace {
        val allSpots = fallbackDefaultPlaces + top50Suburbs.flatMap { it.spots }
        return allSpots.minByOrNull { spot ->
            CyclingMath.haversineMiles(lat, lon, spot.latitude, spot.longitude)
        } ?: fallbackDefaultPlaces.first()
    }

    fun findClosestPlaceName(lat: Double, lon: Double, userSavedPlaces: List<CyclingPlace>): String {
        val pool = (userSavedPlaces + fallbackDefaultPlaces + top50Suburbs.flatMap { it.spots })
            .distinctBy { "${it.name}_${it.region}" }
        val closest = pool.minByOrNull { spot ->
            CyclingMath.haversineMiles(lat, lon, spot.latitude, spot.longitude)
        } ?: fallbackDefaultPlaces.first()
        return "${closest.name} (${closest.region})"
    }
}
