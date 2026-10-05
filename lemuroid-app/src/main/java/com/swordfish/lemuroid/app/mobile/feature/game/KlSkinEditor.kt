package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.swordfish.touchinput.radial.LemuroidPadTheme
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
    var baseColor by rememberSaveable { mutableStateOf(skinHex(initial.customBaseColor)) }
    var outline by rememberSaveable { mutableStateOf(skinHex(initial.customOutline)) }
    var opacity by rememberSaveable { mutableStateOf(initial.customOpacity.coerceIn(0.2f, 1f)) }
    var baseOpacity by rememberSaveable { mutableStateOf(initial.customBaseOpacity.coerceIn(0f, 1f)) }
    var font by rememberSaveable { mutableStateOf(initial.customFont) }
    var base by rememberSaveable { mutableStateOf(initial.customBase) }
    var corners by rememberSaveable { mutableStateOf(initial.customCornerRadius.coerceIn(0f, 36f)) }
    var border by rememberSaveable { mutableStateOf(initial.customOutlineWidth.coerceIn(0f, 3f)) }
    var labelScale by rememberSaveable { mutableStateOf(initial.customLabelScale.coerceIn(0.6f, 1.4f)) }
    var pressScale by rememberSaveable { mutableStateOf(initial.customPressScale.coerceIn(0.85f, 1f)) }
    var bold by rememberSaveable { mutableStateOf(initial.customBold) }
    var shadow by rememberSaveable { mutableStateOf(initial.customShadow) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val valid = listOf(fill, pressed, text, pressedText, baseColor, outline).all { skinColor(it) != null }
    val draft = initial.copy(
        skin = "CUSTOM", customFill = skinColor(fill) ?: initial.customFill,
        customPressed = skinColor(pressed) ?: initial.customPressed,
        customText = skinColor(text) ?: initial.customText,
        customPressedText = skinColor(pressedText) ?: initial.customPressedText,
        customOpacity = opacity, customFont = font, customBase = base,
        customBaseColor = skinColor(baseColor) ?: initial.customBaseColor,
        customBaseOpacity = baseOpacity, customCornerRadius = corners,
        customOutline = skinColor(outline) ?: initial.customOutline,
        customOutlineWidth = border, customLabelScale = labelScale,
        customBold = bold, customShadow = shadow, customPressScale = pressScale,
    )
    val dirty = draft.copy(skin = initial.skin) != initial || !valid
    fun close() { if (dirty) discard = true else onClose() }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.90f
    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 560.dp).heightIn(max = maxHeight).imePadding(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Estúdio de skins KL", style = MaterialTheme.typography.titleLarge)
                        Text("Prévia antes de salvar", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { close() }) { Icon(Icons.Default.Close, "Fechar editor") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Cores", "Base", "Letras").forEachIndexed { index, title ->
                        TextButton(onClick = { tab = index }) {
                            Text(title, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SkinPreview(draft)
                    when (tab) {
                        0 -> {
                            Text("Escolha uma paleta e personalize as cores.")
                            listOf(
                                listOf("Azul" to "#168FC4", "Rosa" to "#A42671", "Verde" to "#087F4C"),
                                listOf("Roxo" to "#7544B4", "Laranja" to "#C75C12", "Grafite" to "#303B4E"),
                            ).forEach { colors ->
                                Row { colors.forEach { (name, hex) ->
                                    TextButton(onClick = {
                                        fill = hex; pressed = "#D7F1FF"; text = "#FFFFFF"; pressedText = "#163A50"; baseColor = hex
                                    }) { Text(name) }
                                } }
                            }
                            SkinColorField("Botões", fill) { fill = it }
                            SkinColorField("Botões pressionados", pressed) { pressed = it }
                            SkinColorField("Letras", text) { text = it }
                            SkinColorField("Letras pressionadas", pressedText) { pressedText = it }
                            SkinSlider("Opacidade dos botões", opacity, 0.2f..1f, "${(opacity * 100).toInt()}%") { opacity = it }
                        }
                        1 -> {
                            Text("Bases próprias do KL, inspiradas em controles de emuladores.")
                            LemuroidPadTheme.baseOptions.forEach { (id, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = base == id, onClick = {
                                        base = id
                                        corners = when (id) { "RETRO" -> 4f; "MINIMAL" -> 12f; "PORTABLE" -> 20f; else -> 36f }
                                        baseOpacity = when (id) { "MINIMAL" -> 0f; "PORTABLE" -> 0.85f; else -> 0.12f }
                                        shadow = id != "MINIMAL"
                                    })
                                    Text(name)
                                }
                            }
                            SkinColorField("Cor da base", baseColor) { baseColor = it }
                            SkinSlider("Opacidade da base", baseOpacity, 0f..1f, "${(baseOpacity * 100).toInt()}%") { baseOpacity = it }
                            SkinSlider("Cantos dos botões", corners, 0f..36f, if (corners >= 36f) "Redondos" else "${corners.toInt()} dp") { corners = it }
                            SkinColorField("Cor do contorno", outline) { outline = it }
                            SkinSlider("Contorno", border, 0f..3f, "%.1f dp".format(border)) { border = it }
                            SkinSlider("Efeito ao pressionar", pressScale, 0.85f..1f, "${(pressScale * 100).toInt()}% do tamanho") { pressScale = it }
                            SkinToggle("Sombras nos controles", shadow) { shadow = it }
                        }
                        else -> {
                            Text("Fontes do Android; a aparência pode variar conforme o celular.")
                            LemuroidPadTheme.fontOptions.forEach { (id, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = font == id, onClick = { font = id })
                                    TextButton(onClick = { font = id }) {
                                        Text(name + " · Aa", fontFamily = LemuroidPadTheme.fontFor(id))
                                    }
                                }
                            }
                            SkinSlider("Tamanho das letras", labelScale, 0.6f..1.4f, "${(labelScale * 100).toInt()}%") { labelScale = it }
                            SkinToggle("Letras em negrito", bold) { bold = it }
                            Text("As setas continuam como ícones.")
                        }
                    }
                    Text("A skin fica salva para este console e esta orientação.", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { close() }) { Text("Cancelar") }
                    Button(enabled = valid, onClick = { onSave(draft) }) { Text("Salvar skin") }
                }
            }
        }
    }
    if (discard) {
        AlertDialog(
            onDismissRequest = { discard = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Descartar alterações?") },
            text = { Text("Sua skin atual continua salva. As mudanças desta prévia serão descartadas.") },
            confirmButton = { TextButton(onClick = onClose) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { discard = false }) { Text("Continuar editando") } },
        )
    }
}

@Composable
private fun SkinPreview(settings: TouchControllerSettingsManager.Settings) {
    val theme = LemuroidPadTheme.fromSettings(settings)
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Normal / pressionado", style = MaterialTheme.typography.labelMedium)
        Row(
            Modifier.fillMaxWidth().background(Color(settings.customBaseColor).copy(alpha = settings.customBaseOpacity), RoundedCornerShape(14.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(false, true).forEach { pressed ->
                val shape = RoundedCornerShape(if (settings.customCornerRadius >= 36f) 50.dp else settings.customCornerRadius.dp)
                Box(
                    Modifier.size(64.dp).graphicsLayer {
                        scaleX = if (pressed) theme.pressedScale else 1f
                        scaleY = if (pressed) theme.pressedScale else 1f
                    }.shadow(if (settings.customShadow) 4.dp else 0.dp, shape)
                        .background(theme.foregroundFill(pressed), shape)
                        .border(settings.customOutlineWidth.dp, theme.outlineColor, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (pressed) "B" else "A", color = theme.icons(pressed), fontFamily = theme.fontFamily,
                        fontWeight = if (theme.boldLabels) FontWeight.Bold else FontWeight.Normal,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize * theme.labelScale)
                }
            }
        }
    }
}

@Composable
private fun SkinColorField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { onValueChange(it.take(7)) }, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, supportingText = { Text("Cor em hexadecimal, por exemplo #168FC4") },
        singleLine = true, isError = skinColor(value) == null,
    )
}

@Composable
private fun SkinSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, change: (Float) -> Unit) {
    Column { Text("$label: $display"); Slider(value = value, onValueChange = change, valueRange = range) }
}

@Composable
private fun SkinToggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked = value, onCheckedChange = change)
    }
}
