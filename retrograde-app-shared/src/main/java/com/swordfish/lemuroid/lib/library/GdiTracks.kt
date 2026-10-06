package com.swordfish.lemuroid.lib.library

/** The descriptor and every track must stay together. Only sibling files are accepted. */
object GdiTracks {
    private val track = Regex("""^\s*\d+\s+\d+\s+\d+\s+\d+\s+(?:"([^"\r\n]+)"|(\S+))\s+\d+\s*$""")

    fun parse(lines: List<String>): List<String>? {
        val rows = lines.map { it.trim() }.filter { it.isNotEmpty() }
        val count = rows.firstOrNull()?.toIntOrNull() ?: return null
        if (count !in 1..99 || rows.size != count + 1) return null
        val names = rows.drop(1).map { row ->
            val match = track.matchEntire(row) ?: return null
            val name = match.groupValues[1].ifEmpty { match.groupValues[2] }
            if (name == "." || name == ".." || name.any { it == '/' || it == '\\' || it == '\u0000' }) return null
            name
        }
        return names
    }
}
