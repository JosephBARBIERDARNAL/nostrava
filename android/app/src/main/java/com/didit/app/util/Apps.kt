package com.didit.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.vector.ImageVector

data class AppConfig(val label: String, val route: String, val icon: ImageVector)

/** Keyed by app kind ("running"/"biking"/"gym") — mirrors src/lib/apps.ts. */
val APPS: Map<String, AppConfig> = mapOf(
    "running" to AppConfig("Running", "running/home", Icons.AutoMirrored.Filled.DirectionsRun),
    "biking" to AppConfig("Biking", "biking/home", Icons.AutoMirrored.Filled.DirectionsBike),
    "gym" to AppConfig("Gym", "gym/home", Icons.Filled.FitnessCenter),
)
