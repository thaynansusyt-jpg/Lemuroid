import com.swordfish.lemuroid.app.shared.profile.KlDiaryMath as Diary
import java.time.*
fun main() {
    val utc = ZoneId.of("UTC")
    fun end(value: String) = Instant.parse(value).toEpochMilli()
    check(Diary.split(end("2026-10-06T00:00:30Z"), 60_000, utc) == listOf(Diary.Portion("2026-10-05", 30_000), Diary.Portion("2026-10-06", 30_000)))
    check(Diary.split(end("2026-10-05T12:00:00Z"), 0, utc).isEmpty())
    check(Diary.split(end("2026-10-05T12:00:00Z"), 15_000, utc).single().millis == 15_000L)
    val ny = ZoneId.of("America/New_York")
    val dstStart = LocalDate.of(2026,3,8).atStartOfDay(ny).toInstant().toEpochMilli()
    val dstEnd = LocalDate.of(2026,3,9).atStartOfDay(ny).toInstant().toEpochMilli()
    check(dstEnd-dstStart == 23*3600_000L)
    check(Diary.split(dstEnd, dstEnd-dstStart, ny) == listOf(Diary.Portion("2026-03-08",23*3600_000L)))
    val today = LocalDate.of(2026,10,5)
    val days = mapOf("2026-10-03" to 60_000L, "2026-10-04" to 90_000L, "2026-10-05" to 59_999L)
    check(Diary.streak(days,today) == 2)
    check(Diary.streak(days + ("2026-10-05" to 60_000L),today) == 3)
    check(Diary.streak(days - "2026-10-04",today) == 0)
    check(Diary.bestStreak(days + ("2026-10-01" to 999_999L)) == 2)
    println("Diary: midnight, DST, zero time, daily goal, grace day and broken streak passed.")
}
