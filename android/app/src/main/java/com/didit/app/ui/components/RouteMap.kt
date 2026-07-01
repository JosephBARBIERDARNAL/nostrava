package com.didit.app.ui.components

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.didit.app.model.TrackPoint
import com.didit.app.ui.theme.DiditColors
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Mirrors src/components/RouteMap.tsx (Leaflet + OSM tiles) using osmdroid. */
@Composable
fun RouteMap(
    points: List<TrackPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    live: Boolean = false,
    emptyLabel: String = "No track recorded",
) {
    val active = remember(points) { points.filter { !it.paused } }

    if (active.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .background(DiditColors.Muted.copy(alpha = 0.2f))
                .border(1.dp, DiditColors.Border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emptyLabel, fontSize = 12.sp, color = DiditColors.MutedForeground)
        }
        return
    }

    val context = LocalContext.current
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DiditColors.Border, RoundedCornerShape(8.dp)),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                setBuiltInZoomControls(false)
                isClickable = false
            }
        },
        update = { map ->
            map.overlays.clear()

            val geoPoints = active.map { GeoPoint(it.lat, it.lng) }
            val polyline = Polyline(map).apply {
                setPoints(geoPoints)
                outlinePaint.color = DiditColors.Brand.toArgb()
                outlinePaint.strokeWidth = 8f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
            }
            map.overlays.add(polyline)

            val startIcon = circleDrawable(
                context,
                fillColor = android.graphics.Color.WHITE,
                strokeColor = DiditColors.Brand.toArgb(),
                strokeWidthDp = 3f,
                diameterDp = 14f,
            )
            val endIcon = circleDrawable(
                context,
                fillColor = DiditColors.Accent.toArgb(),
                strokeColor = android.graphics.Color.BLACK,
                strokeWidthDp = 1.5f,
                diameterDp = if (live) 20f else 16f,
            )
            map.overlays.add(
                Marker(map).apply {
                    position = geoPoints.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = startIcon
                    setInfoWindow(null)
                },
            )
            map.overlays.add(
                Marker(map).apply {
                    position = geoPoints.last()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = endIcon
                    setInfoWindow(null)
                },
            )

            val lats = geoPoints.map { it.latitude }
            val lngs = geoPoints.map { it.longitude }
            val bbox = BoundingBox(lats.max(), lngs.max(), lats.min(), lngs.min())
            val maxZoom = if (live) 17.0 else 16.0
            map.post {
                map.zoomToBoundingBox(bbox, true)
                if (map.zoomLevelDouble > maxZoom) map.controller.setZoom(maxZoom)
            }
            map.invalidate()
        },
    )
}

/** A filled circle with a contrasting stroke ring — osmdroid has no built-in circle marker. */
private fun circleDrawable(
    context: Context,
    fillColor: Int,
    strokeColor: Int,
    strokeWidthDp: Float,
    diameterDp: Float,
): Drawable {
    val density = context.resources.displayMetrics.density
    val diameterPx = (diameterDp * density).toInt()
    val strokeWidthPx = (strokeWidthDp * density).toInt()

    val stroke = ShapeDrawable(OvalShape()).apply {
        intrinsicWidth = diameterPx
        intrinsicHeight = diameterPx
        paint.color = strokeColor
        paint.style = Paint.Style.FILL
    }
    val fill = ShapeDrawable(OvalShape()).apply {
        intrinsicWidth = diameterPx
        intrinsicHeight = diameterPx
        paint.color = fillColor
        paint.style = Paint.Style.FILL
    }
    return LayerDrawable(arrayOf(stroke, fill)).apply {
        setLayerInset(1, strokeWidthPx, strokeWidthPx, strokeWidthPx, strokeWidthPx)
        setBounds(0, 0, diameterPx, diameterPx)
    }
}
