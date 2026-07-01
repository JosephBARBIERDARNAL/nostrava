package com.didit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.didit.app.LocationBridge
import com.didit.app.ui.components.DiditButton
import com.didit.app.ui.components.DiditButtonSize
import com.didit.app.ui.components.DiditButtonVariant
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.APPS
import com.didit.app.util.formatInstallDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppSelectorScreen(nav: NavHostController) {
    var installedAtMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        val ms = withContext(Dispatchers.IO) { LocationBridge.installationInstalledAtMs() }
        installedAtMs = if (ms > 0) ms else null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)) {
            Text("didit", style = MaterialTheme.typography.displayMedium)
            installedAtMs?.let {
                Text(
                    "Installed ${formatInstallDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DiditColors.MutedForeground,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
        ) {
            listOf("running", "biking", "gym").forEach { key ->
                val app = APPS.getValue(key)
                val variant = when (key) {
                    "running" -> DiditButtonVariant.PRIMARY
                    "biking" -> DiditButtonVariant.SECONDARY
                    else -> DiditButtonVariant.OUTLINE
                }
                DiditButton(
                    onClick = { nav.navigate(app.route) },
                    variant = variant,
                    size = DiditButtonSize.XL,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(app.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(app.label, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
