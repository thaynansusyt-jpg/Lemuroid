package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class LemuroidPadTheme(skin: String = "CLASSIC") {
    private data class Palette(
        val fill: Color,
        val pressedFill: Color,
        val pressedIcon: Color,
    )

    private val palette =
        when (skin) {
            "EMERALD" -> Palette(Color(0xFF087F4C), Color(0xFF86EFAC), Color(0xFF063D25))
            "FIRE" -> Palette(Color(0xFFB93820), Color(0xFFFFC38A), Color(0xFF57170B))
            "DARK" -> Palette(Color(0xFF262B36), Color(0xFF94A3B8), Color(0xFF101827))
            else -> null
        }

    companion object {
        val skinOptions =
            listOf(
                "CLASSIC" to "KL Classic",
                "EMERALD" to "KL Emerald",
                "FIRE" to "KL Fire",
                "DARK" to "KL Dark",
            )
    }

    private fun gray(
        luminosity: Float,
        opacity: Float,
    ): Color {
        return Color(luminosity, luminosity, luminosity, opacity)
    }

    val foregroundPadding: Dp = 8.dp
    val padding: Dp = 4.dp

    private val icons = if (palette != null) Color(0xFFF0FFF4).copy(alpha = 0.92f) else gray(0.0f, 0.50f)
    private val iconsPressed = palette?.pressedIcon ?: gray(1.0f, 0.50f)

    private val level3Fill = palette?.fill?.copy(alpha = 0.72f) ?: gray(1.0f, 0.50f)
    private val level3FillPressed = palette?.pressedFill?.copy(alpha = 0.90f) ?: gray(0.0f, 0.50f)
    val level3Shadow = DefaultShadowColor.copy(0.05f)
    val level3ShadowWidth = 4.dp

    private val level2Fill = palette?.fill?.copy(alpha = 0.22f) ?: gray(1.0f, 0.125f)
    private val level2FillPressed = palette?.pressedFill?.copy(alpha = 0.35f) ?: gray(0.0f, 0.125f)
    val level2Shadow = DefaultShadowColor.copy(0.05f)
    val level2ShadowWidth = 4.dp

    val level1Fill = palette?.fill?.copy(alpha = 0.16f) ?: gray(1.0f, 0.10f)
    val level1Shadow = DefaultShadowColor.copy(0.10f)
    val level1ShadowWidth = 4.dp

    val level0CornerRadius = 0.dp
    val level0Fill = palette?.fill?.copy(alpha = 0.12f) ?: gray(1.0f, 0.05f)
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
    
