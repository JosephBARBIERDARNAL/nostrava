package com.didit.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.didit.app.nav.replace
import com.didit.app.ui.components.DiditButton
import com.didit.app.ui.components.DiditButtonSize
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.DEFAULT_EXERCISES
import kotlinx.coroutines.launch

@Composable
fun GymLogScreen(nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var saving by remember { mutableStateOf(false) }

    fun toggle(name: String) {
        selected = if (selected.contains(name)) selected - name else selected + name
    }

    fun onSave() {
        if (saving || selected.isEmpty()) return
        saving = true
        scope.launch {
            try {
                NativeBridge.createGymSession(selected.toList())
                nav.replace("gym/home")
            } catch (e: Exception) {
                saving = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { nav.navigate("gym/home") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = DiditColors.MutedForeground)
                Text("Cancel", color = DiditColors.MutedForeground)
            }
            Text("Log Workout", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(64.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(DEFAULT_EXERCISES) { name ->
                val active = selected.contains(name)
                Surface(
                    onClick = { toggle(name) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) DiditColors.Brand.copy(alpha = 0.1f) else DiditColors.Card,
                    border = BorderStroke(1.dp, if (active) DiditColors.Brand else DiditColors.Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(name, fontWeight = FontWeight.Medium)
                        if (active) Icon(Icons.Filled.Check, contentDescription = null, tint = DiditColors.Brand)
                    }
                }
            }
        }

        DiditButton(
            onClick = ::onSave,
            enabled = !saving && selected.isNotEmpty(),
            size = DiditButtonSize.XL,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (selected.isNotEmpty()) "Save (${selected.size})" else "Save", style = MaterialTheme.typography.titleLarge)
        }
    }
}
