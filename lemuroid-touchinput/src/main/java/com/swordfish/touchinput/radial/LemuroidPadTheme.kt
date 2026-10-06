package com.swordfish.touchinput.radial

import android.graphics.Typeface
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.text.font.FontFamily
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class LemuroidPadTheme(
    skin: String = "CLASSIC",
    private val settings: TouchControllerSettingsManager.Settings? = null,
) {
    private val custom = skin == "CUSTOM" && settings != null
    private val opacity = if (custom) settings!!.customOpacity.coerceIn(0.2f, 1f) else 0.72f
    val fontFamily: FontFamily = fontFor(if (custom) settings!!.customFont else "ROUNDED")
    val labelScale = if (custom) settings!!.customLabelScale.coerceIn(0.6f, 1.4f) else 1f
    val boldLabels = if (custom) settings!!.customBold else true
    val pressedScale = if (custom) settings!!.customPressScale.coerceIn(0.85f, 1f) else 1f
    val buttonCornerRadius = if (custom) {
        if (settings!!.customCornerRadius >= 36f) Dp.Infinity else settings.customCornerRadius.coerceIn(0f, 36f).dp
    } else Dp.Infinity
    val outlineColor = if (skin == "SONIC") Color(0xFFFFD342).copy(alpha = 0.8f) else if (custom) Color(settings!!.customOutline) else Color.Transparent
    val outlineWidth = if (skin == "SONIC") 2.dp else if (custom) settings!!.customOutlineWidth.coerceIn(0f, 6f).dp else 0.dp
    private val baseColor = if (custom) Color(settings!!.customBaseColor) else null
    private val baseOpacity = if (custom) settings!!.customBaseOpacity.coerceIn(0f, 1f) else 0f
    private val shadows = !custom || settings!!.customShadow
    private data class Palette(
        val fill: Color,
        val pressedFill: Color,
        val pressedIcon: Color,
    )

    private val palette =
        when (skin) {
            "CUSTOM" -> Palette(
                Color(settings?.customFill ?: 0xFF168FC4),
                Color(settings?.customPressed ?: 0xFF88DEFF),
                Color(settings?.customPressedText ?: 0xFF083D55),
            )
            "SONIC" -> Palette(Color(0xFF0758D8), Color(0xFFFFD342), Color(0xFF123A75))
            "EMERALD" -> Palette(Color(0xFF087F4C), Color(0xFF86EFAC), Color(0xFF063D25))
            "FIRE" -> Palette(Color(0xFFB93820), Color(0xFFFFC38A), Color(0xFF57170B))
            "DARK" -> Palette(Color(0xFF262B36), Color(0xFF94A3B8), Color(0xFF101827))
            else -> null
        }

    companion object {
        val fontOptions = listOf(
            "ROUNDED" to "Padrão KL", "SERIF" to "Clássica", "MONO" to "Retro",
            "CURSIVE" to "Cursiva", "CONDENSED" to "Compacta", "LIGHT" to "Leve",
            "MEDIUM" to "Moderna", "THIN" to "Fina",
        )
        fun fontFor(id: String): FontFamily {
            KlFontStore.typeface(id)?.let { return FontFamily(it) }
            return when (id) {
            "SERIF" -> FontFamily.Serif
            "MONO" -> FontFamily.Monospace
            "CURSIVE" -> FontFamily.Cursive
            "CONDENSED" -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
            "LIGHT" -> FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
            "MEDIUM" -> FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))
            "THIN" -> FontFamily(Typeface.create("sans-serif-thin", Typeface.NORMAL))
            else -> FontFamily.SansSerif
            }
        }
        val baseOptions = listOf(
            "NORMAL" to "Normal KL", "RETRO" to "Retro quadrado",
            "MINIMAL" to "Minimalista", "PORTABLE" to "Console portátil",
            "OUTLINE" to "Contorno portátil • inspirado no Citra MMJ",
        )

        fun fromSettings(settings: TouchControllerSettingsManager.Settings) =
            LemuroidPadTheme(settings.skin, settings)
        val skinOptions =
            listOf(
                "CLASSIC" to "KL Classic",
                "SONIC" to "Sonic Edition • azul e anéis dourados",
                "EMERALD" to "KL Emerald",
                "FIRE" to "KL Fire",
                "DARK" to "KL Dark",
                "CUSTOM" to "Minha skin",
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

    private val icons = if (custom) Color(settings!!.customText) else if (palette != null) Color(0xFFF0FFF4).copy(alpha = 0.92f) else gray(0.0f, 0.50f)
    private val iconsPressed = palette?.pressedIcon ?: gray(1.0f, 0.50f)

    private val level3Fill = palette?.fill?.copy(alpha = opacity) ?: gray(1.0f, 0.50f)
    private val level3FillPressed = palette?.pressedFill?.copy(alpha = if (custom) opacity else 0.90f) ?: gray(0.0f, 0.50f)
    val level3Shadow = DefaultShadowColor.copy(if (shadows) 0.05f else 0f)
    val level3ShadowWidth = 4.dp

    private val level2Fill = palette?.fill?.copy(alpha = 0.22f) ?: gray(1.0f, 0.125f)
    private val level2FillPressed = palette?.pressedFill?.copy(alpha = 0.35f) ?: gray(0.0f, 0.125f)
    val level2Shadow = DefaultShadowColor.copy(if (shadows) 0.05f else 0f)
    val level2ShadowWidth = 4.dp

    val level1Fill = baseColor?.copy(alpha = baseOpacity) ?: palette?.fill?.copy(alpha = 0.16f) ?: gray(1.0f, 0.10f)
    val level1Shadow = DefaultShadowColor.copy(if (shadows) 0.10f else 0f)
    val level1ShadowWidth = 4.dp

    val level0CornerRadius = if (custom && settings!!.customBase == "PORTABLE") 24.dp else 0.dp
    val level0Fill = baseColor?.copy(alpha = baseOpacity) ?: palette?.fill?.copy(alpha = 0.12f) ?: gray(1.0f, 0.05f)
    val level0Shadow = DefaultShadowColor.copy(if (shadows) 0.10f else 0f)
    val level0ShadowWidth = 2.dp

    fun compositeFill(pressed: Boolean): Color {
        return if (custom) {
            baseColor!!.copy(alpha = if (pressed) (baseOpacity + 0.08f).coerceAtMost(1f) else baseOpacity)
        } else if (pressed) {
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
    
