package com.medusko.velocast.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.medusko.velocast.model.*
import com.medusko.velocast.ui.theme.AccentBlue
import com.medusko.velocast.ui.theme.MeduskoGold
import com.medusko.velocast.ui.theme.TailwindGreen
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CameraFeedDialog(
    placeTitle: String,
    cameras: List<PublicCameraFeed>,
    activeSourceOption: CameraFeedOption,
    weather: WeatherSnapshot?,
    units: UnitPreferences,
    initialSelectedCamera: PublicCameraFeed? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var selectedCamera by remember(cameras, initialSelectedCamera) {
        mutableStateOf(initialSelectedCamera ?: cameras.firstOrNull())
    }
    var refreshNonce by remember { mutableIntStateOf(0) }
    var playInlineVideo by remember { mutableStateOf(false) }
    val timeFmt = remember { SimpleDateFormat("h:mm:ss a", Locale.US) }
    val timestampStr = remember(refreshNonce) { timeFmt.format(Date()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
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
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MeduskoGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Public Roadside Cameras",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$placeTitle • ${activeSourceOption.title}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                selectedCamera?.let { cam ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0A0F18),
                        border = BorderStroke(1.dp, MeduskoGold.copy(alpha = 0.65f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (playInlineVideo && cam.streamPageUrl.isNotBlank()) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.mediaPlaybackRequiresUserGesture = false
                                            webChromeClient = WebChromeClient()
                                            webViewClient = WebViewClient()
                                            loadUrl(cam.streamPageUrl)
                                        }
                                    },
                                    update = { view ->
                                        if (view.url != cam.streamPageUrl) {
                                            view.loadUrl(cam.streamPageUrl)
                                        }
                                    }
                                )
                            } else {
                                val imageReq = remember(cam.imageUrl, refreshNonce) {
                                    ImageRequest.Builder(context)
                                        .data(cam.imageUrl)
                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 15) Velocast/1.0")
                                        .memoryCacheKey("${cam.imageUrl}_$refreshNonce")
                                        .diskCachePolicy(if (refreshNonce > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
                                        .crossfade(true)
                                        .build()
                                }
                                SubcomposeAsyncImage(
                                    model = imageReq,
                                    contentDescription = cam.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        RoadsideTelemetrySimulatedCanvas(cam, weather, units)
                                    },
                                    error = {
                                        RoadsideTelemetrySimulatedCanvas(cam, weather, units)
                                    }
                                )
                            }

                            // Top Telemetry Overlay Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xB8080D16))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .align(Alignment.TopCenter),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(TailwindGreen)
                                    )
                                    Text(
                                        text = if (playInlineVideo) "LIVE STREAM PLAYER" else "LIVE CAMERA FEED",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Updated $timestampStr",
                                    color = Color(0xFFB8C4D9),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Bottom Camera Metadata Overlay Bar
                            if (!playInlineVideo) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xCC080D16))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .align(Alignment.BottomCenter)
                                ) {
                                    Text(
                                        text = cam.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${cam.directionLabel} • Elev ${CyclingMath.formatElevation(cam.elevationFeet, units)}",
                                            color = MeduskoGold,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = CyclingMath.formatDistance(cam.distanceMiles, units) + " away",
                                            color = AccentBlue,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Row: Refresh Frame, Inline Stream Toggle, Open Unique Web Stream URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                playInlineVideo = false
                                refreshNonce++
                            },
                            modifier = Modifier.weight(0.95f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { playInlineVideo = !playInlineVideo },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (playInlineVideo) "Show Photo" else "Live Player",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                        }

                        Button(
                            onClick = {
                                val targetUrl = cam.streamPageUrl.ifBlank { cam.imageUrl }
                                uriHandler.openUri(targetUrl)
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MeduskoGold,
                                contentColor = Color(0xFF0E131D)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Direct Stream", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NEARBY PUBLIC CAMERAS (${cameras.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cameras, key = { it.id }) { cam ->
                        val isSelected = selectedCamera?.id == cam.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCamera = cam
                                    playInlineVideo = false
                                },
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cam.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${cam.locationSubtitle} • ${cam.roadConditionNote}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = cam.streamPageUrl,
                                        fontSize = 9.5.sp,
                                        color = AccentBlue,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CyclingMath.formatDistance(cam.distanceMiles, units),
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = CyclingMath.formatElevation(cam.elevationFeet, units),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun RoadsideTelemetrySimulatedCanvas(
    cam: PublicCameraFeed,
    weather: WeatherSnapshot?,
    units: UnitPreferences
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E1B33), Color(0xFF1E375A), Color(0xFF2B4560)),
                    startY = 0f,
                    endY = h * 0.62f
                ),
                size = size
            )

            val ridge = Path().apply {
                moveTo(0f, h * 0.62f)
                lineTo(w * 0.22f, h * 0.44f)
                lineTo(w * 0.48f, h * 0.53f)
                lineTo(w * 0.76f, h * 0.39f)
                lineTo(w, h * 0.52f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(ridge, color = Color(0xFF141E2E))

            val road = Path().apply {
                moveTo(w * 0.44f, h * 0.52f)
                lineTo(w * 0.56f, h * 0.52f)
                lineTo(w * 0.88f, h)
                lineTo(w * 0.12f, h)
                close()
            }
            drawPath(road, color = Color(0xFF1D2430))

            drawLine(
                color = Color(0xFFE89D24),
                start = Offset(w * 0.5f, h * 0.54f),
                end = Offset(w * 0.5f, h * 0.92f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(w * 0.55f, h * 0.54f),
                end = Offset(w * 0.80f, h),
                strokeWidth = 2.5f
            )

            drawCircle(
                color = Color(0x55E89D24),
                radius = 34f,
                center = Offset(w * 0.5f, h * 0.48f),
                style = Stroke(width = 1.5f)
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xCC0E131D),
            border = BorderStroke(1.dp, MeduskoGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val tempStr = weather?.let { CyclingMath.formatTemp(it.temperatureF, units) } ?: "64°F"
                val windStr = weather?.let { "${CyclingMath.formatSpeed(it.windSpeedMph, units)} ${it.windCardinal}" } ?: "12 mph WNW"
                val rainStr = weather?.let { "${it.rainChancePercent}% Rain" } ?: "10% Rain"
                Text(
                    text = "LOADING LIVE CAMERA SNAPSHOT...",
                    color = MeduskoGold,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp
                )
                Text(
                    text = "$tempStr  •  Wind $windStr  •  $rainStr",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = cam.roadConditionNote,
                    color = Color(0xFF9CA6BA),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}
