package com.swordfish.lemuroid.app.mobile.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import kotlinx.coroutines.delay

@Composable
fun KlIntroduction(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    var finished by remember { mutableStateOf(false) }
    fun enter() { if (!finished) { finished = true; finish() } }
    LaunchedEffect(Unit) { delay(2800); enter() }
    BackHandler { enter() }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val reveal by animateFloatAsState(if (revealed) 1f else 0f, tween(650), label = "KL reveal")
    val progress by animateFloatAsState(if (revealed) 1f else 0f, tween(2800), label = "KL progress")
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.surface, colors.primaryContainer, colors.surface)))) {
        val compact = maxHeight < 500.dp
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
                    modifier = Modifier.graphicsLayer { alpha = reveal; translationY = (1f - reveal) * 24.dp.toPx() }) {
                    Image(painterResource(R.drawable.kl_play_logo), "Logo KL Play",
                        Modifier.size(if (compact) 88.dp else 164.dp).graphicsLayer { scaleX = 0.92f + reveal * 0.08f; scaleY = scaleX })
                    Text("KL PLAY", style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
                    Text("Seu espaço para jogar, do seu jeito.", textAlign = TextAlign.Center, color = colors.onSurfaceVariant)
                    Button(onClick = { enter() }) { Text("Entrar") }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(160.dp), color = colors.primary)
                }
            }
            Text("Um fork independente do Lemuroid", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            Text("Poké Laranja / Senhor Laranja • Créditos ao Lemuroid e aos autores dos núcleos\nGPL-3.0 e licenças dos componentes preservadas",
                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = colors.onSurfaceVariant)
        }
    }
}
