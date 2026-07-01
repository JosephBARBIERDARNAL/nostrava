package com.didit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.didit.app.ui.theme.DiditColors

/** A recent-session/history row — same markup used on ActivityHome and ActivityHistory. */
@Composable
fun SessionListItem(dateLabel: String, distance: String, duration: String, metric: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = DiditColors.Card,
        border = BorderStroke(1.dp, DiditColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(dateLabel, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(distance, style = MaterialTheme.typography.titleMedium)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(duration, style = MaterialTheme.typography.bodySmall, color = DiditColors.MutedForeground, modifier = Modifier.weight(1f))
                Text(metric, style = MaterialTheme.typography.bodySmall, color = DiditColors.MutedForeground, textAlign = TextAlign.End)
            }
        }
    }
}

/** A gym-session row — same markup used on GymHome and GymHistory. */
@Composable
fun GymSessionListItem(dateLabel: String, exercises: List<String>, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = DiditColors.Card,
        border = BorderStroke(1.dp, DiditColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(dateLabel, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(
                    "${exercises.size} exercise${if (exercises.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DiditColors.MutedForeground,
                )
            }
            Text(
                exercises.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = DiditColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
