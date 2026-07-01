package com.didit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.didit.app.bridge.NativeBridge
import com.didit.app.model.GymSessionRow
import com.didit.app.ui.components.DiditButton
import com.didit.app.ui.components.DiditButtonSize
import com.didit.app.ui.components.DiditCard
import com.didit.app.ui.components.GymSessionListItem
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.formatDate

@Composable
fun GymHomeScreen(nav: NavHostController) {
    var recent by remember { mutableStateOf<List<GymSessionRow>>(emptyList()) }

    LaunchedEffect(Unit) {
        recent = runCatching { NativeBridge.listRecentGym(5) }.getOrDefault(emptyList())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Gym", style = MaterialTheme.typography.displayMedium)
            IconButton(onClick = { nav.navigate("selector") }) {
                Icon(Icons.Filled.Home, contentDescription = "All apps", tint = DiditColors.MutedForeground)
            }
        }

        DiditButton(
            onClick = { nav.navigate("gym/log") },
            size = DiditButtonSize.XL,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null)
            Text("Log workout", style = MaterialTheme.typography.titleLarge)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Recent", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { nav.navigate("gym/history") }) {
                Text("History", color = DiditColors.Brand)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DiditColors.Brand)
            }
        }

        if (recent.isEmpty()) {
            DiditCard {
                Text(
                    "No workouts yet. Tap \"Log workout\" to record your first.",
                    color = DiditColors.MutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recent) { s ->
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
