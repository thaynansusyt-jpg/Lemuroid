package com.swordfish.lemuroid.app.shared.profile

/** One shared event window. Installation, login and theme changes never restart it. */
object KlSegaEvent {
    const val START_MS = 1791245504000L // 2026-10-05 21:11:44 America/Sao_Paulo
    const val END_MS = 1792109504000L // 2026-10-15 21:11:44 America/Sao_Paulo
    const val DURATION_MS = 10L * 24 * 60 * 60 * 1000
    enum class Phase { UPCOMING, ACTIVE, ENDED }
    fun phase(now: Long) = when {
        now < START_MS -> Phase.UPCOMING
        now < END_MS -> Phase.ACTIVE
        else -> Phase.ENDED
    }
    fun remaining(now: Long): Long = END_MS - now.coerceIn(START_MS, END_MS)
    fun countdown(now: Long): String {
        val seconds = (remaining(now) + 999) / 1000
        return "%dd %02dh %02dm %02ds".format(java.util.Locale.ROOT,
            seconds / 86400, seconds / 3600 % 24, seconds / 60 % 60, seconds % 60)
    }
}
