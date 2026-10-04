package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class LemuroidPadTheme(private val emerald: Boolean = false) {
    private fun gray(
        luminosity: Float,
        opacity: Float,
    ): Color {
        return Color(luminosity, luminosity, luminosity, opacity)
    }

    val foregroundPadding: Dp = 8.dp
    val padding: Dp = 4.dp

    private val icons = if (emerald) Color(0xFFF0FFF4).copy(alpha = 0.92f) else gray(0.0f, 0.50f)
    private val iconsPressed = if (emerald) Color(0xFF063D25) else gray(1.0f, 0.50f)

    private val level3Fill = if (emerald) Color(0xFF087F4C).copy(alpha = 0.72f) else gray(1.0f, 0.50f)
    private val level3FillPressed = if (emerald) Color(0xFF86EFAC).copy(alpha = 0.90f) else gray(0.0f, 0.50f)
    val level3Shadow = DefaultShadowColor.copy(0.05f)
    val level3ShadowWidth = 4.dp

    private val level2Fill = if (emerald) Color(0xFF065F46).copy(alpha = 0.22f) else gray(1.0f, 0.125f)
    private val level2FillPressed = if (emerald) Color(0xFF86EFAC).copy(alpha = 0.35f) else gray(0.0f, 0.125f)
    val level2Shadow = DefaultShadowColor.copy(0.05f)
    val level2ShadowWidth = 4.dp

    val level1Fill = if (emerald) Color(0xFF065F46).copy(alpha = 0.16f) else gray(1.0f, 0.10f)
    val level1Shadow = DefaultShadowColor.copy(0.10f)
    val level1ShadowWidth = 4.dp

    val level0CornerRadius = 0.dp
    val level0Fill = if (emerald) Color(0xFF022C22).copy(alpha = 0.12f) else gray(1.0f, 0.05f)
    val level0Shadow = DefaultShadowColor.copy(0.10f)
    val level0ShadowWidth = 2.dp

    fun compositeFill(pressed: Boolean): Color {
        return if (pressed) {
            level2FillPressed
        } else {
            level2Fill
        }
    }

    fun foregroundFill(pressed: Boolean): Color {
        return if (pressed) {
            level3FillPressed
        } else {
            level3Fill
        }
    }

    fun icons(pressed: Boolean): Color {
        return if (pressed) {
            iconsPressed
        } else {
            icons
        }
    }
}

val LocalLemuroidPadTheme =
    compositionLocalOf<LemuroidPadTheme> {
        error("LemuroidPadTheme is missing")
    }
    
