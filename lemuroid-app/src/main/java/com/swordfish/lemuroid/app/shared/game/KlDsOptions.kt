package com.swordfish.lemuroid.app.shared.game

/** Values shared with the bundled melonDS and melonDS DS core option definitions. */
object KlDsOptions {
    val profiles = listOf("CORE" to "Padrão do núcleo", "BALANCED" to "Equilibrado", "FAST" to "Desempenho", "COMPATIBLE" to "Compatibilidade")
    fun performance(profile: String): Map<String, String> = when (profile) {
        "BALANCED" -> mapOf("melonds_jit_enable" to "enabled", "melonds_threaded_renderer" to "enabled")
        "FAST" -> mapOf("melonds_jit_enable" to "enabled", "melonds_threaded_renderer" to "enabled", "melonds_jit_block_size" to "32")
        "COMPATIBLE" -> mapOf("melonds_jit_enable" to "disabled", "melonds_threaded_renderer" to "disabled")
        else -> emptyMap()
    }
}
