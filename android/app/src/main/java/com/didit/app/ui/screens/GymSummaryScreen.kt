package com.didit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.didit.app.bridge.NativeBridge
import com.didit.app.model.GymSessionRow
import com.didit.app.nav.replace
import com.didit.app.ui.components.DiditButton
import com.didit.app.ui.components.DiditButtonVariant
import com.didit.app.ui.components.DiditCard
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.formatDateTime
import kotlinx.coroutines.launch

@Composable
fun GymSummaryScreen(nav: NavHostController, id: Long) {
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<GymSessionRow?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        session = runCatching { NativeBridge.getGymSession(id) }.getOrNull()
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this workout?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        NativeBridge.deleteGymSession(id)
                        nav.replace("gym/home")
                    }
                }) { Text("Delete", color = DiditColors.Destructive) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    val s = session
    if (s == null) {
        Text("Loading…", color = DiditColors.MutedForeground)
        return
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
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DiditColors.MutedForeground)
            }
        }

        Text(
            "${s.exercises.size} exercise${if (s.exercises.size == 1) "" else "s"}",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            formatDateTime(s.loggedAtMs),
            style = MaterialTheme.typography.bodySmall,
            color = DiditColors.MutedForeground,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        DiditCard(modifier = Modifier.fillMaxWidth()) {
            s.exercises.forEachIndexed { i, name ->
                if (i > 0) HorizontalDivider(color = DiditColors.Border)
                Text(name, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 12.dp))
            }
        }

        DiditButton(
            onClick = { nav.navigate("gym/home") },
            variant = DiditButtonVariant.OUTLINE,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Text("Done")
        }
    }
}
