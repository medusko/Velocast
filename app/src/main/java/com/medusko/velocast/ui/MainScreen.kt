package com.medusko.velocast.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.medusko.velocast.R
import com.medusko.velocast.data.GeocodedLocation
import com.medusko.velocast.model.*
import com.medusko.velocast.ui.components.*
import com.medusko.velocast.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MainScreen(viewModel: VelocastViewModel) {
    val context = LocalContext.current

    val mappingOption by viewModel.mappingOption.collectAsState()
    val mapStyleOption by viewModel.mapStyleOption.collectAsState()
    val weatherApiOption by viewModel.weatherApiOption.collectAsState()
    val cameraFeedOption by viewModel.cameraFeedOption.collectAsState()
    val units by viewModel.unitPreferences.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val confirmWaypointDelete by viewModel.confirmWaypointDelete.collectAsState()

    val savedPlaces by viewModel.savedPlaces.collectAsState()
    val isRefreshingPlaces by viewModel.isRefreshingPlaces.collectAsState()
    val placeSearchResults by viewModel.placeSearchResults.collectAsState()

    val plannedWaypoints by viewModel.plannedWaypoints.collectAsState()
    val routeGeometry by viewModel.routeGeometry.collectAsState()
    val plannedDistanceMiles by viewModel.plannedDistanceMiles.collectAsState()
    val plannedDurationMinutes by viewModel.plannedDurationMinutes.collectAsState()
    val riderSpeedMph by viewModel.riderSpeedMph.collectAsState()
    val startHourOffset by viewModel.startHourOffset.collectAsState()
    val customDateLabel by viewModel.customDateLabel.collectAsState()
    val showCamerasOnMap by viewModel.showCamerasOnMap.collectAsState()
    val isCalculatingRoute by viewModel.isCalculatingRoute.collectAsState()
    val waypointSearchResults by viewModel.waypointSearchResults.collectAsState()
    val routeCameras by viewModel.routeCameras.collectAsState()
    val modalCameras by viewModel.modalCameras.collectAsState()

    val savedRides by viewModel.savedRides.collectAsState()
    val isRefreshingRides by viewModel.isRefreshingRides.collectAsState()

    val weatherApiKey by viewModel.weatherApiKey.collectAsState()
    val openWeatherMapKey by viewModel.openWeatherMapKey.collectAsState()
    val windyApiKey by viewModel.windyApiKey.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Camera dialog state
    var activeCameraDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeCameraDialogWeather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var activeCameraInitialSelection by remember { mutableStateOf<PublicCameraFeed?>(null) }

    // Pending GPS action after permission prompt
    var pendingGpsAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
        if (granted) {
            pendingGpsAction?.invoke()
        }
        pendingGpsAction = null
    }

    // Request location permission on first launch to establish nearby suburb places
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.onLocationPermissionResult(true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            // Clean Top Bar: only Velocast icon + "Velocast" title + action icons
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_gold),
                                contentDescription = "Velocast Icon",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "Velocast",
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = "Plan a Ride") },
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
                    onPickLocalGpsPlaces = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            viewModel.pickLocalDestinationForPlacesFromGps()
                        } else {
                            pendingGpsAction = { viewModel.pickLocalDestinationForPlacesFromGps() }
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onOpenCamerasForPlace = { place ->
                        viewModel.loadModalCamerasNear(place.latitude, place.longitude)
                        activeCameraDialogTitle = place.name
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
                    mapStyleOption = mapStyleOption,
                    waypoints = plannedWaypoints,
                    routeGeometry = routeGeometry,
                    totalDistanceMiles = plannedDistanceMiles,
                    estimatedMinutes = plannedDurationMinutes,
                    riderSpeedMph = riderSpeedMph,
                    startHourOffset = startHourOffset,
                    customDateLabel = customDateLabel,
                    showCamerasOnMap = showCamerasOnMap,
                    confirmWaypointDelete = confirmWaypointDelete,
                    isCalculating = isCalculatingRoute,
                    waypointSearchResults = waypointSearchResults,
                    units = units,
                    camerasNearRoute = routeCameras,
                    suggestedRideName = viewModel.suggestedRideName(),
                    onMapStyleSelected = viewModel::updateMapStyleOption,
                    onMapTapAddWaypoint = viewModel::addWaypointFromMapTap,
                    onSearchWaypoint = viewModel::searchWaypointAddress,
                    onSelectGeocodedWaypoint = viewModel::addWaypointFromGeocoded,
                    onPickGpsWaypoint = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            viewModel.pickLocalDestinationForRouteFromGps()
                        } else {
                            pendingGpsAction = { viewModel.pickLocalDestinationForRouteFromGps() }
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onRemoveWaypointAt = viewModel::removeWaypointAt,
                    onMoveWaypoint = viewModel::moveWaypoint,
                    onClearWaypoints = viewModel::clearAllWaypoints,
                    onReverseWaypoints = viewModel::reverseWaypoints,
                    onToggleMapCameras = viewModel::toggleShowCamerasOnMap,
                    onSpeedSelected = viewModel::setRiderSpeed,
                    onHourOffsetSelected = viewModel::setStartHourOffset,
                    onCustomDateSelected = viewModel::setCustomPlannedDate,
                    onSaveRide = { name ->
                        viewModel.saveCurrentPlannedRide(name)
                        selectedTab = 2
                    },
                    onOpenCameraModal = { title, lat, lon, weather, initialCam ->
                        viewModel.loadModalCamerasNear(lat, lon)
                        activeCameraDialogTitle = title
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
                            viewModel.loadModalCamerasNear(mid.latitude, mid.longitude)
                            activeCameraDialogTitle = ride.name
                            activeCameraDialogWeather = mid.weather
                            activeCameraInitialSelection = null
                        }
                    },
                    onDeleteRide = viewModel::deleteSavedRide
                )

                3 -> OptionsTab(
                    mappingOption = mappingOption,
                    mapStyleOption = mapStyleOption,
                    weatherApiOption = weatherApiOption,
                    cameraFeedOption = cameraFeedOption,
                    units = units,
                    confirmWaypointDelete = confirmWaypointDelete,
                    weatherApiKey = weatherApiKey,
                    openWeatherMapKey = openWeatherMapKey,
                    windyApiKey = windyApiKey,
                    onMappingOptionSelected = viewModel::updateMappingOption,
                    onMapStyleSelected = viewModel::updateMapStyleOption,
                    onWeatherApiSelected = viewModel::updateWeatherApiOption,
                    onCameraFeedSelected = viewModel::updateCameraFeedOption,
                    onTempUnitSelected = viewModel::updateTempUnit,
                    onSpeedUnitSelected = viewModel::updateSpeedUnit,
                    onDistanceUnitSelected = viewModel::updateDistanceUnit,
                    onElevationUnitSelected = viewModel::updateElevationUnit,
                    onConfirmWaypointDeleteChanged = viewModel::updateConfirmWaypointDelete,
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
            distanceUnit = units.distanceUnit,
            onDismiss = { showAboutDialog = false }
        )
    }

    activeCameraDialogTitle?.let { title ->
        CameraFeedDialog(
            placeTitle = title,
            cameras = modalCameras,
            activeSourceOption = cameraFeedOption,
            weather = activeCameraDialogWeather,
            units = units,
            initialSelectedCamera = activeCameraInitialSelection,
            onDismiss = { activeCameraDialogTitle = null }
        )
    }
}

@Composable
private fun CompactSingleLineSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty() && onClear != null) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClear() }
                )
            }
        }
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
    units: UnitPreferences,
    onSearchQueryChanged: (String) -> Unit,
    onAddPlace: (GeocodedLocation) -> Unit,
    onRemovePlace: (String) -> Unit,
    onPickLocalGpsPlaces: () -> Unit,
    onOpenCamerasForPlace: (CyclingPlace) -> Unit,
    onPlanRideFromPlace: (CyclingPlace) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedPlaceId by remember { mutableStateOf<String?>(places.firstOrNull()?.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        // Compact 1-Line Search Bar + GPS Local Destination Icon Button
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactSingleLineSearchField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onSearchQueryChanged(it)
                        },
                        placeholder = "Add cycling spot (e.g. Mt Diablo, Palomar)...",
                        onClear = {
                            searchQuery = ""
                            onSearchQueryChanged("")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // GPS Local Destination Icon Button
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onPickLocalGpsPlaces() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Use Phone GPS Local Cycling Destinations",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (searchResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        searchResults.forEach { loc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddPlace(loc)
                                        searchQuery = ""
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelemetryMetricTile(
                                label = "WIND & GUSTS",
                                primaryValue = "${CyclingMath.formatSpeed(w.windSpeedMph, units)} ${w.windCardinal}",
                                subValue = "Gusts ${CyclingMath.formatSpeed(w.windGustMph, units)}",
                                accentColor = if (w.windSpeedMph > 16) CrosswindAmber else AccentBlue,
                                windArrowDeg = w.windDirectionDeg,
                                modifier = Modifier.weight(1.15f)
                            )

                            TelemetryMetricTile(
                                label = "RAIN CHANCE",
                                primaryValue = "${w.rainChancePercent}%",
                                subValue = w.conditionLabel,
                                accentColor = if (w.rainChancePercent >= 40) HeadwindRed else TailwindGreen,
                                modifier = Modifier.weight(0.95f)
                            )

                            TelemetryMetricTile(
                                label = "UV INDEX",
                                primaryValue = String.format("%.1f", w.uvIndex),
                                subValue = CyclingMath.uvSeverityLabel(w.uvIndex),
                                accentColor = UvPurple,
                                modifier = Modifier.weight(0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Vector Sunrise / Sunset & Closest Weather Station Bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SunriseVectorIcon(size = 18.dp)
                                        Text(
                                            text = "Sunrise: ${w.sunriseTime}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SunsetVectorIcon(size = 18.dp)
                                        Text(
                                            text = "Sunset: ${w.sunsetTime}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = AccentBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Station: ${w.closestStationId} (${w.closestStationName})",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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

                        // Expandable 24-Hour Cycling Wind & Weather Timeline
                        AnimatedVisibility(visible = isExpanded && w.hourly.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "24-HOUR CYCLING WIND & WEATHER TIMELINE",
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
                                                    text = "${hr.precipProbPercent}% Rain • UV ${hr.uvIndex.roundToInt()}",
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
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
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
// TAB 2: PLAN A RIDE (20 WAYPOINTS + MAP + REORDER + SWIPE DELETE + CUSTOM DATE)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanARideTab(
    mappingOption: MappingOption,
    mapStyleOption: MapStyleOption,
    waypoints: List<RouteWaypoint>,
    routeGeometry: List<Pair<Double, Double>>,
    totalDistanceMiles: Double,
    estimatedMinutes: Int,
    riderSpeedMph: Double,
    startHourOffset: Int,
    customDateLabel: String?,
    showCamerasOnMap: Boolean,
    confirmWaypointDelete: Boolean,
    isCalculating: Boolean,
    waypointSearchResults: List<GeocodedLocation>,
    units: UnitPreferences,
    camerasNearRoute: List<PublicCameraFeed>,
    suggestedRideName: String,
    onMapStyleSelected: (MapStyleOption) -> Unit,
    onMapTapAddWaypoint: (Double, Double) -> Unit,
    onSearchWaypoint: (String) -> Unit,
    onSelectGeocodedWaypoint: (GeocodedLocation) -> Unit,
    onPickGpsWaypoint: () -> Unit,
    onRemoveWaypointAt: (Int) -> Unit,
    onMoveWaypoint: (Int, Int) -> Unit,
    onClearWaypoints: () -> Unit,
    onReverseWaypoints: () -> Unit,
    onToggleMapCameras: () -> Unit,
    onSpeedSelected: (Double) -> Unit,
    onHourOffsetSelected: (Int) -> Unit,
    onCustomDateSelected: (Long) -> Unit,
    onSaveRide: (String) -> Unit,
    onOpenCameraModal: (String, Double, Double, WeatherSnapshot?, PublicCameraFeed?) -> Unit
) {
    var addressQuery by remember { mutableStateOf("") }
    var rideNameInput by remember(suggestedRideName) { mutableStateOf(suggestedRideName) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var pendingDeleteWaypointIndex by remember { mutableStateOf<Int?>(null) }

    val requestDeleteWaypoint: (Int) -> Unit = { idx ->
        if (confirmWaypointDelete) {
            pendingDeleteWaypointIndex = idx
        } else {
            onRemoveWaypointAt(idx)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        // 1. Interactive Map (25% taller + Map Style switcher + Bike/Camera icons)
        item {
            VelocastMapView(
                mappingOption = mappingOption,
                mapStyleOption = mapStyleOption,
                waypoints = waypoints,
                routeGeometry = routeGeometry,
                cameras = camerasNearRoute,
                showCamerasOnMap = showCamerasOnMap,
                units = units,
                onMapStyleSelected = onMapStyleSelected,
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

        // 2. Route Controls Card
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
                            text = "ROUTE WAYPOINTS (${waypoints.size}/20 POINTS)",
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

                    Spacer(modifier = Modifier.height(6.dp))

                    // Single-line Search Input + GPS Local Destination Icon Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactSingleLineSearchField(
                            value = addressQuery,
                            onValueChange = {
                                addressQuery = it
                                onSearchWaypoint(it)
                            },
                            enabled = waypoints.size < 20,
                            placeholder = if (waypoints.size < 20) {
                                "Add point ${waypoints.size + 1} by address or tap map..."
                            } else {
                                "Maximum 20 route points reached"
                            },
                            onClear = {
                                addressQuery = ""
                                onSearchWaypoint("")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable(enabled = waypoints.size < 20) { onPickGpsWaypoint() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Add Local GPS Destination",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

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

                    // Row 1: Departure Time + Tomorrow + Custom Date Button right next to Tomorrow
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val offsets = listOf(0 to "Now", 1 to "+1h", 2 to "+2h", 4 to "+4h", 24 to "Tomorrow")
                        offsets.forEach { (hr, label) ->
                            FilterChip(
                                selected = customDateLabel == null && startHourOffset == hr,
                                onClick = { onHourOffsetSelected(hr) },
                                leadingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        // Custom Date Button right next to Tomorrow
                        FilterChip(
                            selected = customDateLabel != null,
                            onClick = { showDatePickerModal = true },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = {
                                Text(
                                    text = customDateLabel?.let { "Date: $it" } ?: "Custom Date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }

                    // Row 2: Speed Selector moved to the next row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val speeds = listOf(12.0, 15.0, 18.0, 22.0)
                        speeds.forEach { spd ->
                            FilterChip(
                                selected = abs(riderSpeedMph - spd) < 0.5,
                                onClick = { onSpeedSelected(spd) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        CyclingMath.formatSpeed(spd, units),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    // Explanatory note on how speed & segment distance estimate arrival weather
                    Text(
                        text = "Your selected cycling speed and segment distances calculate when you arrive at each point so weather is forecasted for your exact arrival time.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )

                    // Action Buttons Row (No emojis on Cams / Reverse / Clear)
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
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reverse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onToggleMapCameras,
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (showCamerasOnMap) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (showCamerasOnMap) "Cams: ON" else "Cams: OFF",
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
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Single-line Ride Name Field (with default suggestion) + Save Ride Button next to it
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (rideNameInput.isEmpty()) {
                                    Text(
                                        text = suggestedRideName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = rideNameInput,
                                    onValueChange = { rideNameInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Button(
                            onClick = { onSaveRide(rideNameInput.ifBlank { suggestedRideName }) },
                            enabled = waypoints.size >= 2,
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Save Ride", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // 3. Route Summary Telemetry Card (No emojis)
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
                                text = "ROUTE FORECAST SUMMARY • SWIPE OR ARROWS TO EDIT",
                                fontSize = 9.5.sp,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                StatusColorDot(color = TailwindGreen)
                                Text(
                                    text = "$tailwindCount Tailwind",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                StatusColorDot(color = HeadwindRed)
                                Text(
                                    text = "$headwindCount Headwind",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Waypoint-by-Waypoint Weather Cards (Swipe-to-Delete + Reorder Up/Down)
        itemsIndexed(
            items = waypoints,
            key = { idx, wp -> "wp_${idx}_${wp.latitude}_${wp.longitude}" }
        ) { idx, wp ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                        requestDeleteWaypoint(idx)
                        false
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(14.dp),
                        color = HeadwindRed.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = HeadwindRed)
                            Text("Swipe to Remove Point", color = HeadwindRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = HeadwindRed)
                        }
                    }
                }
            ) {
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
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF0E131D)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = wp.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${CyclingMath.formatDistance(wp.distanceFromStartMiles, units)} • ETA ${wp.etaLabel} • Bearing ${wp.bearingDeg}°",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Reorder Up Button
                                IconButton(
                                    onClick = { onMoveWaypoint(idx, idx - 1) },
                                    enabled = idx > 0,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Point Up",
                                        tint = if (idx > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // Reorder Down Button
                                IconButton(
                                    onClick = { onMoveWaypoint(idx, idx + 1) },
                                    enabled = idx < waypoints.lastIndex,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Point Down",
                                        tint = if (idx < waypoints.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = impactColor.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, impactColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = wp.windImpact.label,
                                        color = impactColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { requestDeleteWaypoint(idx) },
                                    modifier = Modifier.size(26.dp)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = null,
                                        tint = MeduskoGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = CyclingMath.formatTemp(w.temperatureF, units),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = null,
                                        tint = impactColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${CyclingMath.formatSpeed(w.windSpeedMph, units)} ${w.windCardinal} (G:${CyclingMath.formatSpeed(w.windGustMph, units)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = impactColor
                                    )
                                }

                                Text(
                                    text = "Rain ${w.rainChancePercent}% • UV ${w.uvIndex.roundToInt()}",
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${w.closestStationId} (${CyclingMath.formatDistance(w.stationDistanceMiles, units)})",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    SunsetVectorIcon(size = 14.dp)
                                    Text(
                                        text = w.sunsetTime,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable {
                                        onOpenCameraModal(wp.label, wp.latitude, wp.longitude, w, null)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Nearby Cam",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() + 86400000L * 2
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onCustomDateSelected(it) }
                        showDatePickerModal = false
                    }
                ) {
                    Text("Apply Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerModal = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    pendingDeleteWaypointIndex?.let { idxToDelete ->
        val wpName = waypoints.getOrNull(idxToDelete)?.label ?: "Point ${idxToDelete + 1}"
        AlertDialog(
            onDismissRequest = { pendingDeleteWaypointIndex = null },
            title = { Text("Delete Route Point?", fontWeight = FontWeight.Black) },
            text = { Text("Do you want to remove \"$wpName\" from your planned ride?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveWaypointAt(idxToDelete)
                        pendingDeleteWaypointIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeadwindRed, contentColor = Color.White)
                ) {
                    Text("Yes, Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeleteWaypointIndex = null }) {
                    Text("No, Keep", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ============================================================================
// TAB 3: SAVED RIDES (TRIMMED TOP SPACE + CLOSEST PLACE SUBTEXT + DELETE CONFIRM)
// ============================================================================
@Composable
private fun SavedRidesTab(
    savedRides: List<SavedRide>,
    isRefreshing: Boolean,
    units: UnitPreferences,
    onLoadRideIntoPlanner: (SavedRide) -> Unit,
    onOpenCamerasForRide: (SavedRide) -> Unit,
    onDeleteRide: (String) -> Unit
) {
    var ridePendingDelete by remember { mutableStateOf<SavedRide?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)
    ) {
        if (isRefreshing) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
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
                            // Closest Place displayed directly underneath in a slightly subtext font
                            Text(
                                text = ride.closestPlaceName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = "${CyclingMath.formatDistance(ride.totalDistanceMiles, units)} • ~${ride.estimatedMinutes}m",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

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

                    // Ride Metrics Row (Vector Icons, Zero Emojis)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Air, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Avg ${CyclingMath.formatSpeed(avgWind, units)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = if (maxRain >= 40) HeadwindRed else TailwindGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Rain $maxRain%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (maxRain >= 40) HeadwindRed else TailwindGreen
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = UvPurple, modifier = Modifier.size(13.dp))
                            Text(
                                text = "UV ${maxUv.roundToInt()}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = UvPurple
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SunsetVectorIcon(size = 15.dp)
                            Text(
                                text = sunset,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeduskoGold
                            )
                        }
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
                                onClick = { ridePendingDelete = ride },
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

    // Confirmation Dialog when deleting a Saved Ride
    ridePendingDelete?.let { ride ->
        AlertDialog(
            onDismissRequest = { ridePendingDelete = null },
            title = { Text("Delete Saved Ride?", fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to delete \"${ride.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRide(ride.id)
                        ridePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeadwindRed, contentColor = Color.White)
                ) {
                    Text("Yes, Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { ridePendingDelete = null }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ============================================================================
// TAB 4: OPTIONS (MAPPING, MAP TYPE, WEATHER API, CAMERAS, GRANULAR UNITS)
// ============================================================================
@Composable
private fun OptionsTab(
    mappingOption: MappingOption,
    mapStyleOption: MapStyleOption,
    weatherApiOption: WeatherApiOption,
    cameraFeedOption: CameraFeedOption,
    units: UnitPreferences,
    confirmWaypointDelete: Boolean,
    weatherApiKey: String,
    openWeatherMapKey: String,
    windyApiKey: String,
    onMappingOptionSelected: (MappingOption) -> Unit,
    onMapStyleSelected: (MapStyleOption) -> Unit,
    onWeatherApiSelected: (WeatherApiOption) -> Unit,
    onCameraFeedSelected: (CameraFeedOption) -> Unit,
    onTempUnitSelected: (TempUnit) -> Unit,
    onSpeedUnitSelected: (SpeedUnit) -> Unit,
    onDistanceUnitSelected: (DistanceUnit) -> Unit,
    onElevationUnitSelected: (ElevationUnit) -> Unit,
    onConfirmWaypointDeleteChanged: (Boolean) -> Unit,
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
        // 1. MAPPING ENGINE & MAP STYLE OPTIONS
        item {
            OptionSectionCard(
                header = "1. MAPPING ENGINE & LAYER TYPE",
                subtitle = "Select your mapping service (Default: OpenFreeMap) and map style (Regular, Street, Topograph)"
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

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Map Layer Type",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapStyleOption.entries.forEach { style ->
                        val selected = mapStyleOption == style
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMapStyleSelected(style) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. WEATHER & MICROCLIMATE API OPTIONS (Dynamic mi / km resolution!)
        item {
            OptionSectionCard(
                header = "2. WEATHER & MICROCLIMATE APIS",
                subtitle = "Select your forecast & closest station telemetry engine (Default: Open-Meteo)"
            ) {
                WeatherApiOption.entries.forEach { option ->
                    SelectableOptionRow(
                        title = option.localizedTitle(units.distanceUnit),
                        badge = option.badge,
                        description = option.localizedDescription(units.distanceUnit),
                        isSelected = weatherApiOption == option,
                        onClick = { onWeatherApiSelected(option) }
                    )
                }
            }
        }

        // 3. PUBLIC CAMERA FEED SOURCES
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

        // 4. INDEPENDENT MEASUREMENT UNITS (Temperature, Speed, Distance, Elevation)
        item {
            OptionSectionCard(
                header = "4. CUSTOM MEASUREMENT UNITS",
                subtitle = "Mix and match Imperial and Metric units independently for weather, speed, distance, and climbs"
            ) {
                UnitTogglePairRow(
                    categoryLabel = "Temperature",
                    leftLabel = TempUnit.FAHRENHEIT.label,
                    rightLabel = TempUnit.CELSIUS.label,
                    isLeftSelected = units.tempUnit == TempUnit.FAHRENHEIT,
                    onSelectLeft = { onTempUnitSelected(TempUnit.FAHRENHEIT) },
                    onSelectRight = { onTempUnitSelected(TempUnit.CELSIUS) }
                )
                UnitTogglePairRow(
                    categoryLabel = "Wind & Speed",
                    leftLabel = SpeedUnit.MPH.label,
                    rightLabel = SpeedUnit.KMH.label,
                    isLeftSelected = units.speedUnit == SpeedUnit.MPH,
                    onSelectLeft = { onSpeedUnitSelected(SpeedUnit.MPH) },
                    onSelectRight = { onSpeedUnitSelected(SpeedUnit.KMH) }
                )
                UnitTogglePairRow(
                    categoryLabel = "Route Distance",
                    leftLabel = DistanceUnit.MILES.label,
                    rightLabel = DistanceUnit.KILOMETERS.label,
                    isLeftSelected = units.distanceUnit == DistanceUnit.MILES,
                    onSelectLeft = { onDistanceUnitSelected(DistanceUnit.MILES) },
                    onSelectRight = { onDistanceUnitSelected(DistanceUnit.KILOMETERS) }
                )
                UnitTogglePairRow(
                    categoryLabel = "Climb / Elevation",
                    leftLabel = ElevationUnit.FEET.label,
                    rightLabel = ElevationUnit.METERS.label,
                    isLeftSelected = units.elevationUnit == ElevationUnit.FEET,
                    onSelectLeft = { onElevationUnitSelected(ElevationUnit.FEET) },
                    onSelectRight = { onElevationUnitSelected(ElevationUnit.METERS) }
                )
            }
        }

        // 5. ROUTE POINT DELETION CONFIRMATION SETTING (Default: No)
        item {
            OptionSectionCard(
                header = "5. ROUTE PLANNER PREFERENCES",
                subtitle = "Prompt for confirmation before deleting a saved point in Plan a Ride (Default: No)"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Confirm Point Deletion in Plan a Ride",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (confirmWaypointDelete) "Enabled: Yes/No popup shown before deleting a route point"
                            else "Disabled (Default): Points delete immediately on swipe or tap",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = confirmWaypointDelete,
                        onCheckedChange = onConfirmWaypointDeleteChanged
                    )
                }
            }
        }

        // 6. OPTIONAL PROVIDER API KEYS
        item {
            OptionSectionCard(
                header = "6. OPTIONAL PROVIDER API KEYS",
                subtitle = "Enter optional keys below if using custom WeatherAPI, OpenWeatherMap, or Windy tiers"
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
private fun UnitTogglePairRow(
    categoryLabel: String,
    leftLabel: String,
    rightLabel: String,
    isLeftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = categoryLabel,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(leftLabel, isLeftSelected, onSelectLeft),
                Triple(rightLabel, !isLeftSelected, onSelectRight)
            ).forEach { (label, selected, onClick) ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClick() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
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
