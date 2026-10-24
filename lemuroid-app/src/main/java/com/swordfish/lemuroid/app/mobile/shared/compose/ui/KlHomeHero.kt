package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun KlHomeHero(recent: Game?, onResume: (Game) -> Unit, onOpenFolder: () -> Unit, scanning: Boolean) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF152C3D), Color(0xFF244B59)))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("KL PLAY / SUA PRÓXIMA AVENTURA", color = Color(0xFFFFBE85), style = MaterialTheme.typography.labelLarge)
            Text(if (recent == null) "Tudo pronto\npara jogar." else "De volta\nao seu mundo.", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(recent?.title ?: "Seus clássicos, suas histórias e seu Sii em um só lugar.", color = Color(0xFFDBEBEF), style = MaterialTheme.typography.titleMedium)
            Button(enabled = recent != null || !scanning, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFBE85), contentColor = Color(0xFF302211)), onClick = { if (recent != null) onResume(recent) else onOpenFolder() }) {
                Text(if (recent != null) "Continuar aventura" else if (scanning) "Procurando jogos…" else "Escolher pasta de jogos")
            }
        }
    }
}
