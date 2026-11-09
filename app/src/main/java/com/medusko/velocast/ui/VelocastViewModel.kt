package com.medusko.velocast.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medusko.velocast.data.AppPreferences
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
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class VelocastViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = AppPreferences(application)
    private val repository = VelocastRepository(prefs)

    // Settings State
    private val _mappingOption = MutableStateFlow(prefs.mappingOption)
    val mappingOption: StateFlow<MappingOption> = _mappingOption.asStateFlow()

    private val _weatherApiOption = MutableStateFlow(prefs.weatherApiOption)
    val weatherApiOption: StateFlow<WeatherApiOption> = _weatherApiOption.asStateFlow()

    private val _cameraFeedOption = MutableStateFlow(prefs.cameraFeedOption)
    val cameraFeedOption: StateFlow<CameraFeedOption> = _cameraFeedOption.asStateFlow()

    private val _unitSystem = MutableStateFlow(prefs.unitSystem)
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _appIconStyle = MutableStateFlow(prefs.appIconStyle)
    val appIconStyle: StateFlow<AppIconStyle> = _appIconStyle.asStateFlow()

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

    // Tab 2: Plan a Ride
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

    init {
        refreshAllPlacesWeather()
        recalculateActiveRoute()
        refreshSavedRidesWeather()
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

    fun clearPlaceSearchResults() {
        _placeSearchResults.value = emptyList()
    }

    fun addCyclingPlace(loc: GeocodedLocation) {
        viewModelScope.launch {
            val w = repository.fetchWeatherForLocation(loc.latitude, loc.longitude, 0)
            val newPlace = CyclingPlace(
                id = "place_${UUID.randomUUID().toString().take(8)}",
                name = loc.name,
                region = loc.region,
                latitude = loc.latitude,
                longitude = loc.longitude,
                elevationFeet = if (loc.elevationFeet > 0) loc.elevationFeet else w.elevationFeet,
                tags = listOf("${loc.elevationFeet} ft", "Custom Cycling Spot"),
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

    // Route Planning
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
        }
    }

    fun addWaypointFromMapTap(lat: Double, lon: Double) {
        val current = _plannedWaypoints.value
        if (current.size >= 5) return
        val idx = current.size
        val label = when (idx) {
            0 -> String.format(Locale.US, "Start (%.3f, %.3f)", lat, lon)
            4 -> String.format(Locale.US, "Finish (%.3f, %.3f)", lat, lon)
            else -> String.format(Locale.US, "Point %d (%.3f, %.3f)", idx + 1, lat, lon)
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
        if (current.size >= 5) return
        val idx = current.size
        val next = current + RouteWaypoint(
            index = idx,
            label = loc.name,
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
        _startHourOffset.value = offsetHours
        recalculateActiveRoute()
    }

    fun toggleShowCamerasOnMap() {
        _showCamerasOnMap.value = !_showCamerasOnMap.value
    }

    fun saveCurrentPlannedRide(customName: String) {
        val pts = _plannedWaypoints.value
        if (pts.isEmpty()) return
        val rideName = customName.trim().ifBlank {
            "${pts.first().label.substringBefore(" (")} → ${pts.last().label.substringBefore(" (")}"
        }
        val regionDesc = "${pts.size} Waypoints • ${
            CyclingMath.formatDistance(_plannedDistanceMiles.value, _unitSystem.value)
        }"
        val newRide = SavedRide(
            id = "ride_${UUID.randomUUID().toString().take(8)}",
            name = rideName,
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
            val updatedRides = currentRides.map { ride ->
                val calc = repository.calculateRouteForecast(
                    rawPoints = ride.waypoints,
                    avgSpeedMph = ride.averageSpeedMph,
                    startHourOffset = 0
                )
                ride.copy(
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

    fun getCamerasNear(lat: Double, lon: Double): List<PublicCameraFeed> {
        return repository.getPublicCamerasNear(lat, lon)
    }

    // Settings Mutators
    fun updateMappingOption(option: MappingOption) {
        prefs.mappingOption = option
        _mappingOption.value = option
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

    fun updateUnitSystem(units: UnitSystem) {
        prefs.unitSystem = units
        _unitSystem.value = units
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        prefs.isDarkMode = next
        _isDarkMode.value = next
    }

    fun updateAppIconStyle(style: AppIconStyle) {
        prefs.appIconStyle = style
        _appIconStyle.value = style
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
