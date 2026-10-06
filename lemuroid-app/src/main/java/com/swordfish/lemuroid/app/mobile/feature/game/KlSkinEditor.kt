package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.swordfish.touchinput.radial.KlFontStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
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
    orientationName: String? = null,
) {
    val snapshot = remember { initial }
    var fill by rememberSaveable { mutableStateOf(skinHex(snapshot.customFill)) }
    var pressed by rememberSaveable { mutableStateOf(skinHex(snapshot.customPressed)) }
    var text by rememberSaveable { mutableStateOf(skinHex(snapshot.customText)) }
    var pressedText by rememberSaveable { mutableStateOf(skinHex(snapshot.customPressedText)) }
    var baseColor by rememberSaveable { mutableStateOf(skinHex(snapshot.customBaseColor)) }
    var outline by rememberSaveable { mutableStateOf(skinHex(snapshot.customOutline)) }
    var opacity by rememberSaveable { mutableStateOf(snapshot.customOpacity.coerceIn(0.2f, 1f)) }
    var baseOpacity by rememberSaveable { mutableStateOf(snapshot.customBaseOpacity.coerceIn(0f, 1f)) }
    var font by rememberSaveable { mutableStateOf(snapshot.customFont) }
    var base by rememberSaveable { mutableStateOf(snapshot.customBase) }
    var corners by rememberSaveable { mutableStateOf(snapshot.customCornerRadius.coerceIn(0f, 36f)) }
    var border by rememberSaveable { mutableStateOf(snapshot.customOutlineWidth.coerceIn(0f, 6f)) }
    var labelScale by rememberSaveable { mutableStateOf(snapshot.customLabelScale.coerceIn(0.6f, 1.4f)) }
    var pressScale by rememberSaveable { mutableStateOf(snapshot.customPressScale.coerceIn(0.85f, 1f)) }
    var bold by rememberSaveable { mutableStateOf(snapshot.customBold) }
    var shadow by rememberSaveable { mutableStateOf(snapshot.customShadow) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val clickSound = com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberKlMenuClick()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var fontMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val importFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try {
                font = withContext(Dispatchers.IO) { KlFontStore.importFont(context, uri) }
                fontMessage = "Fonte importada. Salve a skin para aplicar."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { fontMessage = failure.message ?: "Não foi possível importar a fonte." }
            finally { importing = false }
        }
    }
    val valid = listOf(fill, pressed, text, pressedText, baseColor, outline).all { skinColor(it) != null }
    val draft = snapshot.copy(
        skin = "CUSTOM", customFill = skinColor(fill) ?: snapshot.customFill,
        customPressed = skinColor(pressed) ?: snapshot.customPressed,
        customText = skinColor(text) ?: snapshot.customText,
        customPressedText = skinColor(pressedText) ?: snapshot.customPressedText,
        customOpacity = opacity, customFont = font, customBase = base,
        customBaseColor = skinColor(baseColor) ?: snapshot.customBaseColor,
        customBaseOpacity = baseOpacity, customCornerRadius = corners,
        customOutline = skinColor(outline) ?: snapshot.customOutline,
        customOutlineWidth = border, customLabelScale = labelScale,
        customBold = bold, customShadow = shadow, customPressScale = pressScale,
    )
    val dirty = draft.copy(skin = snapshot.skin) != snapshot || !valid
    fun close() { if (importing) return; if (dirty) discard = true else onClose() }
    val landscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.95f
    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = if (landscape) 840.dp else 560.dp).heightIn(max = maxHeight).imePadding(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Estúdio de skins KL", style = MaterialTheme.typography.titleLarge)
                        Text(if ((orientationName ?: if (landscape) "LANDSCAPE" else "PORTRAIT") == "LANDSCAPE") "Skin horizontal • prévia ao vivo" else "Skin vertical • prévia ao vivo", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { close() }) { Icon(Icons.Default.Close, "Fechar editor") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Cores", "Base", "Letras", "Modelos").forEachIndexed { index, title ->
                        TextButton(onClick = { clickSound(); tab = index }) {
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
                                        corners = when (id) { "RETRO" -> 4f; "MINIMAL" -> 12f; "PORTABLE" -> 20f; "OUTLINE" -> 36f; else -> 36f }
                                        baseOpacity = when (id) { "MINIMAL", "OUTLINE" -> 0f; "PORTABLE" -> 0.85f; else -> 0.12f }
                                        shadow = id !in listOf("MINIMAL", "OUTLINE")
                                        if (id == "OUTLINE") { opacity = 0.2f; border = 2f; outline = "#FFFFFF" }
                                    })
                                    Text(name)
                                }
                            }
                            SkinColorField("Cor da base", baseColor) { baseColor = it }
                            SkinSlider("Opacidade da base", baseOpacity, 0f..1f, "${(baseOpacity * 100).toInt()}%") { baseOpacity = it }
                            SkinSlider("Cantos dos botões", corners, 0f..36f, if (corners >= 36f) "Redondos" else "${corners.toInt()} dp") { corners = it }
                            SkinColorField("Cor do contorno", outline) { outline = it }
                            SkinSlider("Contorno", border, 0f..6f, "%.1f dp".format(border)) { border = it }
                            SkinSlider("Efeito ao pressionar", pressScale, 0.85f..1f, "${(pressScale * 100).toInt()}% do tamanho") { pressScale = it }
                            SkinToggle("Sombras nos controles", shadow) { shadow = it }
                        }
                        2 -> {
                            Text("Fontes do Android; a aparência pode variar conforme o celular.")
                            LemuroidPadTheme.fontOptions.forEach { (id, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = font == id, onClick = { font = id })
                                    TextButton(onClick = { font = id }) {
                                        Text(name + " · Aa", fontFamily = LemuroidPadTheme.fontFor(id))
                                    }
                                }
                            }
                            OutlinedButton(enabled = !importing, onClick = { importFont.launch(arrayOf("*/*")) }) {
                                Text(if (importing) "Importando…" else "Importar fonte TTF / OTF")
                            }
                            if (font.startsWith("FILE:")) Text("Fonte importada selecionada", fontFamily = LemuroidPadTheme.fontFor(font))
                            fontMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            SkinSlider("Tamanho das letras", labelScale, 0.6f..1.4f, "${(labelScale * 100).toInt()}%") { labelScale = it }
                            SkinToggle("Letras em negrito", bold) { bold = it }
                            Text("As setas continuam como ícones.")
                        }
                        else -> {
                            Text("Escolha um ponto de partida; depois ajuste cada detalhe.")
                            listOf("KL Azul", "Contorno portátil", "Retro", "Sonic", "Discreto").forEach { name ->
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !importing, onClick = {
                                    base = "NORMAL"; corners = 36f; border = 0f; opacity = 0.8f
                                    baseOpacity = 0.12f; shadow = true; pressScale = 0.94f
                                    fill = "#168FC4"; pressed = "#88DEFF"; text = "#FFFFFF"
                                    pressedText = "#083D55"; outline = "#FFFFFF"; baseColor = fill
                                    when (name) {
                                        "Contorno portátil" -> { base = "OUTLINE"; opacity = 0.2f; baseOpacity = 0f; border = 2f; shadow = false }
                                        "Retro" -> { base = "RETRO"; corners = 4f; fill = "#303B4E"; baseColor = fill; font = "MONO" }
                                        "Sonic" -> { fill = "#0758D8"; pressed = "#FFD342"; pressedText = "#123A75"; outline = "#FFD342"; border = 2f; baseColor = fill }
                                        "Discreto" -> { base = "MINIMAL"; opacity = 0.35f; baseOpacity = 0f; shadow = false }
                                    }
                                }) { Text(name) }
                            }
                        }
                    }
                    Text("A skin fica salva para este console e esta orientação.", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { close() }) { Text("Cancelar") }
                    Button(enabled = valid && !importing, onClick = { clickSound(); onSave(draft) }) { Text("Salvar skin") }
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
