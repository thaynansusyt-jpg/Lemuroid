package com.swordfish.lemuroid.app.shared.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One launch at a time, with feedback surviving recomposition/activity recreation. */
class KlLaunchCoordinator {
    data class Status(val title: String, val message: String, val error: Boolean = false)
    private val mutableStatus = MutableStateFlow<Status?>(null)
    val status = mutableStatus.asStateFlow()
    private var active: Long? = null
    private var sequence = 0L

    @Synchronized fun begin(title: String): Long? {
        if (active != null) return null
        val token = ++sequence
        active = token
        mutableStatus.value = Status(title, "Preparando a abertura…")
        return token
    }
    @Synchronized fun progress(token: Long, message: String) {
        if (active == token) mutableStatus.value = mutableStatus.value?.copy(message = message)
    }
    @Synchronized fun complete(token: Long) {
        if (active == token) { active = null; mutableStatus.value = null }
    }
    @Synchronized fun fail(token: Long, message: String) {
        if (active == token) {
            active = null
            mutableStatus.value = mutableStatus.value?.copy(message = message, error = true)
        }
    }
    @Synchronized fun reject(title: String, message: String) {
        if (active == null) mutableStatus.value = Status(title, message, error = true)
    }
    @Synchronized fun dismissError() {
        if (active == null) mutableStatus.value = null
    }
}

object KlGameLaunch {
    val coordinator = KlLaunchCoordinator()
    var lastTitle = "Jogo"
}
