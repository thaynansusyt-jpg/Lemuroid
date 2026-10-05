package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import java.io.File
import android.widget.Toast
import com.swordfish.lemuroid.app.shared.multiplayer.KlRoomService
import com.swordfish.lemuroid.app.shared.multiplayer.KlWifiRoom
import com.swordfish.lemuroid.app.shared.multiplayer.KlLinkSession
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GameLauncher(
    private val coresSelection: CoresSelection,
    private val gameLaunchTaskHandler: GameLaunchTaskHandler,
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun launchGameAsync(
        activity: Activity,
        game: Game,
        loadSave: Boolean,
        leanback: Boolean,
    ): Boolean {
        if (GameProcessLock.isHeldByAnotherProcess(activity.applicationContext)) {
            activity.displayToast(R.string.game_process_another_game_running)
            return false
        }

        val room = KlRoomService.room.state.value
        val cable = activity.getSharedPreferences("kl_room_ui", 0).getBoolean("cable", false)
        val inRoom = room.phase in setOf(KlWifiRoom.Phase.HOSTING, KlWifiRoom.Phase.JOINED)
        val link = if (cable && inRoom && game.systemId == "gba") {
            if (room.members.size != 2) {
                Toast.makeText(activity, "O cabo GBA precisa de exatamente 2 pessoas na sala.", Toast.LENGTH_LONG).show()
                return false
            }
            if (!File(activity.applicationInfo.nativeLibraryDir, "libkl_mgba_link.so").isFile) {
                Toast.makeText(activity, "Este primeiro teste do cabo GBA precisa de Android de 64 bits.", Toast.LENGTH_LONG).show()
                return false
            }
            KlLinkSession.Launch(
                if (room.phase == KlWifiRoom.Phase.HOSTING) 0 else 1,
                if (room.phase == KlWifiRoom.Phase.HOSTING) "127.0.0.1" else room.addresses.first(),
                room.linkKey,
            )
        } else null

        GlobalScope.launch {
            val system = GameSystem.findById(game.systemId)
            val coreConfig = if (link != null) {
                system.systemCoreConfigs.first { it.coreID == CoreID.MGBA }.copy(statesSupported = false)
            } else coresSelection.getCoreConfigForSystem(system)
            gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
            BaseGameActivity.launchGame(activity, coreConfig, game, loadSave && link == null, leanback, link)
        }

        return true
    }
}
