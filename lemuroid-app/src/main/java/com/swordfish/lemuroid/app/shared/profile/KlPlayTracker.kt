package com.swordfish.lemuroid.app.shared.profile

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneId
import java.util.UUID

/** Counts foreground play after the first rendered frame; menus and background are excluded. */
class KlPlayTracker(private val context: Context, private val gameId: String, private val title: String) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val monitor = Any()
    private var ready = false
    private var resumed = false
    private var lastElapsed: Long? = null
    private var profileKey: String? = null
    private val session = UUID.randomUUID().toString()
    private val writes = mutableListOf<Job>()

    private val ticker = scope.launch {
        while (isActive) { delay(15_000); capture(false) }
    }
    fun markReady(key: String) = synchronized(monitor) {
        profileKey = key
        ready = true
        if (resumed && lastElapsed == null) lastElapsed = SystemClock.elapsedRealtime()
    }
    override fun onResume(owner: LifecycleOwner) = synchronized(monitor) {
        resumed = true
        if (ready && lastElapsed == null) lastElapsed = SystemClock.elapsedRealtime()
    }
    override fun onPause(owner: LifecycleOwner) { synchronized(monitor) { resumed = false }; capture(true) }
    private fun capture(stop: Boolean) = synchronized(monitor) {
        val before = lastElapsed ?: return@synchronized
        val now = SystemClock.elapsedRealtime()
        val key = profileKey ?: return@synchronized
        lastElapsed = if (stop) null else now
        val elapsed = (now-before).coerceAtLeast(0)
        if (elapsed == 0L) return@synchronized
        val wall = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        writes.removeAll { it.isCompleted }
        writes += scope.launch {
            runCatching { KlProfileStore.record(context, key, session, gameId, title, wall, elapsed, zone) }
                .onFailure { Timber.e(it, "Could not persist local play diary") }
            if (stop && key.startsWith("kl:")) {
                // Network work is separate from diary writes: leaving a game must not wait for the site.
                scope.launch {
                    runCatching { KlCloudAccount.backup(context) }
                        .onFailure { Timber.w("KL profile backup pending; local diary preserved") }
                }
            }
        }
    }
    suspend fun finish() {
        capture(true)
        val pending = synchronized(monitor) { writes.toList() }
        pending.joinAll()
        scope.cancel()
    }
    override fun onDestroy(owner: LifecycleOwner) {
        ticker.cancel()
        capture(true)
        scope.launch {
            synchronized(monitor) { writes.toList() }.joinAll()
            scope.cancel()
        }
    }
}
