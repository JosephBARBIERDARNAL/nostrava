package com.didit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.didit.app.model.TrackPoint
import com.didit.app.ui.theme.DiditColors

/** Mirrors src/components/ElevationChart.tsx (SVG polyline) using Compose Canvas. */
@Composable
fun ElevationChart(points: List<TrackPoint>, modifier: Modifier = Modifier, height: Dp = 80.dp) {
    val alts = remember(points) {
        points.filter { !it.paused && it.altitudeM != null }.map { it.altitudeM!! }
    }

    if (alts.size < 3) {
        Box(
            modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No elevation data", fontSize = 12.sp, color = DiditColors.MutedForeground)
        }
        return
    }

    val min = alts.min()
    val max = alts.max()
    val range = (max - min).coerceAtLeast(1.0)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val path = Path()
        alts.forEachIndexed { i, a ->
            val x = (i.toFloat() / (alts.size - 1)) * w
            val y = h - ((a - min) / range).toFloat() * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = DiditColors.Brand, style = Stroke(width = 2.dp.toPx()))
    }
}
