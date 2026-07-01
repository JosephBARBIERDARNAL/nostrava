package com.didit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.didit.app.ui.theme.DiditColors

enum class DiditButtonVariant { PRIMARY, SECONDARY, OUTLINE, GHOST, DESTRUCTIVE }
enum class DiditButtonSize { SM, MD, LG, XL, ICON }

/** Mirrors src/components/ui/button.tsx's cva variants/sizes as a single composable. */
@Composable
fun DiditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DiditButtonVariant = DiditButtonVariant.PRIMARY,
    size: DiditButtonSize = DiditButtonSize.MD,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val (bg, fg) = when (variant) {
        DiditButtonVariant.PRIMARY -> DiditColors.Accent to DiditColors.AccentForeground
        DiditButtonVariant.SECONDARY -> DiditColors.Brand to DiditColors.BrandForeground
        DiditButtonVariant.OUTLINE -> DiditColors.Card to DiditColors.Foreground
        DiditButtonVariant.GHOST -> Color.Transparent to DiditColors.Foreground
        DiditButtonVariant.DESTRUCTIVE -> DiditColors.Destructive to DiditColors.DestructiveForeground
    }
    val height = when (size) {
        DiditButtonSize.SM -> 36.dp
        DiditButtonSize.MD -> 44.dp
        DiditButtonSize.LG -> 56.dp
        DiditButtonSize.XL -> 64.dp
        DiditButtonSize.ICON -> 40.dp
    }
    val hPad = when (size) {
        DiditButtonSize.SM -> 12.dp
        DiditButtonSize.MD -> 20.dp
        DiditButtonSize.LG -> 24.dp
        DiditButtonSize.XL -> 32.dp
        DiditButtonSize.ICON -> 0.dp
    }
    val fontSize = when (size) {
        DiditButtonSize.LG, DiditButtonSize.XL -> 16.sp
        else -> 14.sp
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .let { if (size == DiditButtonSize.ICON) it.width(40.dp) else it },
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = bg.copy(alpha = 0.5f),
            disabledContentColor = fg.copy(alpha = 0.5f),
        ),
        border = if (variant == DiditButtonVariant.OUTLINE) BorderStroke(1.dp, DiditColors.Border) else null,
        contentPadding = PaddingValues(horizontal = hPad),
    ) {
        ProvideTextStyle(LocalTextStyle.current.copy(fontSize = fontSize)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                content()
            }
        }
    }
}
