package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.core.CoreVariable

object KlPlaySettings {
    const val SPEED = "fast_forward_speed"
    const val SIMPLE_FILTER = "simple_filter"
    const val NATIVE_RESOLUTION = "native_resolution"
    const val SHADER_CACHE = "citra_shader_cache"
    fun preferences(context: Context) = context.getSharedPreferences("kl_play_settings", Context.MODE_PRIVATE)
    fun speed(context: Context) = preferences(context).getInt(SPEED, 2).coerceIn(2, 8)
    fun enabled(context: Context, key: String) = preferences(context).getBoolean(key, false)

    fun coreOptions(context: Context, core: String, original: List<CoreVariable>): List<CoreVariable> {
        val overrides = mutableMapOf<String, String>()
        if (core == "citra") {
            if (enabled(context, NATIVE_RESOLUTION)) overrides["citra_resolution_factor"] = "1x (Native)"
            if (enabled(context, SHADER_CACHE)) overrides["citra_use_hw_shader_cache"] = "enabled"
        }
        if (core == "ppsspp" && enabled(context, NATIVE_RESOLUTION)) {
            overrides["ppsspp_internal_resolution"] = "480x272"
        }
        return original.filterNot { it.key in overrides } + overrides.map { CoreVariable(it.key, it.value) }
    }
}
