package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.core.CoreVariable

object KlPlaySettings {
    const val CPU_3DS = "kl_3ds_cpu_scale"
    const val CLICK_SOUND = "kl_click_sound"
    const val SPEED = "fast_forward_speed"
    const val SIMPLE_FILTER = "simple_filter"
    const val NATIVE_RESOLUTION = "native_resolution"
    const val SHADER_CACHE = "citra_shader_cache"
    const val FULL_SCREEN = "kl_full_screen"
    const val NDS_LAYOUT = "kl_nds_layout"
    const val THREEDS_LAYOUT = "kl_3ds_layout"
    const val NDS_RATIO = "kl_nds_ratio"
    val layouts = listOf("DEFAULT" to "Padrão do núcleo", "LARGE" to "Superior grande + inferior pequena",
        "SIDE" to "Lado a lado", "STACK" to "Uma sobre a outra", "TOP" to "Somente superior", "BOTTOM" to "Somente inferior")
    fun preferences(context: Context) = context.getSharedPreferences("kl_play_settings", Context.MODE_PRIVATE)
    fun speed(context: Context) = preferences(context).getInt(SPEED, 2).coerceIn(2, 8)
    fun enabled(context: Context, key: String) = preferences(context).getBoolean(key, false)

    fun coreOptions(context: Context, core: String, original: List<CoreVariable>): List<CoreVariable> {
        val overrides = mutableMapOf<String, String>()
        val key = if (core == "citra") THREEDS_LAYOUT else NDS_LAYOUT
        val layout = preferences(context).getString(key, "DEFAULT") ?: "DEFAULT"
        overrides.putAll(screenOptions(core, layout, preferences(context).getInt(NDS_RATIO, 2)))
        if (core == "citra") {
            val cpu = preferences(context).getInt(CPU_3DS, 0)
            if (cpu in listOf(50, 75, 100, 125, 150, 200)) overrides["citra_cpu_scale"] = if (cpu == 100) "100% (Default)" else "${cpu}%"
            if (enabled(context, NATIVE_RESOLUTION)) overrides["citra_resolution_factor"] = "1x (Native)"
            if (enabled(context, SHADER_CACHE)) overrides["citra_use_hw_shader_cache"] = "enabled"
        }
        if (core == "ppsspp" && enabled(context, NATIVE_RESOLUTION)) {
            overrides["ppsspp_internal_resolution"] = "480x272"
        }
        if (com.swordfish.lemuroid.app.shared.multiplayer.KlLinkSession.active && core == "gpsp") {
            val mode = com.swordfish.lemuroid.app.shared.multiplayer.KlLinkSession.launch?.mode
            overrides["gpsp_serial"] = if (mode == "WIRELESS") "rfu" else "mul_poke"
            // No frame skipping while serial timing is active.
            overrides["gpsp_frameskip"] = "disabled"
        }
        return original.filterNot { it.key in overrides } + overrides.map { CoreVariable(it.key, it.value) }
    }

    fun screenOptions(core: String, layout: String, ratio: Int): Map<String, String> {
        if (layout == "DEFAULT") return emptyMap()
        return when (core) {
            "citra" -> mapOf("citra_layout_option" to when (layout) {
                "LARGE" -> "Large Screen, Small Screen"; "SIDE" -> "Side by Side"
                "TOP", "BOTTOM" -> "Single Screen Only"; else -> "Default Top-Bottom Screen"
            }, "citra_swap_screen" to if (layout == "BOTTOM") "enabled" else "disabled")
            "melonds" -> mapOf("melonds_screen_layout1" to when (layout) {
                "LARGE" -> "hybrid-top"; "SIDE" -> "left-right"; "TOP" -> "top"
                "BOTTOM" -> "bottom"; else -> "top-bottom"
            }, "melonds_number_of_screen_layouts" to "1", "melonds_hybrid_ratio" to ratio.coerceIn(2, 3).toString(), "melonds_hybrid_small_screen" to "one")
            "desmume" -> mapOf("desmume_screens_layout" to when (layout) {
                "LARGE" -> "hybrid/top"; "SIDE" -> "left/right"; "TOP" -> "top only"
                "BOTTOM" -> "bottom only"; else -> "top/bottom"
            }, "desmume_hybrid_layout_scale" to "disabled", "desmume_hybrid_showboth_screens" to "disabled")
            else -> emptyMap()
        }
    }
}
