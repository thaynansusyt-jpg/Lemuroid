package com.swordfish.lemuroid.app.shared.profile

/** KL usage medals, not game-memory achievements. Pure rules shared by storage and UI. */
object KlAchievements {
    data class Facts(val minutes: Long, val sessions: Long, val games: Long, val days: Long,
        val streak: Long, val notes: Long, val sonicMinutes: Long, val screenMinutes: Long,
        val sonicSessions: Long, val events: Set<String>) {
        fun value(metric: String): Long = when (metric) {
            "minutes" -> minutes; "sessions" -> sessions; "games" -> games; "days" -> days
            "streak" -> streak; "notes" -> notes; "sonicMinutes" -> sonicMinutes
            "screenMinutes" -> screenMinutes; "sonicSessions" -> sonicSessions
            else -> if (metric in events) 1 else 0
        }
    }
    data class Medal(val id: Int, val title: String, val requirement: String, val metric: String,
        val target: Long, val color: String) {
        val reward get() = when (id) {
            1 -> "Conjunto Sii Sonic + medalha + cor"
            100 -> "Conjunto Sii Super Sonic + medalha + cor"
            else -> "Medalha #$id + cor $color para o Sii"
        }
    }
    private fun color(id: Int): String {
        val hue = (id * 137.508) % 360 / 60
        val x = 0.82 * (1 - kotlin.math.abs(hue % 2 - 1))
        val rgb = when (hue.toInt()) {
            0 -> doubleArrayOf(0.82, x, 0.0); 1 -> doubleArrayOf(x, 0.82, 0.0)
            2 -> doubleArrayOf(0.0, 0.82, x); 3 -> doubleArrayOf(0.0, x, 0.82)
            4 -> doubleArrayOf(x, 0.0, 0.82); else -> doubleArrayOf(0.82, 0.0, x)
        }
        return "#" + rgb.joinToString("") { "%02X".format(((it + 0.15) * 255).toInt().coerceIn(0, 255)) }
    }
    val medals: List<Medal> = buildList {
        fun addMedal(id: Int, title: String, requirement: String, metric: String, target: Long) {
            add(Medal(id, title, requirement, metric, target, color(id)))
        }
        addMedal(1, "Primeiro anel", "Jogue por 1 minuto no KL.", "minutes", 1)
        var id = 2
        fun series(title: String, metric: String, unit: String, goals: List<Long>) {
            goals.forEachIndexed { index, goal ->
                if (id == 100) id++
                addMedal(id++, "$title ${index + 1}", "Alcance $goal $unit.", metric, goal)
            }
        }
        series("Tempo de aventura", "minutes", "minutos jogados", listOf(5,15,30,60,120,300,600,1200,3000,6000).map(Int::toLong))
        series("De volta ao jogo", "sessions", "sessões com jogo em primeiro plano", listOf(2,3,5,10,20,40,60,100,200,500).map(Int::toLong))
        series("Explorador da biblioteca", "games", "jogos diferentes no diário", listOf(2,3,4,5,6,8,10,15,20,30).map(Int::toLong))
        series("Calendário de aventuras", "days", "dias com ao menos 1 minuto de jogo", listOf(2,3,5,7,10,15,30,60,100,365).map(Int::toLong))
        series("Sequência de anéis", "streak", "dias de recorde de sequência", listOf(2,3,4,5,7,10,15,20,30,60).map(Int::toLong))
        series("Autor do diário", "notes", "dias com uma anotação salva", listOf(1,2,3,5,7,10,15,20,30,60).map(Int::toLong))
        series("Aventura azul", "sonicMinutes", "minutos jogados com a UI Sonic", listOf(1,5,15,30,60,120,300,600,1200,3000).map(Int::toLong))
        series("Horizonte ampliado", "screenMinutes", "minutos jogados com tela cheia KL", listOf(1,5,15,30,60,120,300,600,1200,3000).map(Int::toLong))
        series("Volta ao mundo azul", "sonicSessions", "sessões com a UI Sonic", listOf(1,2,3,5,10,20,40,60,100,200).map(Int::toLong))
        listOf("sonic_theme" to "Escolha a UI Sonic", "wiiu_theme" to "Escolha a UI Wii U",
            "sonic_skin" to "Escolha a skin Sonic nos controles", "fullscreen" to "Ative a tela cheia KL",
            "nds_layout" to "Personalize o layout de DS", "3ds_layout" to "Personalize o layout de 3DS",
            "profile_name" to "Salve um nome personalizado", "sii_edit" to "Salve uma personalização do Sii").forEach { (event, title) ->
            addMedal(id++, title, "$title.", event, 1)
        }
        check(id == 100)
        addMedal(100, "Super Sonic", "Conclua 99 outras conquistas KL. Esta será a 100ª e libera o Super Sonic.", "earned", 99)
        id = 101
        series("Maratona KL", "minutes", "minutos jogados", listOf(7200,9000,12000,15000,18000,24000,30000,36000,48000,60000).map(Int::toLong))
        series("Diário sem fronteiras", "notes", "dias com anotações salvas", listOf(75,90,100,120,150,180,200,250,300,365).map(Int::toLong))
        series("Coleção além do horizonte", "games", "jogos diferentes no diário", listOf(40,50,60,80,100,120,150,200).map(Int::toLong))
    }.sortedBy { it.id }

    fun unlock(facts: Facts, existing: Set<Int>): Set<Int> {
        val earned = existing.filterTo(mutableSetOf()) { id -> medals.any { it.id == id } }
        medals.filter { it.id != 100 && facts.value(it.metric) >= it.target }.forEach { earned.add(it.id) }
        if (1 in earned && earned.count { it != 100 } >= 99) earned.add(100)
        return earned
    }
    fun progress(medal: Medal, facts: Facts, earned: Set<Int>): Long =
        (if (medal.id == 100) earned.count { it != 100 }.toLong() else facts.value(medal.metric)).coerceIn(0, medal.target)
}
