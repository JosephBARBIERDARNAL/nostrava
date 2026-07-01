package com.didit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.didit.app.ui.theme.DiditColors

/** The small bordered stat tile reused across ActivityTrack/Summary/History and GymHistory. */
@Composable
fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge,
    contentPadding: Dp = 16.dp,
    centered: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DiditColors.Card,
        border = BorderStroke(1.dp, DiditColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            horizontalAlignment = if (centered) androidx.compose.ui.Alignment.CenterHorizontally else androidx.compose.ui.Alignment.Start,
        ) {
            Text(
                label.uppercase(),
                fontSize = 11.sp,
                color = DiditColors.MutedForeground,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            Text(
                value,
                style = valueStyle,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}
