import com.swordfish.lemuroid.app.shared.profile.KlSegaEvent as Event
fun main() {
    check(Event.END_MS - Event.START_MS == Event.DURATION_MS)
    check(Event.remaining(Long.MIN_VALUE) == Event.DURATION_MS)
    check(Event.phase(Event.START_MS - 1) == Event.Phase.UPCOMING)
    check(Event.phase(Event.START_MS) == Event.Phase.ACTIVE)
    check(Event.countdown(Event.START_MS) == "10d 00h 00m 00s")
    check(Event.countdown(Event.END_MS - 1) == "0d 00h 00m 01s")
    check(Event.phase(Event.END_MS) == Event.Phase.ENDED)
    check(Event.remaining(Long.MAX_VALUE) == 0L)
    check(Event.countdown(Event.END_MS) == "0d 00h 00m 00s")
    val start = java.time.Instant.ofEpochMilli(Event.START_MS).atZone(java.time.ZoneId.of("America/Sao_Paulo"))
    val end = java.time.Instant.ofEpochMilli(Event.END_MS).atZone(java.time.ZoneId.of("America/Sao_Paulo"))
    check(start.toString().startsWith("2026-10-05T21:11:44-03:00"))
    check(end.toString().startsWith("2026-10-15T21:11:44-03:00"))
    println("Event: 10 days, timezone, boundaries and extreme clocks passed")
}
