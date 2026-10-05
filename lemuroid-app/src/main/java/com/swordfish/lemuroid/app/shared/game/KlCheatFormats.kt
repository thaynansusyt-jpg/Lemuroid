package com.swordfish.lemuroid.app.shared.game

import java.util.Locale

/** Validation and encoding must match the selected console, not only the UI. */
object KlCheatFormats {
    fun supports(systemId: String) = systemId in setOf("gba", "gb", "gbc", "nds", "n64", "3ds")

    fun hint(systemId: String): String = when (systemId) {
        "gb", "gbc" -> "GameShark: 8 dígitos. Game Genie: XXX-XXX ou XXX-XXX-XXX. Use um tipo por cheat."
        "nds" -> "Action Replay de DS: 8 dígitos + 8 dígitos por linha."
        "n64" -> "GameShark de Nintendo 64: 8 dígitos + 4 dígitos por linha."
        "3ds" -> "Códigos Citra/Gateway: 8 dígitos + 8 dígitos por linha. Feche e abra o jogo para aplicar."
        else -> "GBA: GameShark, Action Replay ou CodeBreaker; 8 dígitos + 4 ou 8 dígitos por linha."
    }

    fun normalize(input: String, systemId: String): String {
        require(supports(systemId)) { "Cheats ainda não disponíveis para este console." }
        require(input.length <= 8192) { "Use até 8192 caracteres por cheat." }
        val lines = input.replace("\r", "").split('\n', '+', ';').map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.isNotEmpty()) { "Cole o código do cheat." }
        require(lines.size <= 128) { "Use até 128 linhas por cheat." }
        val normalized = lines.mapIndexed { index, line ->
            val compact = line.replace(Regex("\\s+"), "").uppercase(Locale.ROOT)
            val message = "Confira a linha ${index + 1}. " + hint(systemId)
            when (systemId) {
                "gb", "gbc" -> {
                    require(compact.matches(Regex("[0-9A-F]{8}|[0-9A-F]{6}|[0-9A-F]{9}|[0-9A-F]{3}-[0-9A-F]{3}(-[0-9A-F]{3})?"))) { message }
                    if ('-' !in compact && compact.length != 8) compact.chunked(3).joinToString("-") else compact
                }
                else -> {
                    val lengths = when (systemId) { "n64" -> setOf(12); "gba" -> setOf(12, 16); else -> setOf(16) }
                    require(compact.length in lengths && compact.matches(Regex("[0-9A-F]+"))) { message }
                    compact.take(8) + " " + compact.drop(8)
                }
            }
        }
        if (systemId == "gb" || systemId == "gbc") {
            require(normalized.map { '-' in it }.distinct().size == 1) {
                "Separe GameShark e Game Genie em cheats diferentes."
            }
        }
        return normalized.joinToString("\n")
    }

    fun coreCodes(systemId: String, codes: List<String>): List<String> {
        val normalized = codes.map { normalize(it, systemId) }
        return when (systemId) {
            // Older Gambatte builds replace each cheat family on each call.
            "gb", "gbc" -> normalized.groupBy { '-' in it }.values.map { it.joinToString("+").replace("\n", "+") }
            "nds" -> normalized.map { it.replace("\n", " ") }
            "3ds" -> emptyList() // Citra uses its native title-ID file, not retro_cheat_set.
            else -> normalized.map { it.replace("\n", "+") }
        }
    }
}
