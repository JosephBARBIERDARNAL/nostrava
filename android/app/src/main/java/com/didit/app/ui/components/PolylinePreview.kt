package com.didit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.didit.app.model.TrackPoint
import com.didit.app.ui.theme.DiditColors
import kotlin.math.PI
import kotlin.math.cos

/**
 * Mirrors src/components/PolylinePreview.tsx — a lightweight, tile-free SVG
 * preview of a GPS track. Not currently wired into any screen, matching the
 * TS component's unused-but-available status.
 */
@Composable
fun PolylinePreview(
    points: List<TrackPoint>,
    modifier: Modifier = Modifier,
    width: Dp = 360.dp,
    height: Dp = 200.dp,
) {
    val active = remember(points) { points.filter { !it.paused } }

    if (active.size < 2) {
        Box(modifier = modifier.width(width).height(height), contentAlignment = Alignment.Center) {
            Text("No track recorded", fontSize = 12.sp, color = DiditColors.MutedForeground)
        }
        return
    }

    Canvas(modifier = modifier.width(width).height(height)) {
        val lats = active.map { it.lat }
        val lngs = active.map { it.lng }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLng = lngs.min()
        val maxLng = lngs.max()

        val meanLat = (minLat + maxLat) / 2
        val cosLat = cos(meanLat * PI / 180)
        val xScaleRaw = ((maxLng - minLng) * cosLat).coerceAtLeast(1e-9)
        val yScaleRaw = (maxLat - minLat).coerceAtLeast(1e-9)

        val pad = 12.dp.toPx()
        val availW = size.width - pad * 2
        val availH = size.height - pad * 2
        val scale = minOf(availW / xScaleRaw, availH / yScaleRaw)
        val projW = xScaleRaw * scale
        val projH = yScaleRaw * scale
        val offX = pad + (availW - projW) / 2
        val offY = pad + (availH - projH) / 2

        fun project(lat: Double, lng: Double): Offset {
            val x = offX + (lng - minLng) * cosLat * scale
            val y = offY + (maxLat - lat) * scale
            return Offset(x.toFloat(), y.toFloat())
        }

        val cornerRadius = CornerRadius(12.dp.toPx())
        drawRoundRect(color = DiditColors.Card, cornerRadius = cornerRadius, style = Fill)
        drawRoundRect(color = DiditColors.Border, cornerRadius = cornerRadius, style = Stroke(width = 1.dp.toPx()))

        val path = Path()
        active.forEachIndexed { i, p ->
            val o = project(p.lat, p.lng)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(
            path,
            color = DiditColors.Brand,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val start = project(active.first().lat, active.first().lng)
        val end = project(active.last().lat, active.last().lng)
        drawCircle(color = DiditColors.Brand, radius = 5.dp.toPx(), center = start)
        drawCircle(color = DiditColors.Accent, radius = 5.dp.toPx(), center = end)
        drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = end, style = Stroke(width = 1.dp.toPx()))
    }
}
