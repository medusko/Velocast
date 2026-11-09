package com.medusko.velocast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medusko.velocast.R
import com.medusko.velocast.data.GeocodedLocation
import com.medusko.velocast.model.*
import com.medusko.velocast.ui.components.AboutVelocastDialog
import com.medusko.velocast.ui.components.CameraFeedDialog
import com.medusko.velocast.ui.components.VelocastMapView
import com.medusko.velocast.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MainScreen(viewModel: VelocastViewModel) {
    val mappingOption by viewModel.mappingOption.collectAsState()
    val weatherApiOption by viewModel.weatherApiOption.collectAsState()
    val cameraFeedOption by viewModel.cameraFeedOption.collectAsState()
    val units by viewModel.unitSystem.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val appIconStyle by viewModel.appIconStyle.collectAsState()

    val savedPlaces by viewModel.savedPlaces.collectAsState()
    val isRefreshingPlaces by viewModel.isRefreshingPlaces.collectAsState()
    val placeSearchResults by viewModel.placeSearchResults.collectAsState()

    val plannedWaypoints by viewModel.plannedWaypoints.collectAsState()
    val routeGeometry by viewModel.routeGeometry.collectAsState()
    val plannedDistanceMiles by viewModel.plannedDistanceMiles.collectAsState()
    val plannedDurationMinutes by viewModel.plannedDurationMinutes.collectAsState()
    val riderSpeedMph by viewModel.riderSpeedMph.collectAsState()
    val startHourOffset by viewModel.startHourOffset.collectAsState()
    val showCamerasOnMap by viewModel.showCamerasOnMap.collectAsState()
    val isCalculatingRoute by viewModel.isCalculatingRoute.collectAsState()
    val waypointSearchResults by viewModel.waypointSearchResults.collectAsState()

    val savedRides by viewModel.savedRides.collectAsState()
    val isRefreshingRides by viewModel.isRefreshingRides.collectAsState()

    val weatherApiKey by viewModel.weatherApiKey.collectAsState()
    val openWeatherMapKey by viewModel.openWeatherMapKey.collectAsState()
    val windyApiKey by viewModel.windyApiKey.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Camera dialog state
    var activeCameraDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeCameraDialogCoords by remember { mutableStateOf(Pair(37.8267, -122.4994)) }
    var activeCameraDialogWeather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var activeCameraInitialSelection by remember { mutableStateOf<PublicCameraFeed?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (appIconStyle == AppIconStyle.GOLD) R.drawable.ic_app_gold else R.drawable.ic_app_classic
                                ),
                                contentDescription = "Velocast Icon",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Velocast",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = weatherApiOption.title.substringBefore(" ("),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${mappingOption.title.substringBefore(" (")} • ${cameraFeedOption.title.substringBefore(" API")}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    viewModel.refreshAllPlacesWeather()
                                    viewModel.recalculateActiveRoute()
                                    viewModel.refreshSavedRidesWeather()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Weather",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { showAboutDialog = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About Velocast",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Place, contentDescription = "Places") },
                    label = { Text("Places", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.DirectionsBike, contentDescription = "Plan a Ride") },
                    label = { Text("Plan a Ride", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Route, contentDescription = "Saved Rides") },
                    label = { Text("Saved Rides", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Options") },
                    label = { Text("Options", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> SavedPlacesTab(
                    places = savedPlaces,
                    isRefreshing = isRefreshingPlaces,
                    searchResults = placeSearchResults,
                    units = units,
                    onSearchQueryChanged = viewModel::searchPlacesToAdd,
                    onAddPlace = viewModel::addCyclingPlace,
                    onRemovePlace = viewModel::removeCyclingPlace,
                    onOpenCamerasForPlace = { place ->
                        activeCameraDialogTitle = place.name
                        activeCameraDialogCoords = Pair(place.latitude, place.longitude)
                        activeCameraDialogWeather = place.weather
                        activeCameraInitialSelection = null
                    },
                    onPlanRideFromPlace = { place ->
                        viewModel.startRideFromPlace(place)
                        selectedTab = 1
                    }
                )

                1 -> PlanARideTab(
                    mappingOption = mappingOption,
                    waypoints = plannedWaypoints,
                    routeGeometry = routeGeometry,
                    totalDistanceMiles = plannedDistanceMiles,
                    estimatedMinutes = plannedDurationMinutes,
                    riderSpeedMph = riderSpeedMph,
                    startHourOffset = startHourOffset,
                    showCamerasOnMap = showCamerasOnMap,
                    isCalculating = isCalculatingRoute,
                    waypointSearchResults = waypointSearchResults,
                    units = units,
                    camerasNearRoute = remember(plannedWaypoints, cameraFeedOption) {
                        val ref = plannedWaypoints.firstOrNull()
                        viewModel.getCamerasNear(ref?.latitude ?: 37.8267, ref?.longitude ?: -122.4994)
                    },
                    onMapTapAddWaypoint = viewModel::addWaypointFromMapTap,
                    onSearchWaypoint = viewModel::searchWaypointAddress,
                    onSelectGeocodedWaypoint = viewModel::addWaypointFromGeocoded,
                    onRemoveWaypointAt = viewModel::removeWaypointAt,
                    onClearWaypoints = viewModel::clearAllWaypoints,
                    onReverseWaypoints = viewModel::reverseWaypoints,
                    onToggleMapCameras = viewModel::toggleShowCamerasOnMap,
                    onSpeedSelected = viewModel::setRiderSpeed,
                    onHourOffsetSelected = viewModel::setStartHourOffset,
                    onSaveRide = { name ->
                        viewModel.saveCurrentPlannedRide(name)
                        selectedTab = 2
                    },
                    onOpenCameraModal = { title, lat, lon, weather, initialCam ->
                        activeCameraDialogTitle = title
                        activeCameraDialogCoords = Pair(lat, lon)
                        activeCameraDialogWeather = weather
                        activeCameraInitialSelection = initialCam
                    }
                )

                2 -> SavedRidesTab(
                    savedRides = savedRides,
                    isRefreshing = isRefreshingRides,
                    units = units,
                    onLoadRideIntoPlanner = { ride ->
                        viewModel.loadSavedRideIntoPlanner(ride)
                        selectedTab = 1
                    },
                    onOpenCamerasForRide = { ride ->
                        val mid = ride.waypoints.getOrNull(ride.waypoints.size / 2) ?: ride.waypoints.firstOrNull()
                        if (mid != null) {
                            activeCameraDialogTitle = ride.name
                            activeCameraDialogCoords = Pair(mid.latitude, mid.longitude)
                            activeCameraDialogWeather = mid.weather
                            activeCameraInitialSelection = null
                        }
                    },
                    onDeleteRide = viewModel::deleteSavedRide,
                    onRefreshRides = viewModel::refreshSavedRidesWeather
                )

                3 -> OptionsTab(
                    mappingOption = mappingOption,
                    weatherApiOption = weatherApiOption,
                    cameraFeedOption = cameraFeedOption,
                    units = units,
                    appIconStyle = appIconStyle,
                    weatherApiKey = weatherApiKey,
                    openWeatherMapKey = openWeatherMapKey,
                    windyApiKey = windyApiKey,
                    onMappingOptionSelected = viewModel::updateMappingOption,
                    onWeatherApiSelected = viewModel::updateWeatherApiOption,
                    onCameraFeedSelected = viewModel::updateCameraFeedOption,
                    onUnitSystemSelected = viewModel::updateUnitSystem,
                    onAppIconStyleSelected = viewModel::updateAppIconStyle,
                    onSaveApiKeys = viewModel::updateApiKeys
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutVelocastDialog(
            mappingOption = mappingOption,
            weatherApiOption = weatherApiOption,
            cameraFeedOption = cameraFeedOption,
            onDismiss = { showAboutDialog = false }
        )
    }

    activeCameraDialogTitle?.let { title ->
        val cams = remember(activeCameraDialogCoords, cameraFeedOption) {
            viewModel.getCamerasNear(activeCameraDialogCoords.first, activeCameraDialogCoords.second)
        }
        CameraFeedDialog(
            placeTitle = title,
            cameras = cams,
            activeSourceOption = cameraFeedOption,
            weather = activeCameraDialogWeather,
            units = units,
            initialSelectedCamera = activeCameraInitialSelection,
            onDismiss = { activeCameraDialogTitle = null }
        )
    }
}

// ============================================================================
// TAB 1: SAVED PLACES (CYCLING SPOTS)
// ============================================================================
@Composable
private fun SavedPlacesTab(
    places: List<CyclingPlace>,
    isRefreshing: Boolean,
    searchResults: List<GeocodedLocation>,
    units: UnitSystem,
    onSearchQueryChanged: (String) -> Unit,
    onAddPlace: (GeocodedLocation) -> Unit,
    onRemovePlace: (String) -> Unit,
    onOpenCamerasForPlace: (CyclingPlace) -> Unit,
    onPlanRideFromPlace: (CyclingPlace) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedPlaceId by remember { mutableStateOf<String?>(places.firstOrNull()?.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Search & Add Common Cycling Place Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SAVED CYCLING PLACES & MICROCLIMATES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onSearchQueryChanged(it)
                        },
                        placeholder = { Text("Add cycling place (e.g. Mt Diablo, Palomar, Girona)...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    onSearchQueryChanged("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            searchResults.forEach { loc ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddPlace(loc)
                                            searchQuery = ""
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = loc.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${loc.region} • Elev ${CyclingMath.formatElevation(loc.elevationFeet, units)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "+ Save Spot",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Places Cards
        items(places, key = { it.id }) { place ->
            val w = place.weather
            val isExpanded = expandedPlaceId == place.id

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedPlaceId = if (isExpanded) null else place.id
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top Row: Name + Region + Temperature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${place.region} • Elev ${CyclingMath.formatElevation(place.elevationFeet, units)}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (w != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CyclingMath.formatTemp(w.temperatureF, units),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "H:${CyclingMath.formatTemp(w.tempHighF, units)} L:${CyclingMath.formatTemp(w.tempLowF, units)}",
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (w != null) {
                        // 4 Primary Telemetry Badges: Wind, Rain %, UV Index, Sunrise/Sunset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Wind Vector Pill
                            TelemetryMetricTile(
                                label = "WIND & GUSTS",
                                primaryValue = "${CyclingMath.formatSpeed(w.windSpeedMph, units)} ${w.windCardinal}",
                                subValue = "Gusts ${CyclingMath.formatSpeed(w.windGustMph, units)}",
                                accentColor = if (w.windSpeedMph > 16) CrosswindAmber else AccentBlue,
                                windArrowDeg = w.windDirectionDeg,
                                modifier = Modifier.weight(1.15f)
                            )

                            // Chance of Rain Pill
                            TelemetryMetricTile(
                                label = "RAIN CHANCE",
                                primaryValue = "${w.rainChancePercent}%",
                                subValue = w.conditionLabel,
                                accentColor = if (w.rainChancePercent >= 40) HeadwindRed else TailwindGreen,
                                modifier = Modifier.weight(0.95f)
                            )

                            // UV Index Pill
                            TelemetryMetricTile(
                                label = "UV INDEX",
                                primaryValue = String.format("%.1f", w.uvIndex),
                                subValue = CyclingMath.uvSeverityLabel(w.uvIndex),
                                accentColor = UvPurple,
                                modifier = Modifier.weight(0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Astronomy (Sunrise / Sunset) & Closest Weather Station Bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🌅 Sunrise: ${w.sunriseTime}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MeduskoGold
                                    )
                                    Text(
                                        text = "🌇 Sunset: ${w.sunsetTime}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrosswindAmber
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📡 Station: ${w.closestStationId} (${w.closestStationName})",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${CyclingMath.formatDistance(w.stationDistanceMiles, units)} away",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }

                        // Expandable 12-Hour Cycling Timeline
                        AnimatedVisibility(visible = isExpanded && w.hourly.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "12-HOUR CYCLING WIND & WEATHER TIMELINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.7.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    w.hourly.forEach { hr ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = hr.timeLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = CyclingMath.formatTemp(hr.tempF, units),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Navigation,
                                                        contentDescription = null,
                                                        tint = AccentBlue,
                                                        modifier = Modifier
                                                            .size(11.dp)
                                                            .rotate(((hr.windDirectionDeg + 180) % 360).toFloat())
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = CyclingMath.formatSpeed(hr.windSpeedMph, units),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AccentBlue
                                                    )
                                                }
                                                Text(
                                                    text = "💧${hr.precipProbPercent}% • UV ${hr.uvIndex.roundToInt()}",
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Action Buttons: Live Cams, Plan Ride From Here, Remove
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onOpenCamerasForPlace(place) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Live Cams", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                            }

                            Button(
                                onClick = { onPlanRideFromPlace(place) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBike,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Plan Ride Here", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                            }

                            if (places.size > 1) {
                                IconButton(
                                    onClick = { onRemovePlace(place.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Place",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricTile(
    label: String,
    primaryValue: String,
    subValue: String,
    accentColor: Color,
    windArrowDeg: Int? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (windArrowDeg != null) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Wind Direction",
                        tint = accentColor,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(((windArrowDeg + 180) % 360).toFloat())
                    )
                }
                Text(
                    text = primaryValue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subValue,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// TAB 2: PLAN A RIDE (5 WAYPOINTS + MAP + ROUTE WEATHER + CAMERAS)
// ============================================================================
@Composable
private fun PlanARideTab(
    mappingOption: MappingOption,
    waypoints: List<RouteWaypoint>,
    routeGeometry: List<Pair<Double, Double>>,
    totalDistanceMiles: Double,
    estimatedMinutes: Int,
    riderSpeedMph: Double,
    startHourOffset: Int,
    showCamerasOnMap: Boolean,
    isCalculating: Boolean,
    waypointSearchResults: List<GeocodedLocation>,
    units: UnitSystem,
    camerasNearRoute: List<PublicCameraFeed>,
    onMapTapAddWaypoint: (Double, Double) -> Unit,
    onSearchWaypoint: (String) -> Unit,
    onSelectGeocodedWaypoint: (GeocodedLocation) -> Unit,
    onRemoveWaypointAt: (Int) -> Unit,
    onClearWaypoints: () -> Unit,
    onReverseWaypoints: () -> Unit,
    onToggleMapCameras: () -> Unit,
    onSpeedSelected: (Double) -> Unit,
    onHourOffsetSelected: (Int) -> Unit,
    onSaveRide: (String) -> Unit,
    onOpenCameraModal: (String, Double, Double, WeatherSnapshot?, PublicCameraFeed?) -> Unit
) {
    var addressQuery by remember { mutableStateOf("") }
    var customRideName by remember { mutableStateOf("") }
    var showSaveInput by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // 1. Interactive Map
        item {
            VelocastMapView(
                mappingOption = mappingOption,
                waypoints = waypoints,
                routeGeometry = routeGeometry,
                cameras = camerasNearRoute,
                showCamerasOnMap = showCamerasOnMap,
                units = units,
                onMapTapAddWaypoint = onMapTapAddWaypoint,
                onCameraMarkerSelected = { cam ->
                    onOpenCameraModal(
                        cam.title,
                        cam.latitude,
                        cam.longitude,
                        waypoints.firstOrNull()?.weather,
                        cam
                    )
                }
            )
        }

        // 2. Map Toolbar & Address Input (up to 5 points)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROUTE WAYPOINTS (${waypoints.size}/5 POINTS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.7.sp
                        )
                        if (isCalculating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Address Search Input
                    OutlinedTextField(
                        value = addressQuery,
                        onValueChange = {
                            addressQuery = it
                            onSearchWaypoint(it)
                        },
                        enabled = waypoints.size < 5,
                        placeholder = {
                            Text(
                                if (waypoints.size < 5) "Type address/climb or tap map to add point ${waypoints.size + 1}..."
                                else "Maximum 5 route waypoints reached",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (waypointSearchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            waypointSearchResults.forEach { loc ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectGeocodedWaypoint(loc)
                                            addressQuery = ""
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${loc.name} (${loc.region})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "+ Pt ${waypoints.size + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Departure Time & Cycling Speed Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val offsets = listOf(0 to "Now", 1 to "+1h", 2 to "+2h", 4 to "+4h", 16 to "Tomorrow AM")
                        offsets.forEach { (hr, label) ->
                            FilterChip(
                                selected = startHourOffset == hr,
                                onClick = { onHourOffsetSelected(hr) },
                                label = { Text("🕒 $label", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        val speeds = listOf(12.0, 15.0, 18.0, 22.0)
                        speeds.forEach { spd ->
                            FilterChip(
                                selected = abs(riderSpeedMph - spd) < 0.5,
                                onClick = { onSpeedSelected(spd) },
                                label = {
                                    Text(
                                        "🚴 ${CyclingMath.formatSpeed(spd, units)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReverseWaypoints,
                            enabled = waypoints.size >= 2,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                        ) {
                            Text("⇄ Reverse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onToggleMapCameras,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                if (showCamerasOnMap) "📷 Cams: ON" else "📷 Cams: OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showCamerasOnMap) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onClearWaypoints,
                            enabled = waypoints.isNotEmpty(),
                            modifier = Modifier.weight(0.85f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showSaveInput = !showSaveInput },
                            enabled = waypoints.size >= 2,
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("💾 Save Ride", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    AnimatedVisibility(visible = showSaveInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customRideName,
                                onValueChange = { customRideName = it },
                                placeholder = { Text("Enter ride name (e.g. Sunday Coastal Loop)", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    onSaveRide(customRideName)
                                    customRideName = ""
                                    showSaveInput = false
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Confirm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Route Summary Telemetry Card
        if (waypoints.isNotEmpty()) {
            item {
                val temps = waypoints.mapNotNull { it.weather?.temperatureF }
                val minTemp = temps.minOrNull() ?: 60.0
                val maxTemp = temps.maxOrNull() ?: 68.0
                val headwindCount = waypoints.count { it.windImpact == WindImpact.HEADWIND }
                val tailwindCount = waypoints.count { it.windImpact == WindImpact.TAILWIND }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ROUTE FORECAST SUMMARY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${CyclingMath.formatDistance(totalDistanceMiles, units)} • ~${estimatedMinutes} min ride",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Temp: ${CyclingMath.formatTemp(minTemp, units)} – ${CyclingMath.formatTemp(maxTemp, units)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = MeduskoGold
                            )
                            Text(
                                text = "🟢 $tailwindCount Tailwind  •  🔴 $headwindCount Headwind",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Waypoint-by-Waypoint Weather Cards
        itemsIndexed(waypoints, key = { idx, wp -> "${idx}_${wp.latitude}_${wp.longitude}" }) { idx, wp ->
            val w = wp.weather
            val impactColor = when (wp.windImpact) {
                WindImpact.TAILWIND -> TailwindGreen
                WindImpact.HEADWIND -> HeadwindRed
                WindImpact.CROSSWIND -> CrosswindAmber
                WindImpact.CALM -> AccentBlue
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, impactColor.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = impactColor
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0E131D)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = wp.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${CyclingMath.formatDistance(wp.distanceFromStartMiles, units)} • ETA ${wp.etaLabel} • Bearing ${wp.bearingDeg}°",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = impactColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, impactColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = wp.windImpact.label,
                                    color = impactColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { onRemoveWaypointAt(idx) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Waypoint",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (w != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌡️ ${CyclingMath.formatTemp(w.temperatureF, units)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "💨 ${CyclingMath.formatSpeed(w.windSpeedMph, units)} ${w.windCardinal} (G:${CyclingMath.formatSpeed(w.windGustMph, units)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = impactColor
                            )
                            Text(
                                text = "💧 ${w.rainChancePercent}%  •  ☀️ UV ${w.uvIndex.roundToInt()}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📡 ${w.closestStationId} (${CyclingMath.formatDistance(w.stationDistanceMiles, units)}) • 🌇 ${w.sunsetTime}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "📷 Nearby Cam ↗",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                modifier = Modifier.clickable {
                                    onOpenCameraModal(wp.label, wp.latitude, wp.longitude, w, null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: SAVED RIDES (SHOWING TEMPERATURE & WIND PER RIDE)
// ============================================================================
@Composable
private fun SavedRidesTab(
    savedRides: List<SavedRide>,
    isRefreshing: Boolean,
    units: UnitSystem,
    onLoadRideIntoPlanner: (SavedRide) -> Unit,
    onOpenCamerasForRide: (SavedRide) -> Unit,
    onDeleteRide: (String) -> Unit,
    onRefreshRides: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAVED CYCLING RIDES & TEMPERATURE FORECASTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Live temperature, wind vectors, UV & rain across every saved route",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TextButton(onClick = onRefreshRides) {
                        Text("Refresh All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        items(savedRides, key = { it.id }) { ride ->
            val temps = ride.waypoints.mapNotNull { it.weather?.temperatureF }
            val startTemp = ride.waypoints.firstOrNull()?.weather?.temperatureF ?: 64.0
            val finishTemp = ride.waypoints.lastOrNull()?.weather?.temperatureF ?: 66.0
            val minTemp = temps.minOrNull() ?: startTemp
            val maxTemp = temps.maxOrNull() ?: finishTemp
            val maxRain = ride.waypoints.mapNotNull { it.weather?.rainChancePercent }.maxOrNull() ?: 10
            val maxUv = ride.waypoints.mapNotNull { it.weather?.uvIndex }.maxOrNull() ?: 5.0
            val avgWind = ride.waypoints.mapNotNull { it.weather?.windSpeedMph }.average().takeIf { !it.isNaN() } ?: 11.0
            val sunset = ride.waypoints.firstOrNull()?.weather?.sunsetTime ?: "5:08 PM"

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ride.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${ride.region} • ${CyclingMath.formatDistance(ride.totalDistanceMiles, units)} • ~${ride.estimatedMinutes}m",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Ride Temperature Span Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "${CyclingMath.formatTemp(minTemp, units)} – ${CyclingMath.formatTemp(maxTemp, units)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Ride Temp Range",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ride Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💨 Avg Wind: ${CyclingMath.formatSpeed(avgWind, units)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                        Text(
                            text = "💧 Rain: $maxRain%",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (maxRain >= 40) HeadwindRed else TailwindGreen
                        )
                        Text(
                            text = "☀️ UV: ${maxUv.roundToInt()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = UvPurple
                        )
                        Text(
                            text = "🌇 $sunset",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeduskoGold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Waypoint Temperature & Wind Breakdown Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ride.waypoints.forEachIndexed { idx, wp ->
                            val w = wp.weather
                            val dotColor = when (wp.windImpact) {
                                WindImpact.TAILWIND -> TailwindGreen
                                WindImpact.HEADWIND -> HeadwindRed
                                WindImpact.CROSSWIND -> CrosswindAmber
                                WindImpact.CALM -> AccentBlue
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, dotColor.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(128.dp)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${wp.label}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (w != null) {
                                            "${CyclingMath.formatTemp(w.temperatureF, units)} • ${CyclingMath.formatSpeed(w.windSpeedMph, units)}"
                                        } else {
                                            wp.etaLabel
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${wp.etaLabel} • ${wp.windImpact.label}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dotColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onOpenCamerasForRide(ride) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Route Cams", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onLoadRideIntoPlanner(ride) },
                            modifier = Modifier.weight(1.25f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Open on Map", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                        }

                        if (savedRides.size > 1) {
                            IconButton(
                                onClick = { onDeleteRide(ride.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Ride",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 4: OPTIONS (MAPPING, WEATHER API, PUBLIC CAMERAS, UNITS & ICON)
// ============================================================================
@Composable
private fun OptionsTab(
    mappingOption: MappingOption,
    weatherApiOption: WeatherApiOption,
    cameraFeedOption: CameraFeedOption,
    units: UnitSystem,
    appIconStyle: AppIconStyle,
    weatherApiKey: String,
    openWeatherMapKey: String,
    windyApiKey: String,
    onMappingOptionSelected: (MappingOption) -> Unit,
    onWeatherApiSelected: (WeatherApiOption) -> Unit,
    onCameraFeedSelected: (CameraFeedOption) -> Unit,
    onUnitSystemSelected: (UnitSystem) -> Unit,
    onAppIconStyleSelected: (AppIconStyle) -> Unit,
    onSaveApiKeys: (String, String, String) -> Unit
) {
    var localWeatherApiKey by remember(weatherApiKey) { mutableStateOf(weatherApiKey) }
    var localOwmKey by remember(openWeatherMapKey) { mutableStateOf(openWeatherMapKey) }
    var localWindyKey by remember(windyApiKey) { mutableStateOf(windyApiKey) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // 1. MAPPING ENGINE OPTIONS (First 2 from research, default to OpenFreeMap)
        item {
            OptionSectionCard(
                header = "1. MAPPING OPTIONS (FREE & API-FRIENDLY)",
                subtitle = "Select your map rendering engine for route planning (Default: OpenFreeMap)"
            ) {
                MappingOption.entries.forEach { option ->
                    SelectableOptionRow(
                        title = option.title,
                        badge = option.badge,
                        description = option.description,
                        isSelected = mappingOption == option,
                        onClick = { onMappingOptionSelected(option) }
                    )
                }
            }
        }

        // 2. WEATHER & MICROCLIMATE API OPTIONS (All 4 selectable, default to Open-Meteo)
        item {
            OptionSectionCard(
                header = "2. WEATHER & MICROCLIMATE APIS",
                subtitle = "Select your forecast & closest station telemetry engine (Default: Open-Meteo)"
            ) {
                WeatherApiOption.entries.forEach { option ->
                    SelectableOptionRow(
                        title = option.title,
                        badge = option.badge,
                        description = option.description,
                        isSelected = weatherApiOption == option,
                        onClick = { onWeatherApiSelected(option) }
                    )
                }
            }
        }

        // 3. PUBLIC CAMERA FEEDS (Both selectable, default to Windy Webcams)
        item {
            OptionSectionCard(
                header = "3. PUBLIC CAMERA FEED SOURCES",
                subtitle = "Select your roadside & mountain pass camera provider (Default: Windy Webcams)"
            ) {
                CameraFeedOption.entries.forEach { option ->
                    SelectableOptionRow(
                        title = option.title,
                        badge = option.badge,
                        description = option.description,
                        isSelected = cameraFeedOption == option,
                        onClick = { onCameraFeedSelected(option) }
                    )
                }
            }
        }

        // 4. MEASUREMENT UNITS & SELECTABLE APP ICONS (Gold & Classic)
        item {
            OptionSectionCard(
                header = "4. MEASUREMENT UNITS & LAUNCHER ICON",
                subtitle = "Customize temperature/wind units and Medusko launcher icon aesthetic"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnitSystem.entries.forEach { u ->
                        val selected = units == u
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUnitSystemSelected(u) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = u.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppIconStyle.entries.forEach { style ->
                        val selected = appIconStyle == style
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAppIconStyleSelected(style) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(
                                        id = if (style == AppIconStyle.GOLD) R.drawable.ic_app_gold else R.drawable.ic_app_classic
                                    ),
                                    contentDescription = style.label,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Text(
                                    text = style.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. OPTIONAL PROVIDER API KEYS
        item {
            OptionSectionCard(
                header = "5. OPTIONAL PROVIDER API KEYS",
                subtitle = "Open-Meteo, NOAA, OpenFreeMap, OSMDroid & DOT 511 require ZERO keys. Enter optional keys below if using custom WeatherAPI, OpenWeatherMap, or Windy Pro tiers."
            ) {
                OutlinedTextField(
                    value = localWeatherApiKey,
                    onValueChange = { localWeatherApiKey = it },
                    label = { Text("WeatherAPI.com Key (Optional)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = localOwmKey,
                    onValueChange = { localOwmKey = it },
                    label = { Text("OpenWeatherMap API Key (Optional)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = localWindyKey,
                    onValueChange = { localWindyKey = it },
                    label = { Text("Windy Webcams v3 Key (Optional)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSaveApiKeys(localWeatherApiKey, localOwmKey, localWindyKey) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Optional Keys", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun OptionSectionCard(
    header: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = header,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun SelectableOptionRow(
    title: String,
    badge: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
