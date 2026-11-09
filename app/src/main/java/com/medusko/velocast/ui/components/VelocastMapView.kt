package com.medusko.velocast.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.medusko.velocast.model.*
import com.medusko.velocast.ui.theme.MeduskoGold
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun VelocastMapView(
    mappingOption: MappingOption,
    waypoints: List<RouteWaypoint>,
    routeGeometry: List<Pair<Double, Double>>,
    cameras: List<PublicCameraFeed>,
    showCamerasOnMap: Boolean,
    units: UnitSystem,
    onMapTapAddWaypoint: (Double, Double) -> Unit,
    onCameraMarkerSelected: (PublicCameraFeed) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (mappingOption) {
                MappingOption.OPEN_FREE_MAP -> {
                    OpenFreeMapVectorCanvas(
                        waypoints = waypoints,
                        routeGeometry = routeGeometry,
                        cameras = if (showCamerasOnMap) cameras else emptyList(),
                        units = units,
                        onMapTapAddWaypoint = onMapTapAddWaypoint,
                        onCameraSelected = onCameraMarkerSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MappingOption.OSMDROID -> {
                    OsmDroidRasterMapCanvas(
                        waypoints = waypoints,
                        routeGeometry = routeGeometry,
                        cameras = if (showCamerasOnMap) cameras else emptyList(),
                        units = units,
                        onMapTapAddWaypoint = onMapTapAddWaypoint,
                        onCameraSelected = onCameraMarkerSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Engine & Attribution HUD Pill (Top Left)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xD90E131D),
                border = BorderStroke(1.dp, MeduskoGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (mappingOption == MappingOption.OPEN_FREE_MAP) {
                            "🗺️ OpenFreeMap Vector"
                        } else {
                            "🗺️ OSMDroid Raster"
                        },
                        color = MeduskoGold,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${waypoints.size}/5 pts",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tap Hint Pill (Bottom Left)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xCC0E131D)
            ) {
                Text(
                    text = if (waypoints.size < 5) {
                        "Tap map to add waypoint (${5 - waypoints.size} remaining) • © OpenStreetMap"
                    } else {
                        "Max 5 route points active • © OpenStreetMap contributors"
                    },
                    color = Color(0xFFD6DCE8),
                    fontSize = 9.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private class MapJsBridge(
    private val onTap: (Double, Double) -> Unit,
    private val onCamTap: (String) -> Unit
) {
    @JavascriptInterface
    fun onMapClicked(lat: Double, lon: Double) {
        onTap(lat, lon)
    }

    @JavascriptInterface
    fun onCameraClicked(camId: String) {
        onCamTap(camId)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OpenFreeMapVectorCanvas(
    waypoints: List<RouteWaypoint>,
    routeGeometry: List<Pair<Double, Double>>,
    cameras: List<PublicCameraFeed>,
    units: UnitSystem,
    onMapTapAddWaypoint: (Double, Double) -> Unit,
    onCameraSelected: (PublicCameraFeed) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnTap by rememberUpdatedState(onMapTapAddWaypoint)
    val currentOnCam by rememberUpdatedState(onCameraSelected)
    val currentCams by rememberUpdatedState(cameras)

    val stateJson = remember(waypoints, routeGeometry, cameras, units) {
        buildMapStateJson(waypoints, routeGeometry, cameras, units)
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("window.updateVelocastMap && window.updateVelocastMap($stateJson);", null)
                    }
                }
                addJavascriptInterface(
                    MapJsBridge(
                        onTap = { lat, lon ->
                            post { currentOnTap(lat, lon) }
                        },
                        onCamTap = { camId ->
                            post {
                                currentCams.firstOrNull { it.id == camId }?.let { currentOnCam(it) }
                            }
                        }
                    ),
                    "VelocastBridge"
                )
                // Prevent parent scroll view from intercepting map drags
                setOnTouchListener { v, event ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                loadDataWithBaseURL(
                    "https://tiles.openfreemap.org/",
                    OPEN_FREE_MAP_HTML,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            webView.evaluateJavascript("window.updateVelocastMap && window.updateVelocastMap($stateJson);", null)
        }
    )
}

@Composable
private fun OsmDroidRasterMapCanvas(
    waypoints: List<RouteWaypoint>,
    routeGeometry: List<Pair<Double, Double>>,
    cameras: List<PublicCameraFeed>,
    units: UnitSystem,
    onMapTapAddWaypoint: (Double, Double) -> Unit,
    onCameraSelected: (PublicCameraFeed) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onMapTapAddWaypoint)
    val currentOnCam by rememberUpdatedState(onCameraSelected)

    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue = "VelocastCyclingWeather/1.0 (https://medusko.com)"
        onDispose { }
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(11.5)
                val startLat = waypoints.firstOrNull()?.latitude ?: 37.8267
                val startLon = waypoints.firstOrNull()?.longitude ?: -122.4994
                controller.setCenter(GeoPoint(startLat, startLon))
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // 1. Tap listener overlay
            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        currentOnTap(p.latitude, p.longitude)
                        return true
                    }
                    return false
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            })
            mapView.overlays.add(eventsOverlay)

            // 2. Route Polyline
            val polyPoints = if (routeGeometry.size >= 2) {
                routeGeometry.map { GeoPoint(it.first, it.second) }
            } else {
                waypoints.map { GeoPoint(it.latitude, it.longitude) }
            }
            if (polyPoints.size >= 2) {
                val line = Polyline(mapView).apply {
                    setPoints(polyPoints)
                    outlinePaint.color = AndroidColor.parseColor("#E89D24")
                    outlinePaint.strokeWidth = 11f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.isAntiAlias = true
                }
                mapView.overlays.add(line)
            }

            // 3. Camera Markers
            cameras.forEach { cam ->
                val camMarker = Marker(mapView).apply {
                    position = GeoPoint(cam.latitude, cam.longitude)
                    title = "📷 ${cam.title}"
                    snippet = cam.roadConditionNote
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        currentOnCam(cam)
                        true
                    }
                }
                mapView.overlays.add(camMarker)
            }

            // 4. Waypoint Markers
            waypoints.forEachIndexed { idx, wp ->
                val w = wp.weather
                val weatherDesc = if (w != null) {
                    "${CyclingMath.formatTemp(w.temperatureF, units)} • ${CyclingMath.formatSpeed(w.windSpeedMph, units)} ${w.windCardinal} (${wp.windImpact.label})"
                } else {
                    wp.etaLabel
                }
                val marker = Marker(mapView).apply {
                    position = GeoPoint(wp.latitude, wp.longitude)
                    title = "Pt ${idx + 1}: ${wp.label}"
                    snippet = weatherDesc
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
            }

            if (waypoints.isNotEmpty()) {
                val focus = waypoints.last()
                mapView.controller.animateTo(GeoPoint(focus.latitude, focus.longitude))
            }
            mapView.invalidate()
        }
    )
}

private fun buildMapStateJson(
    waypoints: List<RouteWaypoint>,
    routeGeometry: List<Pair<Double, Double>>,
    cameras: List<PublicCameraFeed>,
    units: UnitSystem
): String {
    val root = JSONObject()
    val wpArr = JSONArray()
    waypoints.forEachIndexed { i, wp ->
        val w = wp.weather
        val obj = JSONObject().apply {
            put("index", i + 1)
            put("label", wp.label)
            put("lat", wp.latitude)
            put("lon", wp.longitude)
            put("eta", wp.etaLabel)
            put("impact", wp.windImpact.name)
            put("temp", if (w != null) CyclingMath.formatTemp(w.temperatureF, units) else "--")
            put("wind", if (w != null) "${CyclingMath.formatSpeed(w.windSpeedMph, units)} ${w.windCardinal}" else "--")
            put("windDeg", w?.windDirectionDeg ?: 0)
        }
        wpArr.put(obj)
    }
    root.put("waypoints", wpArr)

    val geomArr = JSONArray()
    routeGeometry.forEach { (lat, lon) ->
        val pt = JSONArray()
        pt.put(lon)
        pt.put(lat)
        geomArr.put(pt)
    }
    root.put("geometry", geomArr)

    val camArr = JSONArray()
    cameras.forEach { cam ->
        camArr.put(
            JSONObject().apply {
                put("id", cam.id)
                put("title", cam.title)
                put("lat", cam.latitude)
                put("lon", cam.longitude)
            }
        )
    }
    root.put("cameras", camArr)
    return root.toString()
}

private const val OPEN_FREE_MAP_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link href="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css" rel="stylesheet" />
<script src="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js"></script>
<style>
  html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #0E1525; font-family: -apple-system, BlinkMacSystemFont, sans-serif; }
  .wp-marker {
    background: #0E1525;
    border: 2px solid #E89D24;
    border-radius: 10px;
    padding: 3px 7px;
    color: #FFFFFF;
    font-size: 11px;
    font-weight: 800;
    box-shadow: 0 3px 10px rgba(0,0,0,0.65);
    white-space: nowrap;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .wp-badge {
    background: #E89D24;
    color: #0E1525;
    border-radius: 50%;
    width: 16px;
    height: 16px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 900;
  }
  .cam-marker {
    background: #132238;
    border: 1.5px solid #38BDF8;
    border-radius: 8px;
    padding: 2px 6px;
    color: #38BDF8;
    font-size: 11px;
    font-weight: 700;
    box-shadow: 0 2px 6px rgba(0,0,0,0.5);
    cursor: pointer;
  }
</style>
</head>
<body>
<div id="map"></div>
<script>
  let map = null;
  let markers = [];
  let pendingData = null;

  function initMap() {
    try {
      map = new maplibregl.Map({
        container: 'map',
        style: 'https://tiles.openfreemap.org/styles/liberty',
        center: [-122.485, 37.825],
        zoom: 11.2,
        attributionControl: false
      });

      map.on('load', function() {
        map.addSource('route', {
          type: 'geojson',
          data: { type: 'Feature', geometry: { type: 'LineString', coordinates: [] } }
        });
        map.addLayer({
          id: 'route-casing',
          type: 'line',
          source: 'route',
          layout: { 'line-join': 'round', 'line-cap': 'round' },
          paint: { 'line-color': '#0E1525', 'line-width': 8 }
        });
        map.addLayer({
          id: 'route-line',
          type: 'line',
          source: 'route',
          layout: { 'line-join': 'round', 'line-cap': 'round' },
          paint: { 'line-color': '#E89D24', 'line-width': 4.5 }
        });
        if (pendingData) {
          renderState(pendingData);
        }
      });

      map.on('click', function(e) {
        if (window.VelocastBridge && window.VelocastBridge.onMapClicked) {
          window.VelocastBridge.onMapClicked(e.lngLat.lat, e.lngLat.lng);
        }
      });
    } catch (err) {
      console.error(err);
    }
  }

  function renderState(data) {
    markers.forEach(m => m.remove());
    markers = [];

    if (map && map.getSource('route')) {
      const coords = (data.geometry && data.geometry.length >= 2)
        ? data.geometry
        : (data.waypoints || []).map(w => [w.lon, w.lat]);
      map.getSource('route').setData({
        type: 'Feature',
        geometry: { type: 'LineString', coordinates: coords }
      });
    }

    (data.cameras || []).forEach(cam => {
      const el = document.createElement('div');
      el.className = 'cam-marker';
      el.innerHTML = '📷 Live Cam';
      el.onclick = function(ev) {
        ev.stopPropagation();
        if (window.VelocastBridge) window.VelocastBridge.onCameraClicked(cam.id);
      };
      const m = new maplibregl.Marker({ element: el }).setLngLat([cam.lon, cam.lat]).addTo(map);
      markers.push(m);
    });

    (data.waypoints || []).forEach(wp => {
      const el = document.createElement('div');
      el.className = 'wp-marker';
      const borderCol = wp.impact === 'HEADWIND' ? '#FF3D57' : (wp.impact === 'TAILWIND' ? '#00C853' : '#E89D24');
      el.style.borderColor = borderCol;
      const arrowDeg = ((wp.windDeg || 0) + 180) % 360;
      el.innerHTML = '<span class="wp-badge">' + wp.index + '</span>' +
                     '<span>' + wp.temp + ' • ' + wp.wind + '</span>' +
                     '<span style="display:inline-block;transform:rotate(' + arrowDeg + 'deg)">↑</span>';
      const m = new maplibregl.Marker({ element: el }).setLngLat([wp.lon, wp.lat]).addTo(map);
      markers.push(m);
    });

    if (data.waypoints && data.waypoints.length > 0 && map) {
      const last = data.waypoints[data.waypoints.length - 1];
      map.easeTo({ center: [last.lon, last.lat], duration: 500 });
    }
  }

  window.updateVelocastMap = function(data) {
    pendingData = data;
    if (map && map.isStyleLoaded()) {
      renderState(data);
    }
  };

  initMap();
</script>
</body>
</html>
"""
