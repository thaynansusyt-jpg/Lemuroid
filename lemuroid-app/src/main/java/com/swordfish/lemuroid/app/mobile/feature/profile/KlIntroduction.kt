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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import kotlinx.coroutines.delay

@Composable
fun KlIntroduction(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) { delay(2400); finish() }
    BackHandler { finish() }
    val transition = rememberInfiniteTransition(label = "KL intro")
    val scale by transition.animateFloat(0.94f, 1.02f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "logo")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAFBFF), Color(0xFFBCEBFF), Color(0xFFEDF7FF))))) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.kl_play_logo), "Logo KL Play", Modifier.size(180.dp).graphicsLayer { scaleX = scale; scaleY = scale })
            Spacer(Modifier.height(24.dp))
            Text("KL PLAY", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF153855))
            Text("o Port Definitivo do Lemuroid", textAlign = TextAlign.Center, color = Color(0xFF153855))
            Spacer(Modifier.height(32.dp))
            Button(onClick = { finish() }) { Text("Entrar") }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("© Poké Laranja/Senhor Laranja & Lemuroid, todos os direitos reservados", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = Color(0xFF153855))
            Text("Fork independente • GPL-3.0 e licenças dos componentes preservadas", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = Color(0xFF153855))
        }
    }
}
