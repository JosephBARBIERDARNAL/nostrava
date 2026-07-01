package com.didit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.didit.app.ui.theme.DiditColors

/** Mirrors App.tsx's `max-w-[440px] mx-auto px-5 py-6` mobile shell. */
@Composable
fun DiditScreenShell(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiditColors.Background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            content()
        }
    }
}
