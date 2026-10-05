package com.swordfish.lemuroid.app.mobile.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.profile.KlProfileStore

private fun ink(value: String) = Color(android.graphics.Color.parseColor(value))

@Composable
fun KlSii(sii: KlProfileStore.Sii, modifier: Modifier = Modifier) {
    val logo = ImageBitmap.imageResource(R.drawable.kl_play_logo)
    Canvas(modifier.background(Color(0xFFEAF7FF))) {
        val u = minOf(size.width / 160f, size.height / 210f)
        val x = size.width / 2
        fun p(dx: Float, y: Float) = Offset(x + dx*u, y*u)
        val color = ink(sii.ink)
        val sonic = sii.clothes in setOf("sonic", "super_sonic")
        val shirt = if (sonic) Color(if (sii.clothes == "super_sonic") 0xFFFFD342 else 0xFF0758D8) else ink(sii.shirt)
        fun line(a: Offset, b: Offset, c: Color = color, w: Float = 7f) = drawLine(c, a, b, w*u, StrokeCap.Round)
        if (sonic) {
            val spikes = Path().apply {
                moveTo(x - 25*u, 53*u); lineTo(x - 40*u, 45*u); lineTo(x - 24*u, 35*u)
                lineTo(x - 35*u, 17*u); lineTo(x - 9*u, 27*u); lineTo(x, 6*u)
                lineTo(x + 10*u, 27*u); lineTo(x + 35*u, 17*u); lineTo(x + 24*u, 35*u)
                lineTo(x + 40*u, 45*u); lineTo(x + 25*u, 53*u); close()
            }
            drawPath(spikes, shirt)
            drawCircle(shirt, 28*u, p(0f, 47f))
        }
        if (sii.clothes == "hoodie") drawCircle(shirt, 29*u, p(0f, 47f))
        drawCircle(Color(0xFFFFE6BE), 22*u, p(0f, 47f))
        drawCircle(color, 22*u, p(0f, 47f), style = androidx.compose.ui.graphics.drawscope.Stroke(4*u))
        val human = sii.clothes == "sii_plus"
        line(p(0f, 70f), p(0f, 142f))
        if (human) {
            drawArc(color, 180f, 180f, true, topLeft = p(-23f, 24f), size = androidx.compose.ui.geometry.Size(46*u, 34*u))
            line(p(-16f, 85f), p(-42f, 126f), Color(0xFFFFE6BE), 13f)
            line(p(16f, 85f), p(42f, 126f), Color(0xFFFFE6BE), 13f)
            line(p(-11f, 131f), p(-18f, 183f), color, 18f)
            line(p(11f, 131f), p(18f, 183f), color, 18f)
            line(p(-18f, 184f), p(-26f, 184f), shirt, 12f)
            line(p(18f, 184f), p(26f, 184f), shirt, 12f)
        } else {
            line(p(-16f, 85f), p(-46f, 129f))
            line(p(16f, 85f), p(46f, 129f))
            line(p(0f, 137f), p(-29f, 188f))
            line(p(0f, 137f), p(29f, 188f))
        }
        val torso = Path().apply { moveTo(x-22*u, 78*u); lineTo(x+22*u, 78*u); lineTo(x+25*u, 133*u); lineTo(x-25*u, 133*u); close() }
        drawPath(torso, shirt)
        if (sii.clothes == "sport") { line(p(-18f, 91f), p(18f, 91f), Color.White, 5f); line(p(-18f, 118f), p(18f, 118f), Color.White, 5f) }
        if (sii.clothes == "hoodie") { line(p(-6f, 82f), p(-6f, 98f), Color.White, 2f); line(p(6f, 82f), p(6f, 98f), Color.White, 2f) }
        if (sii.clothes == "kl" || human) drawImage(logo, dstOffset = IntOffset((x-14*u).toInt(), (94*u).toInt()), dstSize = IntSize((28*u).toInt().coerceAtLeast(1), (28*u).toInt().coerceAtLeast(1)))
        if (sonic) {
            drawCircle(Color.White, 8*u, p(-46f, 129f)); drawCircle(Color.White, 8*u, p(46f, 129f))
            line(p(-29f, 188f), p(-38f, 188f), Color(0xFFE83336), 12f)
            line(p(29f, 188f), p(38f, 188f), Color(0xFFE83336), 12f)
            drawCircle(Color(0xFFFFD342), 11*u, p(0f, 109f), style = androidx.compose.ui.graphics.drawscope.Stroke(4*u))
        }
        if (sii.badge > 0) {
            drawCircle(Color(0xFFFFD342), 10*u, p(24f, 128f))
            val label = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 8*u; textAlign = android.graphics.Paint.Align.CENTER; setColor(android.graphics.Color.rgb(16, 35, 60)); isFakeBoldText = true }
            drawContext.canvas.nativeCanvas.drawText(sii.badge.toString(), x+24*u, 131*u, label)
        }
        if (sii.face == "cool") { line(p(-14f, 43f), p(14f, 43f), color, 8f) }
        else { drawCircle(color, 2*u, p(-8f, 42f)); drawCircle(color, 2*u, p(8f, 42f)) }
        if (sii.face == "happy") { line(p(-7f, 53f), p(0f, 57f), color, 2f); line(p(0f, 57f), p(7f, 53f), color, 2f) }
        else line(p(-6f, 55f), p(6f, 55f), color, 2f)
    }
}

@Composable
fun KlSiiEditor(initial: KlProfileStore.Sii, siiPlus: Boolean = false, onClose: () -> Unit, onSave: (KlProfileStore.Sii) -> Unit, earned: Set<Int> = emptySet()) {
    var sii by remember { mutableStateOf(initial) }
    var customInk by remember { mutableStateOf(initial.ink) }
    var customShirt by remember { mutableStateOf(initial.shirt) }
    AlertDialog(onDismissRequest = onClose, properties = DialogProperties(dismissOnClickOutside = false),
        title = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Seu Sii"); TextButton(onClick = onClose) { Text("✕") } } },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KlSii(sii, Modifier.fillMaxWidth().height(180.dp))
            Text("Camiseta KL • edição de lançamento")
            listOf("kl" to "Camiseta KL", "tee" to "Camiseta simples", "hoodie" to "Moletom", "sport" to "Esportiva").forEach { (key, label) ->
                TextButton(onClick = { sii = sii.copy(clothes = key) }) { Text((if (sii.clothes == key) "✓ " else "") + label) }
            }
            Text("Conjuntos")
            TextButton(onClick = { sii = sii.copy(clothes = "sii_plus") }, enabled = siiPlus) {
                Text(if (siiPlus) "Sii+ • conjunto humano KL" else "Sii+ • entre com uma conta KL para desbloquear")
            }
            listOf(1 to ("sonic" to "Sii Sonic"), 100 to ("super_sonic" to "Sii Super Sonic")).forEach { (medal, costume) ->
                TextButton(onClick = { sii = sii.copy(clothes = costume.first) }, enabled = medal in earned) {
                    Text(if (medal in earned) costume.second else "${costume.second} • desbloqueie a conquista #$medal")
                }
            }
            if (sii.badge > 0) TextButton(onClick = { sii = sii.copy(badge = 0) }) { Text("Remover medalha do peito") }
            Text(if (sii.clothes == "sii_plus") "Cor do cabelo e da calça" else "Cor do palito")
            ColorChoices { sii = sii.copy(ink = it); customInk = it }
            OutlinedTextField(customInk, { customInk = it.take(7); if (it.matches(Regex("#[0-9a-fA-F]{6}"))) sii = sii.copy(ink = it) }, label = { Text("Cor personalizada #RRGGBB") }, singleLine = true)
            Text("Cor da roupa")
            ColorChoices { sii = sii.copy(shirt = it); customShirt = it }
            OutlinedTextField(customShirt, { customShirt = it.take(7); if (it.matches(Regex("#[0-9a-fA-F]{6}"))) sii = sii.copy(shirt = it) }, label = { Text("Cor personalizada #RRGGBB") }, singleLine = true)
            Row { listOf("happy" to "Feliz", "cool" to "Óculos", "calm" to "Calmo").forEach { (key, label) -> TextButton(onClick = { sii = sii.copy(face = key) }) { Text(label) } } }
        } }, confirmButton = { Button(onClick = { onSave(sii) }) { Text("Salvar") } })
}

@Composable
private fun ColorChoices(onSelect: (String) -> Unit) {
    val colors = listOf("#243447", "#0789FF", "#FF7043", "#9B51E0", "#2DAD68", "#FFD740", "#FFFFFF", "#FF70BA")
    Column { colors.chunked(4).forEach { row -> Row { row.forEach { color ->
        TextButton(onClick = { onSelect(color) }, modifier = Modifier.weight(1f)) { Text("●", color = ink(color), style = MaterialTheme.typography.headlineMedium) }
    } } } }
}
