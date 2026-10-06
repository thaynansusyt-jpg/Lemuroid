package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings

/** Android's click effect follows the device's sound-effects and volume settings. */
@Composable
fun rememberKlMenuClick(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return {
        if (KlPlaySettings.preferences(context).getBoolean(KlPlaySettings.CLICK_SOUND, true)) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}
