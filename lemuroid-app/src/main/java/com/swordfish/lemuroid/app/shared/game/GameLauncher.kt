package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import java.io.File
import com.swordfish.lemuroid.app.shared.multiplayer.KlRoomService
import com.swordfish.lemuroid.app.shared.multiplayer.KlWifiRoom
import com.swordfish.lemuroid.app.shared.multiplayer.KlLinkSession
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameLauncher(
    private val coresSelection: CoresSelection,
    private val gameLaunchTaskHandler: GameLaunchTaskHandler,
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun launchGameAsync(activity: Activity, game: Game, loadSave: Boolean, leanback: Boolean): Boolean {
        val feedback = KlGameLaunch.coordinator
        val token = feedback.begin(game.title) ?: return false
        KlGameLaunch.lastTitle = game.title
        com.swordfish.lemuroid.lib.game.KlLaunchTrace.record(activity.applicationContext, "Pedido de abertura: ${game.fileName}")
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val inUse = withContext(Dispatchers.IO) { GameProcessLock.isHeldByAnotherProcess(activity.applicationContext) }
                check(!inUse) { "Há outro jogo aberto. Feche esse jogo pelo menu do KL antes de iniciar outro." }
                val room = KlRoomService.room.state.value
                val prefs = activity.getSharedPreferences("kl_room_ui", 0)
                val inRoom = room.phase in setOf(KlWifiRoom.Phase.HOSTING, KlWifiRoom.Phase.JOINED)
                val link = if (prefs.getBoolean("cable", false) && inRoom && game.systemId in setOf("gb", "gbc")) {
                    check(room.members.size == 2) { "O cabo GB/GBC precisa de exatamente 2 pessoas na sala." }
                    check(File(activity.applicationInfo.nativeLibraryDir, "libkl_gambatte_link.so").isFile) {
                        "O núcleo de cabo GB/GBC não está incluído nesta compilação. Saia da sala para jogar individualmente."
                    }
                    val host = room.phase == KlWifiRoom.Phase.HOSTING
                    KlLinkSession.Launch(if (host) 0 else 1,
                        if (host) "127.0.0.1" else room.addresses.first(), room.linkKey, "MULTIPAK")
                } else null
                if (game.systemId == "gamecube") {
                    feedback.progress(token, "Preparando o Dolphin pela primeira vez. Aguarde; não é necessário tocar novamente.")
                    withContext(Dispatchers.IO) { KlGameCube.prepare(activity.applicationContext) }
                }
                feedback.progress(token, "Abrindo o jogo…")
                com.swordfish.lemuroid.lib.game.KlLaunchTrace.record(activity.applicationContext, "Preparação concluída; escolhendo o núcleo")
                val system = GameSystem.findById(game.systemId)
                val coreConfig = if (link != null) {
                    com.swordfish.lemuroid.app.shared.multiplayer.KlLinkCoreConfig.resolve(system, link.mode)
                } else if (game.systemId == "nds" && com.swordfish.lemuroid.app.shared.multiplayer.KlDsWfc.enabled(activity)) {
                    check(com.swordfish.lemuroid.app.shared.multiplayer.KlDsWfc.available(activity)) {
                        "O núcleo DS online está ausente desta compilação."
                    }
                    withContext(Dispatchers.IO) {
                        com.swordfish.lemuroid.app.shared.multiplayer.KlDsWfc.preservePreviousStates(activity.applicationContext, game)
                    }
                    com.swordfish.lemuroid.app.shared.multiplayer.KlDsWfc.config(system)
                } else coresSelection.getCoreConfigForSystem(system)
                check(!activity.isFinishing && !activity.isDestroyed) { "A tela foi fechada durante a preparação. Abra o KL e tente novamente." }
                gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                com.swordfish.lemuroid.lib.game.KlLaunchTrace.record(activity.applicationContext, "Abrindo a tela do jogo")
                BaseGameActivity.launchGame(activity, coreConfig, game, loadSave && link == null, leanback, link)
                feedback.complete(token)
            } catch (error: CancellationException) {
                feedback.complete(token)
                throw error
            } catch (error: Exception) {
                timber.log.Timber.e(error, "Could not launch game")
                feedback.fail(token, error.message ?: "Não foi possível abrir o jogo. A etapa de preparação falhou.")
            }
        }
        return true
    }
}
