package com.didit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.didit.app.model.Split
import com.didit.app.ui.theme.DiditColors
import com.didit.app.util.formatDuration

/** Mirrors src/components/SplitsTable.tsx. */
@Composable
fun SplitsTable(splits: List<Split>, modifier: Modifier = Modifier) {
    if (splits.isEmpty()) {
        Text("No full kilometres in this run.", fontSize = 14.sp, color = DiditColors.MutedForeground, modifier = modifier)
        return
    }

    val fastest = splits.minOf { it.durationMs }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DiditColors.Border),
        color = DiditColors.Card,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DiditColors.Muted.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("KM", modifier = Modifier.weight(1f), fontSize = 11.sp, color = DiditColors.MutedForeground)
                Text(
                    "TIME", modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                    fontSize = 11.sp, color = DiditColors.MutedForeground,
                )
                Text(
                    "↑ M", modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                    fontSize = 11.sp, color = DiditColors.MutedForeground,
                )
            }
            splits.forEach { s ->
                HorizontalDivider(color = DiditColors.Border)
                val isFastest = s.durationMs == fastest
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(s.kmIndex.toString(), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text(
                        formatDuration(s.durationMs),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = if (isFastest) DiditColors.Brand else DiditColors.Foreground,
                        fontWeight = if (isFastest) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        if (s.elevationGainM > 0) String.format("%.0f", s.elevationGainM) else "—",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = DiditColors.MutedForeground,
                    )
                }
            }
        }
    }
}
