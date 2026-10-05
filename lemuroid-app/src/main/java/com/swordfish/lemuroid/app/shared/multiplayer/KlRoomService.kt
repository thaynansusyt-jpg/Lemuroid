package com.swordfish.lemuroid.app.shared.multiplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Owns the LAN lobby independently of navigation and the game process. */
class KlRoomService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wifiLock: WifiManager.WifiLock? = null
    private var cpuLock: PowerManager.WakeLock? = null
    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Salas Wi-Fi KL", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        ServiceCompat.startForeground(this, ID, notification("Preparando sala…"),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0)
        // Keep networking alive when switching tabs or opening a game.
        if (wifiLock == null) {
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "KL:room").apply { setReferenceCounted(false); acquire() }
            val power = getSystemService(Context.POWER_SERVICE) as PowerManager
            cpuLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KL:room").apply { setReferenceCounted(false); acquire() }
            scope.launch {
                room.state.collect { state ->
                    if (state.phase == KlWifiRoom.Phase.ERROR) {
                        stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java)?.notify(ID, notification(state.message.ifBlank { "Preparando sala…" }))
                    }
                }
            }
        }
        val name = intent?.getStringExtra("name").orEmpty()
        if (intent?.action == HOST) room.host(name)
        if (intent?.action == JOIN) room.join(intent.getStringExtra("ip").orEmpty(), name)
        return START_NOT_STICKY
    }

    private fun notification(message: String) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_menu_controls)
        .setContentTitle("KL • Multiplayer Wi-Fi")
        .setContentText(message)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).putExtra("kl_open_multiplayer", true), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .addAction(0, "Sair da sala", PendingIntent.getService(this, 1, Intent(this, KlRoomService::class.java).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .build()

    override fun onDestroy() {
        val failed = room.state.value.phase == KlWifiRoom.Phase.ERROR
        if (!failed) room.leave()
        wifiLock?.let { if (it.isHeld) it.release() }
        cpuLock?.let { if (it.isHeld) it.release() }
        scope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        val room = KlWifiRoom()
        private const val CHANNEL = "kl_wifi_room"
        private const val ID = 7303
        private const val HOST = "kl.room.HOST"
        private const val JOIN = "kl.room.JOIN"
        private const val STOP = "kl.room.STOP"
        fun start(context: Context, host: Boolean, name: String, ip: String = "") {
            ContextCompat.startForegroundService(context, Intent(context, KlRoomService::class.java)
                .setAction(if (host) HOST else JOIN).putExtra("name", name).putExtra("ip", ip))
        }
        fun leave(context: Context) {
            room.leave()
            context.stopService(Intent(context, KlRoomService::class.java))
        }
    }
}
