package com.medusko.velocast.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medusko.velocast.data.AppPreferences
import com.medusko.velocast.data.CyclingSuburbsDatabase
import com.medusko.velocast.data.GeocodedLocation
import com.medusko.velocast.data.VelocastRepository
import com.medusko.velocast.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class VelocastViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = AppPreferences(application)
    private val repository = VelocastRepository(prefs)

    // Settings State
    private val _mappingOption = MutableStateFlow(prefs.mappingOption)
    val mappingOption: StateFlow<MappingOption> = _mappingOption.asStateFlow()

    private val _mapStyleOption = MutableStateFlow(prefs.mapStyleOption)
    val mapStyleOption: StateFlow<MapStyleOption> = _mapStyleOption.asStateFlow()

    private val _weatherApiOption = MutableStateFlow(prefs.weatherApiOption)
    val weatherApiOption: StateFlow<WeatherApiOption> = _weatherApiOption.asStateFlow()

    private val _cameraFeedOption = MutableStateFlow(prefs.cameraFeedOption)
    val cameraFeedOption: StateFlow<CameraFeedOption> = _cameraFeedOption.asStateFlow()

    private val _unitPreferences = MutableStateFlow(prefs.unitPreferences)
    val unitPreferences: StateFlow<UnitPreferences> = _unitPreferences.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _confirmWaypointDelete = MutableStateFlow(prefs.confirmWaypointDelete)
    val confirmWaypointDelete: StateFlow<Boolean> = _confirmWaypointDelete.asStateFlow()

    private val _weatherApiKey = MutableStateFlow(prefs.weatherApiKey)
    val weatherApiKey: StateFlow<String> = _weatherApiKey.asStateFlow()

    private val _openWeatherMapKey = MutableStateFlow(prefs.openWeatherMapKey)
    val openWeatherMapKey: StateFlow<String> = _openWeatherMapKey.asStateFlow()

    private val _windyApiKey = MutableStateFlow(prefs.windyApiKey)
    val windyApiKey: StateFlow<String> = _windyApiKey.asStateFlow()

    // Tab 1: Saved Places
    private val _savedPlaces = MutableStateFlow(prefs.loadSavedPlaces())
    val savedPlaces: StateFlow<List<CyclingPlace>> = _savedPlaces.asStateFlow()

    private val _isRefreshingPlaces = MutableStateFlow(false)
    val isRefreshingPlaces: StateFlow<Boolean> = _isRefreshingPlaces.asStateFlow()

    private val _placeSearchResults = MutableStateFlow<List<GeocodedLocation>>(emptyList())
    val placeSearchResults: StateFlow<List<GeocodedLocation>> = _placeSearchResults.asStateFlow()

    // Tab 2: Plan a Ride (Up to 20 points)
    private val _plannedWaypoints = MutableStateFlow<List<RouteWaypoint>>(
        prefs.loadSavedRides().firstOrNull()?.waypoints ?: emptyList()
    )
    val plannedWaypoints: StateFlow<List<RouteWaypoint>> = _plannedWaypoints.asStateFlow()

    private val _routeGeometry = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routeGeometry: StateFlow<List<Pair<Double, Double>>> = _routeGeometry.asStateFlow()

    private val _plannedDistanceMiles = MutableStateFlow(18.4)
    val plannedDistanceMiles: StateFlow<Double> = _plannedDistanceMiles.asStateFlow()

    private val _plannedDurationMinutes = MutableStateFlow(74)
    val plannedDurationMinutes: StateFlow<Int> = _plannedDurationMinutes.asStateFlow()

    private val _riderSpeedMph = MutableStateFlow(15.0)
    val riderSpeedMph: StateFlow<Double> = _riderSpeedMph.asStateFlow()

    private val _startHourOffset = MutableStateFlow(0)
    val startHourOffset: StateFlow<Int> = _startHourOffset.asStateFlow()

    private val _customDateLabel = MutableStateFlow<String?>(null)
    val customDateLabel: StateFlow<String?> = _customDateLabel.asStateFlow()

    private val _showCamerasOnMap = MutableStateFlow(true)
    val showCamerasOnMap: StateFlow<Boolean> = _showCamerasOnMap.asStateFlow()

    private val _isCalculatingRoute = MutableStateFlow(false)
    val isCalculatingRoute: StateFlow<Boolean> = _isCalculatingRoute.asStateFlow()

    private val _waypointSearchResults = MutableStateFlow<List<GeocodedLocation>>(emptyList())
    val waypointSearchResults: StateFlow<List<GeocodedLocation>> = _waypointSearchResults.asStateFlow()

    // Tab 3: Saved Rides
    private val _savedRides = MutableStateFlow(prefs.loadSavedRides())
    val savedRides: StateFlow<List<SavedRide>> = _savedRides.asStateFlow()

    private val _isRefreshingRides = MutableStateFlow(false)
    val isRefreshingRides: StateFlow<Boolean> = _isRefreshingRides.asStateFlow()

    // Camera Feeds State
    private val _routeCameras = MutableStateFlow<List<PublicCameraFeed>>(emptyList())
    val routeCameras: StateFlow<List<PublicCameraFeed>> = _routeCameras.asStateFlow()

    private val _modalCameras = MutableStateFlow<List<PublicCameraFeed>>(emptyList())
    val modalCameras: StateFlow<List<PublicCameraFeed>> = _modalCameras.asStateFlow()

    init {
        refreshAllPlacesWeather()
        recalculateActiveRoute()
        refreshSavedRidesWeather()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLastKnownLocation(): Location? {
        val app = getApplication<Application>()
        val fineGranted = ContextCompat.checkSelfPermission(
            app,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            app,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return null

        return try {
            val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            providers.mapNotNull { provider ->
                if (lm.isProviderEnabled(provider)) lm.getLastKnownLocation(provider) else null
            }.maxByOrNull { it.time }
        } catch (_: Exception) {
            null
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!prefs.hasResolvedInitialGpsPlaces) {
            prefs.hasResolvedInitialGpsPlaces = true
            val loc = if (granted) getDeviceLastKnownLocation() else null
            val resolvedPlaces = CyclingSuburbsDatabase.resolvePlacesForLocation(
                lat = loc?.latitude,
                lon = loc?.longitude
            )
            _savedPlaces.value = resolvedPlaces
            prefs.savePlaces(resolvedPlaces)
            refreshAllPlacesWeather()
        } else if (granted) {
            refreshAllPlacesWeather()
        }
    }

    fun pickLocalDestinationForPlacesFromGps() {
        val loc = getDeviceLastKnownLocation()
        val targetPlaces = CyclingSuburbsDatabase.resolvePlacesForLocation(loc?.latitude, loc?.longitude)
        val merged = (targetPlaces + _savedPlaces.value).distinctBy { "${it.name}_${it.region}" }
        _savedPlaces.value = merged
        prefs.savePlaces(merged)
        refreshAllPlacesWeather()
    }

    fun pickLocalDestinationForRouteFromGps() {
        val current = _plannedWaypoints.value
        if (current.size >= 20) return
        val loc = getDeviceLastKnownLocation()
        val spot = if (loc != null) {
            CyclingSuburbsDatabase.findClosestCyclingSpotTo(loc.latitude, loc.longitude)
        } else {
            CyclingSuburbsDatabase.fallbackDefaultPlaces.first()
        }
        val next = current + RouteWaypoint(
            index = current.size,
            label = "${spot.name} (${spot.region})",
            latitude = spot.latitude,
            longitude = spot.longitude
        )
        _plannedWaypoints.value = next
        recalculateActiveRoute()
    }

    fun refreshAllPlacesWeather() {
        viewModelScope.launch {
            _isRefreshingPlaces.value = true
            val currentList = _savedPlaces.value
            val updated = coroutineScope {
                currentList.map { place ->
                    async {
                        val w = repository.fetchWeatherForLocation(place.latitude, place.longitude, 0)
                        place.copy(
                            region = CyclingMath.ensureCountryInRegion(place.region),
                            elevationFeet = if (w.elevationFeet > 0) w.elevationFeet else place.elevationFeet,
                            weather = w
                        )
                    }
                }.awaitAll()
            }
            _savedPlaces.value = updated
            _isRefreshingPlaces.value = false
        }
    }

    fun searchPlacesToAdd(query: String) {
        viewModelScope.launch {
            if (query.trim().length < 2) {
                _placeSearchResults.value = emptyList()
                return@launch
            }
            _placeSearchResults.value = repository.searchLocations(query)
        }
    }

    fun addCyclingPlace(loc: GeocodedLocation) {
        viewModelScope.launch {
            val w = repository.fetchWeatherForLocation(loc.latitude, loc.longitude, 0)
            val newPlace = CyclingPlace(
                id = "place_${UUID.randomUUID().toString().take(8)}",
                name = loc.name,
                region = CyclingMath.ensureCountryInRegion(loc.region),
                latitude = loc.latitude,
                longitude = loc.longitude,
                elevationFeet = if (loc.elevationFeet > 0) loc.elevationFeet else w.elevationFeet,
                tags = listOf("Custom Cycling Spot"),
                weather = w
            )
            val updated = listOf(newPlace) + _savedPlaces.value
            _savedPlaces.value = updated
            prefs.savePlaces(updated)
            _placeSearchResults.value = emptyList()
        }
    }

    fun removeCyclingPlace(placeId: String) {
        val current = _savedPlaces.value
        if (current.size <= 1) return
        val updated = current.filterNot { it.id == placeId }
        _savedPlaces.value = updated
        prefs.savePlaces(updated)
    }

    // Route Planning (Up to 20 points)
    fun recalculateActiveRoute() {
        val pts = _plannedWaypoints.value
        if (pts.isEmpty()) {
            _routeGeometry.value = emptyList()
            _plannedDistanceMiles.value = 0.0
            _plannedDurationMinutes.value = 0
            return
        }
        viewModelScope.launch {
            _isCalculatingRoute.value = true
            val result = repository.calculateRouteForecast(
                rawPoints = pts,
                avgSpeedMph = _riderSpeedMph.value,
                startHourOffset = _startHourOffset.value
            )
            _plannedWaypoints.value = result.waypoints
            _routeGeometry.value = result.geometry
            _plannedDistanceMiles.value = result.totalDistanceMiles
            _plannedDurationMinutes.value = result.estimatedMinutes
            _isCalculatingRoute.value = false

            val first = result.waypoints.firstOrNull()
            if (first != null) {
                _routeCameras.value = repository.fetchPublicCamerasNear(first.latitude, first.longitude)
            }
        }
    }

    fun addWaypointFromMapTap(lat: Double, lon: Double) {
        val current = _plannedWaypoints.value
        if (current.size >= 20) return
        val idx = current.size
        val closestSpot = CyclingSuburbsDatabase.findClosestCyclingSpotTo(lat, lon)
        val distToSpot = CyclingMath.haversineMiles(lat, lon, closestSpot.latitude, closestSpot.longitude)
        val label = if (distToSpot <= 2.5) {
            closestSpot.name
        } else {
            String.format(Locale.US, "Point %d (%.3f, %.3f)", idx + 1, lat, lon)
        }
        val next = current + RouteWaypoint(
            index = idx,
            label = label,
            latitude = lat,
            longitude = lon
        )
        _plannedWaypoints.value = next
        recalculateActiveRoute()
    }

    fun searchWaypointAddress(query: String) {
        viewModelScope.launch {
            if (query.trim().length < 2) {
                _waypointSearchResults.value = emptyList()
                return@launch
            }
            _waypointSearchResults.value = repository.searchLocations(query)
        }
    }

    fun addWaypointFromGeocoded(loc: GeocodedLocation) {
        val current = _plannedWaypoints.value
        if (current.size >= 20) return
        val idx = current.size
        val next = current + RouteWaypoint(
            index = idx,
            label = "${loc.name} (${loc.region})",
            latitude = loc.latitude,
            longitude = loc.longitude
        )
        _plannedWaypoints.value = next
        _waypointSearchResults.value = emptyList()
        recalculateActiveRoute()
    }

    fun removeWaypointAt(index: Int) {
        val current = _plannedWaypoints.value.toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        _plannedWaypoints.value = current.mapIndexed { idx, wp -> wp.copy(index = idx) }
        recalculateActiveRoute()
    }

    fun moveWaypoint(fromIndex: Int, toIndex: Int) {
        val current = _plannedWaypoints.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _plannedWaypoints.value = current.mapIndexed { idx, wp -> wp.copy(index = idx) }
        recalculateActiveRoute()
    }

    fun clearAllWaypoints() {
        _plannedWaypoints.value = emptyList()
        _routeGeometry.value = emptyList()
        _plannedDistanceMiles.value = 0.0
        _plannedDurationMinutes.value = 0
    }

    fun reverseWaypoints() {
        val reversed = _plannedWaypoints.value.reversed().mapIndexed { idx, wp -> wp.copy(index = idx) }
        _plannedWaypoints.value = reversed
        recalculateActiveRoute()
    }

    fun startRideFromPlace(place: CyclingPlace) {
        val startWp = RouteWaypoint(
            index = 0,
            label = place.name,
            latitude = place.latitude,
            longitude = place.longitude,
            weather = place.weather
        )
        val finishWp = RouteWaypoint(
            index = 1,
            label = "${place.name} Overlook Loop",
            latitude = place.latitude + 0.032,
            longitude = place.longitude - 0.028
        )
        _plannedWaypoints.value = listOf(startWp, finishWp)
        recalculateActiveRoute()
    }

    fun setRiderSpeed(mph: Double) {
        _riderSpeedMph.value = mph
        recalculateActiveRoute()
    }

    fun setStartHourOffset(offsetHours: Int) {
        _customDateLabel.value = null
        _startHourOffset.value = offsetHours
        recalculateActiveRoute()
    }

    fun setCustomPlannedDate(utcMillis: Long) {
        val nowMillis = System.currentTimeMillis()
        val diffHours = max(0L, (utcMillis - nowMillis) / (1000L * 60L * 60L)).toInt() + 8
        val fmt = SimpleDateFormat("MMM d", Locale.US)
        val cal = Calendar.getInstance().apply { timeInMillis = utcMillis }
        _customDateLabel.value = fmt.format(Date(cal.timeInMillis))
        _startHourOffset.value = diffHours.coerceIn(0, 336)
        recalculateActiveRoute()
    }

    fun toggleShowCamerasOnMap() {
        _showCamerasOnMap.value = !_showCamerasOnMap.value
    }

    fun suggestedRideName(): String {
        val pts = _plannedWaypoints.value
        if (pts.isEmpty()) return "Morning Cycling Route"
        val startShort = pts.first().label.substringBefore(" (").substringBefore(" •")
        val endShort = pts.last().label.substringBefore(" (").substringBefore(" •")
        return if (pts.size == 1 || startShort == endShort) {
            "$startShort Loop"
        } else {
            "$startShort to $endShort"
        }
    }

    fun saveCurrentPlannedRide(customName: String) {
        val pts = _plannedWaypoints.value
        if (pts.isEmpty()) return
        val rideName = customName.trim().ifBlank { suggestedRideName() }
        val firstPt = pts.first()
        val closestPlaceName = CyclingSuburbsDatabase.findClosestPlaceName(
            lat = firstPt.latitude,
            lon = firstPt.longitude,
            userSavedPlaces = _savedPlaces.value
        )
        val regionDesc = "${pts.size} Waypoints • ${
            CyclingMath.formatDistance(_plannedDistanceMiles.value, _unitPreferences.value)
        }"
        val newRide = SavedRide(
            id = "ride_${UUID.randomUUID().toString().take(8)}",
            name = rideName,
            closestPlaceName = closestPlaceName,
            region = regionDesc,
            totalDistanceMiles = _plannedDistanceMiles.value,
            estimatedMinutes = _plannedDurationMinutes.value,
            averageSpeedMph = _riderSpeedMph.value,
            waypoints = pts,
            routeGeometry = _routeGeometry.value
        )
        val updated = listOf(newRide) + _savedRides.value
        _savedRides.value = updated
        prefs.saveRides(updated)
    }

    fun loadSavedRideIntoPlanner(ride: SavedRide) {
        _plannedWaypoints.value = ride.waypoints
        _riderSpeedMph.value = ride.averageSpeedMph
        recalculateActiveRoute()
    }

    fun deleteSavedRide(rideId: String) {
        val current = _savedRides.value
        if (current.size <= 1) return
        val updated = current.filterNot { it.id == rideId }
        _savedRides.value = updated
        prefs.saveRides(updated)
    }

    fun refreshSavedRidesWeather() {
        viewModelScope.launch {
            _isRefreshingRides.value = true
            val currentRides = _savedRides.value
            val currentPlaces = _savedPlaces.value
            val updatedRides = currentRides.map { ride ->
                val calc = repository.calculateRouteForecast(
                    rawPoints = ride.waypoints,
                    avgSpeedMph = ride.averageSpeedMph,
                    startHourOffset = 0
                )
                val firstPt = calc.waypoints.firstOrNull()
                val closestPlace = ride.closestPlaceName.ifBlank {
                    if (firstPt != null) {
                        CyclingSuburbsDatabase.findClosestPlaceName(firstPt.latitude, firstPt.longitude, currentPlaces)
                    } else {
                        "Marin Headlands • Hawk Hill (Sausalito, CA, USA)"
                    }
                }
                ride.copy(
                    closestPlaceName = closestPlace,
                    waypoints = calc.waypoints,
                    routeGeometry = calc.geometry,
                    totalDistanceMiles = if (calc.totalDistanceMiles > 0.1) calc.totalDistanceMiles else ride.totalDistanceMiles,
                    estimatedMinutes = if (calc.estimatedMinutes > 0) calc.estimatedMinutes else ride.estimatedMinutes
                )
            }
            _savedRides.value = updatedRides
            _isRefreshingRides.value = false
        }
    }

    fun loadModalCamerasNear(lat: Double, lon: Double) {
        viewModelScope.launch {
            _modalCameras.value = repository.fetchPublicCamerasNear(lat, lon)
        }
    }

    // Settings Mutators
    fun updateMappingOption(option: MappingOption) {
        prefs.mappingOption = option
        _mappingOption.value = option
    }

    fun updateMapStyleOption(style: MapStyleOption) {
        prefs.mapStyleOption = style
        _mapStyleOption.value = style
    }

    fun updateWeatherApiOption(option: WeatherApiOption) {
        prefs.weatherApiOption = option
        _weatherApiOption.value = option
        refreshAllPlacesWeather()
        recalculateActiveRoute()
        refreshSavedRidesWeather()
    }

    fun updateCameraFeedOption(option: CameraFeedOption) {
        prefs.cameraFeedOption = option
        _cameraFeedOption.value = option
    }

    fun updateTempUnit(unit: TempUnit) {
        val next = _unitPreferences.value.copy(tempUnit = unit)
        prefs.unitPreferences = next
        _unitPreferences.value = next
    }

    fun updateSpeedUnit(unit: SpeedUnit) {
        val next = _unitPreferences.value.copy(speedUnit = unit)
        prefs.unitPreferences = next
        _unitPreferences.value = next
    }

    fun updateDistanceUnit(unit: DistanceUnit) {
        val next = _unitPreferences.value.copy(distanceUnit = unit)
        prefs.unitPreferences = next
        _unitPreferences.value = next
    }

    fun updateElevationUnit(unit: ElevationUnit) {
        val next = _unitPreferences.value.copy(elevationUnit = unit)
        prefs.unitPreferences = next
        _unitPreferences.value = next
    }

    fun updateConfirmWaypointDelete(confirm: Boolean) {
        prefs.confirmWaypointDelete = confirm
        _confirmWaypointDelete.value = confirm
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        prefs.isDarkMode = next
        _isDarkMode.value = next
    }

    fun updateApiKeys(weatherKey: String, owmKey: String, windyKey: String) {
        prefs.weatherApiKey = weatherKey
        prefs.openWeatherMapKey = owmKey
        prefs.windyApiKey = windyKey
        _weatherApiKey.value = weatherKey
        _openWeatherMapKey.value = owmKey
        _windyApiKey.value = windyKey
        refreshAllPlacesWeather()
        recalculateActiveRoute()
    }
}
