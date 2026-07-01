package com.didit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.didit.app.bridge.NativeBridge
import com.didit.app.model.SessionDetail
import com.didit.app.nav.replace
import com.didit.app.ui.components.DiditButton
import com.didit.app.ui.components.DiditButtonVariant
import com.didit.app.ui.components.DiditCard
import com.didit.app.ui.components.ElevationChart
import com.didit.app.ui.components.RouteMap
import com.didit.app.ui.components.SplitsTable
import com.didit.app.ui.components.StatBox
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.ACTIVITY_CONFIG
import com.didit.app.util.Metric
import com.didit.app.util.formatDateTime
import com.didit.app.util.formatDistance
import com.didit.app.util.formatDuration
import com.didit.app.util.formatPace
import com.didit.app.util.formatSpeed
import kotlinx.coroutines.launch

@Composable
fun ActivitySummaryScreen(nav: NavHostController, activity: String, id: Long) {
    val config = ACTIVITY_CONFIG.getValue(activity)
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<SessionDetail?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        detail = runCatching { NativeBridge.getSession(id) }.getOrNull()
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this ${config.noun}?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        NativeBridge.deleteSession(id)
                        nav.replace("$activity/home")
                    }
                }) { Text("Delete", color = DiditColors.Destructive) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    val d = detail
    if (d == null) {
        Text("Loading…", color = DiditColors.MutedForeground)
        return
    }

    val s = d.session
    val metricLabel = if (config.metric == Metric.PACE) "Avg pace" else "Avg speed"
    val formatMetric: (Double?) -> String = if (config.metric == Metric.PACE) ::formatPace else ::formatSpeed

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { nav.navigate("$activity/home") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = DiditColors.MutedForeground)
                Text("Home", color = DiditColors.MutedForeground)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DiditColors.MutedForeground)
            }
        }

        Text(
            "${formatDistance(s.totalDistanceM)} · ${formatDuration(s.movingDurationMs ?: 0)}",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            formatDateTime(s.startedAtMs),
            style = MaterialTheme.typography.bodySmall,
            color = DiditColors.MutedForeground,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        DiditCard(contentPadding = PaddingValues(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            RouteMap(points = d.points, height = 220.dp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatBox(metricLabel, formatMetric(s.avgPaceSPerKm), modifier = Modifier.weight(1f))
                StatBox("Moving time", formatDuration(s.movingDurationMs ?: 0), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatBox("Total time", formatDuration(s.totalDurationMs ?: 0), modifier = Modifier.weight(1f))
                StatBox(
                    "Elevation",
                    "↑${"%.0f".format(s.elevationGainM ?: 0.0)} ↓${"%.0f".format(s.elevationLossM ?: 0.0)} m",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text("Splits", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        SplitsTable(d.splits, modifier = Modifier.padding(bottom = 24.dp))

        Text("Elevation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        DiditCard(modifier = Modifier.fillMaxWidth()) {
            ElevationChart(d.points)
        }

        DiditButton(
            onClick = { nav.navigate("$activity/home") },
            variant = DiditButtonVariant.OUTLINE,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Text("Done")
        }
    }
}
