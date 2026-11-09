package com.medusko.velocast.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
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
import com.medusko.velocast.ui.theme.AccentBlue
import com.medusko.velocast.ui.theme.CrosswindAmber
import com.medusko.velocast.ui.theme.HeadwindRed
import com.medusko.velocast.ui.theme.MeduskoGold
import com.medusko.velocast.ui.theme.TailwindGreen
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun VelocastMapView(
    mappingOption: MappingOption,
    mapStyleOption: MapStyleOption,
    waypoints: List<RouteWaypoint>,
    routeGeometry: List<Pair<Double, Double>>,
    cameras: List<PublicCameraFeed>,
    showCamerasOnMap: Boolean,
    units: UnitPreferences,
    onMapStyleSelected: (MapStyleOption) -> Unit,
    onMapTapAddWaypoint: (Double, Double) -> Unit,
    onCameraMarkerSelected: (PublicCameraFeed) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onMapTapAddWaypoint)
    val currentOnCam by rememberUpdatedState(onCameraMarkerSelected)

    var selectedWaypointPopup by remember { mutableStateOf<RouteWaypoint?>(null) }

    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue = "VelocastCyclingWeather/1.0 (https://medusko.com)"
        onDispose { }
    }

    // Expanded vertically by 25% (310.dp -> 390.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(390.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(resolveTileSource(mappingOption, mapStyleOption))
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
                    val desiredSource = resolveTileSource(mappingOption, mapStyleOption)
                    if (mapView.tileProvider.tileSource.name() != desiredSource.name()) {
                        mapView.setTileSource(desiredSource)
                    }

                    mapView.overlays.clear()

                    // 1. Tap listener overlay
                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                selectedWaypointPopup = null
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
                        val casing = Polyline(mapView).apply {
                            setPoints(polyPoints)
                            outlinePaint.color = AndroidColor.parseColor("#0E1525")
                            outlinePaint.strokeWidth = 17f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.isAntiAlias = true
                        }
                        val line = Polyline(mapView).apply {
                            setPoints(polyPoints)
                            outlinePaint.color = AndroidColor.parseColor("#E89D24")
                            outlinePaint.strokeWidth = 10f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.isAntiAlias = true
                        }
                        mapView.overlays.add(casing)
                        mapView.overlays.add(line)
                    }

                    // 3. Camera Markers (Custom Camera Icon Drawable)
                    if (showCamerasOnMap) {
                        val camIcon = createCameraMarkerDrawable(context)
                        cameras.forEach { cam ->
                            val camMarker = Marker(mapView).apply {
                                position = GeoPoint(cam.latitude, cam.longitude)
                                icon = camIcon
                                infoWindow = null
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                setOnMarkerClickListener { _, _ ->
                                    currentOnCam(cam)
                                    true
                                }
                            }
                            mapView.overlays.add(camMarker)
                        }
                    }

                    // 4. Waypoint Markers (Custom Bike Icon Drawable + Modern Solid Popup)
                    waypoints.forEachIndexed { idx, wp ->
                        val bikeDrawable = createBikeWaypointMarkerDrawable(
                            context = context,
                            pointNumber = idx + 1,
                            impact = wp.windImpact
                        )
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(wp.latitude, wp.longitude)
                            icon = bikeDrawable
                            infoWindow = null // Disable legacy gray gradient bubble
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setOnMarkerClickListener { _, _ ->
                                selectedWaypointPopup = wp
                                true
                            }
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

            // Top-Left: Engine & Point Count HUD
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xEC0E131D),
                border = BorderStroke(1.dp, MeduskoGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = MeduskoGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${waypoints.size}/20 pts",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top-Right: Map Type Selector (Regular, Street, Topograph)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xEC0E131D),
                border = BorderStroke(1.dp, Color(0xFF2D3952))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Type",
                        tint = MeduskoGold,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 2.dp)
                            .size(13.dp)
                    )
                    MapStyleOption.entries.forEach { style ->
                        val selected = mapStyleOption == style
                        Surface(
                            modifier = Modifier.clickable { onMapStyleSelected(style) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) MeduskoGold else Color.Transparent
                        ) {
                            Text(
                                text = style.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color(0xFF0E131D) else Color(0xFFD6DCE8),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Modern Solid-Color Popup Card when a Waypoint Marker is tapped (replaces legacy gray gradient)
            AnimatedVisibility(
                visible = selectedWaypointPopup != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                selectedWaypointPopup?.let { wp ->
                    val w = wp.weather
                    val impactCol = when (wp.windImpact) {
                        WindImpact.TAILWIND -> TailwindGreen
                        WindImpact.HEADWIND -> HeadwindRed
                        WindImpact.CROSSWIND -> CrosswindAmber
                        WindImpact.CALM -> AccentBlue
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF151C28),
                        border = BorderStroke(1.5.dp, impactCol)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = impactCol
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                            contentDescription = null,
                                            tint = Color(0xFF0E131D),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Pt ${wp.index + 1}: ${wp.label}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    val weatherLine = if (w != null) {
                                        "${CyclingMath.formatTemp(w.temperatureF, units)} • Wind ${CyclingMath.formatSpeed(w.windSpeedMph, units)} ${w.windCardinal} (${wp.windImpact.label})"
                                    } else {
                                        "ETA ${wp.etaLabel} • ${CyclingMath.formatDistance(wp.distanceFromStartMiles, units)}"
                                    }
                                    Text(
                                        text = weatherLine,
                                        color = impactCol,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = { selectedWaypointPopup = null },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Popup",
                                    tint = Color(0xFF9CA6BA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Hint Pill when no popup is open
            if (selectedWaypointPopup == null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xE6151C28),
                    border = BorderStroke(1.dp, Color(0xFF2D3952))
                ) {
                    Text(
                        text = if (waypoints.size < 20) {
                            "Tap map to add bike point (${20 - waypoints.size} remaining) • Tap bike/camera icon for details"
                        } else {
                            "Max 20 route points active"
                        },
                        color = Color(0xFFD6DCE8),
                        fontSize = 9.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun resolveTileSource(
    mappingOption: MappingOption,
    styleOption: MapStyleOption
): ITileSource {
    return when (mappingOption) {
        MappingOption.OPEN_FREE_MAP -> {
            when (styleOption) {
                MapStyleOption.REGULAR -> XYTileSource(
                    "OpenFreeMap_Regular",
                    0, 19, 256, ".png",
                    arrayOf(
                        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
                    )
                )
                MapStyleOption.STREET -> XYTileSource(
                    "OpenFreeMap_Street",
                    0, 19, 256, ".png",
                    arrayOf(
                        "https://a.tile.openstreetmap.fr/hot/",
                        "https://b.tile.openstreetmap.fr/hot/"
                    )
                )
                MapStyleOption.TOPOGRAPHIC -> object : OnlineTileSourceBase(
                    "OpenFreeMap_Topo",
                    0, 18, 256, "",
                    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/")
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        val z = MapTileIndex.getZoom(pMapTileIndex)
                        val x = MapTileIndex.getX(pMapTileIndex)
                        val y = MapTileIndex.getY(pMapTileIndex)
                        return "$baseUrl$z/$y/$x"
                    }
                }
            }
        }
        MappingOption.OSMDROID -> {
            when (styleOption) {
                MapStyleOption.REGULAR -> TileSourceFactory.MAPNIK
                MapStyleOption.STREET -> XYTileSource(
                    "CyclOSM_Street",
                    0, 19, 256, ".png",
                    arrayOf(
                        "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
                        "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
                        "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
                    )
                )
                MapStyleOption.TOPOGRAPHIC -> TileSourceFactory.OpenTopo
            }
        }
    }
}

/**
 * Renders a crisp custom Bicycle icon marker with waypoint number badge.
 */
private fun createBikeWaypointMarkerDrawable(
    context: Context,
    pointNumber: Int,
    impact: WindImpact
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (38f * density).toInt().coerceAtLeast(48)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val strokeColor = when (impact) {
        WindImpact.TAILWIND -> AndroidColor.parseColor("#00C853")
        WindImpact.HEADWIND -> AndroidColor.parseColor("#FF3D57")
        WindImpact.CROSSWIND -> AndroidColor.parseColor("#FFAB00")
        WindImpact.CALM -> AndroidColor.parseColor("#E89D24")
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0E1525")
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }

    val cx = sizePx * 0.5f
    val cy = sizePx * 0.5f
    val r = sizePx * 0.42f
    canvas.drawCircle(cx, cy, r, bgPaint)
    canvas.drawCircle(cx, cy, r, borderPaint)

    // Draw miniature Bicycle inside circle
    val bikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#E89D24")
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val wy = cy + sizePx * 0.08f
    val rwX = cx - sizePx * 0.14f
    val fwX = cx + sizePx * 0.14f
    val wr = sizePx * 0.09f
    canvas.drawCircle(rwX, wy, wr, bikePaint)
    canvas.drawCircle(fwX, wy, wr, bikePaint)

    val frame = Path().apply {
        moveTo(rwX, wy)
        lineTo(cx - sizePx * 0.02f, wy)
        lineTo(cx + sizePx * 0.08f, wy - sizePx * 0.13f)
        lineTo(cx - sizePx * 0.06f, wy - sizePx * 0.13f)
        close()
        moveTo(cx + sizePx * 0.08f, wy - sizePx * 0.15f)
        lineTo(fwX, wy)
    }
    canvas.drawPath(frame, bikePaint)

    // Number badge at top-right corner
    val badgeR = sizePx * 0.20f
    val bx = sizePx - badgeR - 1f * density
    val by = badgeR + 1f * density
    val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(bx, by, badgeR, badgeFill)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0E1525")
        textSize = 9.5f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textY = by - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(pointNumber.toString(), bx, textY, textPaint)

    return BitmapDrawable(context.resources, bmp)
}

/**
 * Renders a crisp custom Camera icon marker for roadside cameras on the map.
 */
private fun createCameraMarkerDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (32f * density).toInt().coerceAtLeast(40)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val rect = RectF(2f * density, 4f * density, sizePx - 2f * density, sizePx - 4f * density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#122238")
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawRoundRect(rect, 7f * density, 7f * density, bgPaint)
    canvas.drawRoundRect(rect, 7f * density, 7f * density, borderPaint)

    // Draw camera body & lens
    val camPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
    }
    val bodyRect = RectF(sizePx * 0.22f, sizePx * 0.34f, sizePx * 0.66f, sizePx * 0.66f)
    canvas.drawRoundRect(bodyRect, 2.5f * density, 2.5f * density, camPaint)
    val lensPath = Path().apply {
        moveTo(sizePx * 0.66f, sizePx * 0.43f)
        lineTo(sizePx * 0.80f, sizePx * 0.35f)
        lineTo(sizePx * 0.80f, sizePx * 0.65f)
        lineTo(sizePx * 0.66f, sizePx * 0.57f)
        close()
    }
    canvas.drawPath(lensPath, camPaint)

    return BitmapDrawable(context.resources, bmp)
}
