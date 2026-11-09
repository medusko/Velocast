package com.medusko.velocast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SunriseVectorIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val isDark = isSystemInDarkTheme()
    val sunColor = if (isDark) Color(0xFFF5B041) else Color(0xFFD68910)
    val horizonColor = if (isDark) Color(0xFF9CA6BA) else Color(0xFF5D6D7E)
    val arrowColor = if (isDark) Color(0xFFFFD54F) else Color(0xFFB7950B)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.095f

        // Horizon baseline
        drawLine(
            color = horizonColor,
            start = Offset(w * 0.10f, h * 0.74f),
            end = Offset(w * 0.90f, h * 0.74f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Sub-horizon reflection line
        drawLine(
            color = horizonColor.copy(alpha = 0.6f),
            start = Offset(w * 0.26f, h * 0.88f),
            end = Offset(w * 0.74f, h * 0.88f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )

        // Semi-circular rising sun disk
        val arcRadius = w * 0.24f
        drawArc(
            color = sunColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.5f - arcRadius, h * 0.74f - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f)
        )

        // Sun rays
        val rayAngles = listOf(205.0, 235.0, 305.0, 335.0)
        for (deg in rayAngles) {
            val rad = Math.toRadians(deg)
            val r1 = arcRadius + w * 0.06f
            val r2 = arcRadius + w * 0.15f
            val cx = w * 0.5f
            val cy = h * 0.74f
            drawLine(
                color = sunColor,
                start = Offset((cx + cos(rad) * r1).toFloat(), (cy + sin(rad) * r1).toFloat()),
                end = Offset((cx + cos(rad) * r2).toFloat(), (cy + sin(rad) * r2).toFloat()),
                strokeWidth = stroke * 0.85f,
                cap = StrokeCap.Round
            )
        }

        // Upward arrow above sun
        val arrowPath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.37f, h * 0.26f)
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.63f, h * 0.26f)
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.5f, h * 0.38f)
        }
        drawPath(
            path = arrowPath,
            color = arrowColor,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun SunsetVectorIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val isDark = isSystemInDarkTheme()
    val sunColor = if (isDark) Color(0xFFFF7043) else Color(0xFFD35400)
    val horizonColor = if (isDark) Color(0xFF9CA6BA) else Color(0xFF5D6D7E)
    val arrowColor = if (isDark) Color(0xFFFFAB91) else Color(0xFFA04000)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.095f

        // Horizon baseline
        drawLine(
            color = horizonColor,
            start = Offset(w * 0.10f, h * 0.74f),
            end = Offset(w * 0.90f, h * 0.74f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = horizonColor.copy(alpha = 0.6f),
            start = Offset(w * 0.26f, h * 0.88f),
            end = Offset(w * 0.74f, h * 0.88f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )

        // Setting sun disk
        val arcRadius = w * 0.24f
        drawArc(
            color = sunColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.5f - arcRadius, h * 0.74f - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f)
        )

        val rayAngles = listOf(205.0, 235.0, 305.0, 335.0)
        for (deg in rayAngles) {
            val rad = Math.toRadians(deg)
            val r1 = arcRadius + w * 0.06f
            val r2 = arcRadius + w * 0.15f
            val cx = w * 0.5f
            val cy = h * 0.74f
            drawLine(
                color = sunColor,
                start = Offset((cx + cos(rad) * r1).toFloat(), (cy + sin(rad) * r1).toFloat()),
                end = Offset((cx + cos(rad) * r2).toFloat(), (cy + sin(rad) * r2).toFloat()),
                strokeWidth = stroke * 0.85f,
                cap = StrokeCap.Round
            )
        }

        // Downward arrow above sun
        val arrowPath = Path().apply {
            moveTo(w * 0.5f, h * 0.38f)
            lineTo(w * 0.37f, h * 0.24f)
            moveTo(w * 0.5f, h * 0.38f)
            lineTo(w * 0.63f, h * 0.24f)
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.5f, h * 0.38f)
        }
        drawPath(
            path = arrowPath,
            color = arrowColor,
            style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun StatusColorDot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        drawCircle(color = color)
    }
}
