package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager

private fun skinHex(value: Long) = "#%06X".format(value and 0xFFFFFF)
private fun skinColor(value: String): Long? {
    val hex = value.trim().removePrefix("#")
    if (!hex.matches(Regex("[0-9a-fA-F]{6}"))) return null
    return 0xFF000000L or hex.toLong(16)
}

@Composable
fun KlSkinEditor(
    initial: TouchControllerSettingsManager.Settings,
    onClose: () -> Unit,
    onSave: (TouchControllerSettingsManager.Settings) -> Unit,
) {
    var fill by rememberSaveable { mutableStateOf(skinHex(initial.customFill)) }
    var pressed by rememberSaveable { mutableStateOf(skinHex(initial.customPressed)) }
    var text by rememberSaveable { mutableStateOf(skinHex(initial.customText)) }
    var pressedText by rememberSaveable { mutableStateOf(skinHex(initial.customPressedText)) }
    var opacity by rememberSaveable { mutableStateOf(initial.customOpacity.coerceIn(0.2f, 1f)) }
    var font by rememberSaveable { mutableStateOf(initial.customFont) }
    val valid = listOf(fill, pressed, text, pressedText).all { skinColor(it) != null }
    val fontFamily = when (font) {
        "SERIF" -> FontFamily.Serif
        "MONO" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Crie sua skin KL") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Prévia: normal e pressionado")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(false, true).forEach { isPressed ->
                        Text(
                            text = if (isPressed) "B" else "A",
                            modifier = Modifier.background(
                                Color(skinColor(if (isPressed) pressed else fill) ?: initial.customFill)
                                    .copy(alpha = opacity), RoundedCornerShape(18.dp),
                            ).padding(horizontal = 28.dp, vertical = 16.dp),
                            color = Color(skinColor(if (isPressed) pressedText else text) ?: initial.customText),
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                Text("Escolha uma base ou digite suas próprias cores abaixo.")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Azul" to "#168FC4", "Rosa" to "#A42671", "Verde" to "#087F4C").forEach { (name, hex) ->
                        TextButton(onClick = {
                            fill = hex; pressed = "#C3F1FF"; text = "#FFFFFF"; pressedText = "#163A50"
                        }) { Text(name) }
                    }
                }
                SkinColorField("Cor dos botões", fill) { fill = it }
                SkinColorField("Cor ao pressionar", pressed) { pressed = it }
                SkinColorField("Cor das letras", text) { text = it }
                SkinColorField("Letras ao pressionar", pressedText) { pressedText = it }
                Text("Opacidade: ${(opacity * 100).toInt()}%")
                Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.2f..1f)
                Text("Fonte das letras")
                listOf("ROUNDED" to "Padrão", "SERIF" to "Clássica", "MONO" to "Retro", "CURSIVE" to "Cursiva")
                    .forEach { (id, name) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = font == id, onClick = { font = id })
                            TextButton(onClick = { font = id }) { Text(name) }
                        }
                    }
                Text("Salva para este console e esta orientação. A fonte muda as letras; as setas continuam como ícones.")
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(initial.copy(
                    skin = "CUSTOM",
                    customFill = skinColor(fill)!!,
                    customPressed = skinColor(pressed)!!,
                    customText = skinColor(text)!!,
                    customPressedText = skinColor(pressedText)!!,
                    customOpacity = opacity,
                    customFont = font,
                ))
            }) { Text("Salvar skin") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}

@Composable
private fun SkinColorField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(7)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text("Exemplo: #168FC4") },
        singleLine = true,
        isError = skinColor(value) == null,
    )
}
