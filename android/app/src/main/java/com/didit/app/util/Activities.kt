package com.didit.app.util

/** Metric shown alongside distance/duration: "pace" (running) or "speed" (biking). */
enum class Metric { PACE, SPEED }

data class ActivityConfig(
    val title: String,
    val verb: String,
    val noun: String,
    val nounPlural: String,
    val trackingLabel: String,
    val metric: Metric,
)

/** Keyed by the raw activity string ("running"/"biking") used everywhere — nav args, JNI calls, DB rows. */
val ACTIVITY_CONFIG: Map<String, ActivityConfig> = mapOf(
    "running" to ActivityConfig(
        title = "Running",
        verb = "Start a run",
        noun = "run",
        nounPlural = "Runs",
        trackingLabel = "Running",
        metric = Metric.PACE,
    ),
    "biking" to ActivityConfig(
        title = "Biking",
        verb = "Start a ride",
        noun = "ride",
        nounPlural = "Rides",
        trackingLabel = "Riding",
        metric = Metric.SPEED,
    ),
)
