package com.didit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.didit.app.bridge.NativeBridge
import com.didit.app.model.LiveMetrics
import com.didit.app.model.TrackPoint
import com.didit.app.nav.replace
import com.didit.app.ui.components.DiditButton
import com.didit.app.ui.components.DiditButtonSize
import com.didit.app.ui.components.DiditButtonVariant
import com.didit.app.ui.components.RouteMap
import com.didit.app.ui.components.StatBox
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.ACTIVITY_CONFIG
import com.didit.app.util.Metric
import com.didit.app.util.formatDistance
import com.didit.app.util.formatDuration
import com.didit.app.util.formatPace
import com.didit.app.util.formatSpeed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ActivityTrackScreen(nav: NavHostController, activity: String) {
    val config = ACTIVITY_CONFIG.getValue(activity)
    val scope = rememberCoroutineScope()

    var metrics by remember { mutableStateOf<LiveMetrics?>(null) }
    var points by remember { mutableStateOf<List<TrackPoint>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    // If there's no active session (e.g. process restarted with nothing running), bail home.
    LaunchedEffect(activity) {
        if (NativeBridge.currentState() == "idle") nav.replace("$activity/home")
    }

    // Poll at ~1Hz while this screen is on-screen — replaces the old Tauri "metrics" event push.
    LaunchedEffect(activity) {
        while (isActive) {
            metrics = runCatching { NativeBridge.liveMetrics() }.getOrNull()
            points = runCatching { NativeBridge.livePoints() }.getOrNull() ?: emptyList()
            delay(1000)
        }
    }

    fun refreshOnce() {
        scope.launch {
            metrics = runCatching { NativeBridge.liveMetrics() }.getOrNull()
            points = runCatching { NativeBridge.livePoints() }.getOrNull() ?: emptyList()
        }
    }

    fun onPause() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                NativeBridge.pauseSession()
                refreshOnce()
            } finally {
                busy = false
            }
        }
    }

    fun onResume() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                NativeBridge.resumeSession()
                refreshOnce()
            } finally {
                busy = false
            }
        }
    }

    fun onStop() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val detail = NativeBridge.stopSession()
                nav.replace("$activity/summary/${detail.session.id}")
            } catch (e: Exception) {
                busy = false
            }
        }
    }

    val isPaused = metrics?.state == "paused"
    val metricLabel = if (config.metric == Metric.PACE) "Pace" else "Speed"
    val formatMetric: (Double?) -> String = if (config.metric == Metric.PACE) ::formatPace else ::formatSpeed

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (isPaused) "Paused" else config.trackingLabel,
            style = MaterialTheme.typography.labelSmall,
            color = DiditColors.MutedForeground,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .background(
                        if (isPaused) DiditColors.Muted.copy(alpha = 0.3f) else DiditColors.Accent.copy(alpha = 0.15f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(232.dp)
                        .border(3.dp, if (isPaused) DiditColors.Muted else DiditColors.Brand, CircleShape),
                )
                Text(
                    formatDuration(metrics?.movingDurationMs ?: 0),
                    style = MaterialTheme.typography.displayLarge,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatBox(
                    "Distance",
                    formatDistance(metrics?.totalDistanceM),
                    modifier = Modifier.weight(1f),
                    centered = true,
                )
                StatBox(
                    metricLabel,
                    formatMetric(metrics?.avgPaceSPerKm),
                    modifier = Modifier.weight(1f),
                    centered = true,
                )
            }

            RouteMap(
                points = points,
                height = 180.dp,
                live = true,
                emptyLabel = "Waiting for GPS",
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isPaused) {
                DiditButton(onClick = ::onResume, enabled = !busy, size = DiditButtonSize.LG, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("Resume")
                }
            } else {
                DiditButton(onClick = ::onPause, enabled = !busy, size = DiditButtonSize.LG, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Text("Pause")
                }
            }
            DiditButton(
                onClick = ::onStop, enabled = !busy, size = DiditButtonSize.LG,
                variant = DiditButtonVariant.OUTLINE, modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Text("Stop")
            }
        }
    }
}
