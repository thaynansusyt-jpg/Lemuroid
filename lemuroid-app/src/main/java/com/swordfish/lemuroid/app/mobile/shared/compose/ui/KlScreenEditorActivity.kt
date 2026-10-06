package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Settings preview uses an entire Activity, never a popup. Rotate to edit the other orientation. */
class KlScreenEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { KlDualScreenEditor(intent.getStringExtra("system") ?: "NDS", {finish()}, {finish()}) } }
    }
}
