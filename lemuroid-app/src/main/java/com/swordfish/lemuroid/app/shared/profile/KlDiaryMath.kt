package com.swordfish.lemuroid.app.shared.profile

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object KlDiaryMath {
    const val DAILY_GOAL_MS = 60_000L
    data class Portion(val date: String, val millis: Long)

    fun split(endWall: Long, elapsedMillis: Long, zone: ZoneId): List<Portion> {
        if (elapsedMillis <= 0) return emptyList()
        var cursor = endWall - elapsedMillis
        val portions = mutableListOf<Portion>()
        while (cursor < endWall) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val boundary = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val until = minOf(endWall, boundary)
            if (until <= cursor) break
            portions += Portion(date.toString(), until - cursor)
            cursor = until
        }
        return portions
    }

    fun streak(days: Map<String, Long>, today: LocalDate): Int {
        var day = if ((days[today.toString()] ?: 0L) >= DAILY_GOAL_MS) today else today.minusDays(1)
        var count = 0
        while ((days[day.toString()] ?: 0L) >= DAILY_GOAL_MS) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun bestStreak(days: Map<String, Long>): Int {
        var best = 0
        var count = 0
        var previous: LocalDate? = null
        days.filterValues { it >= DAILY_GOAL_MS }.keys.sorted().forEach {
            val current = LocalDate.parse(it)
            count = if (previous?.plusDays(1) == current) count + 1 else 1
            best = maxOf(best, count)
            previous = current
        }
        return best
    }
}
