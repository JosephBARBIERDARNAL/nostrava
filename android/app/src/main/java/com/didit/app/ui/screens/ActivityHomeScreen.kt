package com.didit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.didit.app.LocationBridge
import com.didit.app.bridge.NativeBridge
import com.didit.app.model.SessionRow
import com.didit.app.nav.replace
import com.didit.app.ui.components.DiditButton
import com.didit.app.ui.components.DiditButtonSize
import com.didit.app.ui.components.DiditCard
import com.didit.app.ui.components.SessionListItem
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.ACTIVITY_CONFIG
import com.didit.app.util.Metric
import com.didit.app.util.formatDate
import com.didit.app.util.formatDistance
import com.didit.app.util.formatDuration
import com.didit.app.util.formatInstallDate
import com.didit.app.util.formatPace
import com.didit.app.util.formatSpeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ActivityHomeScreen(nav: NavHostController, activity: String) {
    val config = ACTIVITY_CONFIG.getValue(activity)
    val scope = rememberCoroutineScope()

    var recent by remember { mutableStateOf<List<SessionRow>>(emptyList()) }
    var updatedAtMs by remember { mutableStateOf<Long?>(null) }
    var starting by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        val ms = withContext(Dispatchers.IO) { LocationBridge.installationUpdatedAtMs() }
        updatedAtMs = if (ms > 0) ms else null
        recent = runCatching { NativeBridge.listRecent(5, activity) }.getOrDefault(emptyList())
        val active = runCatching { NativeBridge.activeActivity() }.getOrNull()
        if (active == activity) nav.replace("$activity/track")
    }

    fun onStart() {
        if (starting) return
        starting = true
        scope.launch {
            try {
                NativeBridge.startSession(activity)
                nav.navigate("$activity/track")
            } catch (e: Exception) {
                starting = false
            }
        }
    }

    val formatMetric: (Double?) -> String = if (config.metric == Metric.PACE) ::formatPace else ::formatSpeed

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(config.title, style = MaterialTheme.typography.displayMedium)
                updatedAtMs?.let {
                    Text(
                        "Version ${formatInstallDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DiditColors.MutedForeground,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            IconButton(onClick = { nav.navigate("selector") }) {
                Icon(Icons.Filled.Home, contentDescription = "All apps", tint = DiditColors.MutedForeground)
            }
        }

        DiditButton(
            onClick = ::onStart,
            enabled = !starting,
            size = DiditButtonSize.XL,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(config.verb, style = MaterialTheme.typography.titleLarge)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Recent", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { nav.navigate("$activity/history") }) {
                Text("History", color = DiditColors.Brand)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DiditColors.Brand, modifier = Modifier.size(16.dp))
            }
        }

        if (recent.isEmpty()) {
            DiditCard {
                Text(
                    "No ${config.nounPlural.lowercase()} yet. Tap \"${config.verb}\" to record your first.",
                    color = DiditColors.MutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recent) { s ->
                    SessionListItem(
                        dateLabel = formatDate(s.startedAtMs),
                        distance = formatDistance(s.totalDistanceM),
                        duration = formatDuration(s.movingDurationMs ?: 0),
                        metric = formatMetric(s.avgPaceSPerKm),
                        onClick = { nav.navigate("$activity/summary/${s.id}") },
                    )
                }
            }
        }
    }
}
