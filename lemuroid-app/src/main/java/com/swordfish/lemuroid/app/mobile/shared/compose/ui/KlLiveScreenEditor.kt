package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.Variable
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings

@Composable
fun KlLiveScreenEditor(viewModel: BaseGameScreenViewModel) {
    val context = LocalContext.current
    val core = viewModel.klCoreName
    var view by remember { mutableStateOf<GLRetroView?>(null) }
    var original by remember { mutableStateOf(emptyArray<Variable>()) }
    var saved by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(emptyMap<String,String>()) }
    LaunchedEffect(Unit) {
        val gameView = viewModel.retroGameView.retroGameViewFlow()
        gameView.queueEvent {
            original = gameView.getVariables()
            view = gameView
        }
    }
    LaunchedEffect(view, draft) {
        val gameView = view ?: return@LaunchedEffect
        val options = KlPlaySettings.screenOptions(core, "CUSTOM", 2) +
            (if(core=="desmume") mapOf("desmume_screens_gap" to "0", "desmume_input_rotation" to "0") else emptyMap()) + draft
        gameView.queueEvent { gameView.updateVariables(*options.map { Variable(it.key,it.value) }.toTypedArray()) }
    }
    DisposableEffect(Unit) {
        onDispose {
            val gameView = view
            if (gameView != null && !saved) {
                val options = KlPlaySettings.coreOptions(context, core,
                    original.mapNotNull { v -> v.key?.let { k -> v.value?.let { com.swordfish.lemuroid.lib.core.CoreVariable(k,it) } } })
                gameView.queueEvent { gameView.updateVariables(*options.map { Variable(it.key,it.value) }.toTypedArray()) }
            }
        }
    }
    KlDualScreenEditor(if(core=="citra")"3DS" else "NDS",
        {viewModel.klEditingScreens.value=false},
        {saved=true;viewModel.klEditingScreens.value=false},live=true,onPreview={draft=it})
}
