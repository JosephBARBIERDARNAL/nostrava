package com.didit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.didit.app.bridge.NativeBridge
import com.didit.app.model.GymHistoryBucket
import com.didit.app.ui.components.DiditCard
import com.didit.app.ui.components.DiditTabs
import com.didit.app.ui.components.GymSessionListItem
import com.didit.app.ui.components.StatBox
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.formatDate
import com.didit.app.util.shiftAnchor

@Composable
fun GymHistoryScreen(nav: NavHostController) {
    var range by remember { mutableStateOf("week") }
    var anchorMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var bucket by remember { mutableStateOf<GymHistoryBucket?>(null) }

    LaunchedEffect(range, anchorMs) {
        bucket = runCatching { NativeBridge.listGymRange(range, anchorMs) }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { nav.navigate("gym/home") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = DiditColors.MutedForeground)
                Text("Home", color = DiditColors.MutedForeground)
            }
            Text("History", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(48.dp))
        }

        DiditTabs(
            options = listOf("week" to "Week", "month" to "Month", "year" to "Year"),
            selected = range,
            onSelect = { range = it },
            modifier = Modifier.padding(bottom = 20.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { anchorMs = shiftAnchor(anchorMs, range, forward = false) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = DiditColors.MutedForeground)
            }
            Text(bucket?.label ?: "—", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { anchorMs = shiftAnchor(anchorMs, range, forward = true) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = DiditColors.MutedForeground)
            }
        }

        bucket?.let { b ->
            StatBox(
                "Workouts", b.sessionCount.toString(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                centered = true,
            )
        }

        val sessions = bucket?.sessions ?: emptyList()
        if (bucket != null && sessions.isEmpty()) {
            DiditCard {
                Text(
                    "No workouts in this $range.",
                    color = DiditColors.MutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions) { s ->
                    GymSessionListItem(
                        dateLabel = formatDate(s.loggedAtMs),
                        exercises = s.exercises,
                        onClick = { nav.navigate("gym/summary/${s.id}") },
                    )
                }
            }
        }
    }
}
