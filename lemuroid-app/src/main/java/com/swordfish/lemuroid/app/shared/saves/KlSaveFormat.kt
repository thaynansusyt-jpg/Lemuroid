package com.swordfish.lemuroid.app.shared.saves

/** Cartridge battery saves only. Quick states are intentionally rejected. */
object KlSaveFormat {
    const val MAX_BYTES = 8 * 1024 * 1024 + 4096
    private val footer = "|<--Snip above here to create a raw sav by excluding this DeSmuME savedata footer:".toByteArray(Charsets.US_ASCII)
    fun decode(name: String, data: ByteArray, system: String): ByteArray {
        require(name.substringAfterLast('.', "").lowercase() in setOf("sav", "srm", "dsv")) {
            "Escolha um save .sav, .srm ou .dsv feito dentro do jogo."
        }
        require(data.isNotEmpty() && data.size <= MAX_BYTES) { "Save vazio ou grande demais." }
        val raw = if (name.endsWith(".dsv", true)) {
            require(system == "nds") { "DSV é um formato de Nintendo DS." }
            // A footer must be near EOF and follow a valid cartridge save size.
            // Never truncate on a string found inside actual game data.
            val start = (data.size - 4096).coerceAtLeast(0)
            val index = (start..(data.size - footer.size)).firstOrNull { i ->
                validSize(i, system) && footer.indices.all { data[i + it] == footer[it] }
            }
            if (index == null) data else data.copyOf(index)
        } else data
        require(validSize(raw.size, system)) { "Tamanho de save não reconhecido. Exporte o save de bateria no emulador de origem." }
        return raw
    }
    private fun validSize(size: Int, system: String): Boolean {
        val minimum = if (system == "nds") 512 else 128
        return size in minimum..(8 * 1024 * 1024) && size and (size - 1) == 0
    }
}
