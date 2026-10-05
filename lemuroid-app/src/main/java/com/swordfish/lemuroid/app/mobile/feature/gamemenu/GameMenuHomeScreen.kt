package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import com.swordfish.lemuroid.app.shared.multiplayer.KlLinkSession
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.KlCheats
import com.swordfish.lemuroid.app.shared.game.KlCheatFormats
import com.swordfish.lemuroid.app.shared.game.KlCitraCheats
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.alorma.compose.settings.storage.memory.rememberMemoryBooleanSettingState
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.tilt.TiltConfigurationMenuEntry
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import kotlin.reflect.KFunction1

@Composable
fun GameMenuHomeScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    var showCheats by rememberSaveable { mutableStateOf(false) }
    if (showCheats) {
        KlCheatsDialog(game = gameMenuRequest.game, onClose = { showCheats = false })
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (KlLinkSession.active) Text("Cabo Wi-Fi experimental: mantenha os dois jogos abertos. Cheats, estados e aceleração ficam desativados nesta sessão.")
        val cheatCoreSupported = when (gameMenuRequest.game.systemId) {
            "gba" -> gameMenuRequest.coreConfig.coreID.coreName == "mgba"
            "gb", "gbc" -> gameMenuRequest.coreConfig.coreID.coreName == "gambatte"
            "nds" -> gameMenuRequest.coreConfig.coreID.coreName in setOf("melonds", "desmume")
            "n64" -> gameMenuRequest.coreConfig.coreID.coreName == "mupen64plus_next_gles3"
            "3ds" -> gameMenuRequest.coreConfig.coreID.coreName == "citra"
            else -> false
        }
        if (cheatCoreSupported && !KlLinkSession.active) {
            LemuroidSettingsMenuLink(
                title = { Text("Cheats KL") },
                icon = {
                    Icon(painterResource(R.drawable.ic_menu_settings), contentDescription = "Cheats KL")
                },
                onClick = { showCheats = true },
            )
        }
        if (gameMenuRequest.coreConfig.statesSupported) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_save)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_save),
                        contentDescription = stringResource(id = R.string.game_menu_save),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.SAVE) },
            )

            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_load)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_load),
                        contentDescription = stringResource(id = R.string.game_menu_load),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.LOAD) },
            )
        }

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_quit)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_quit),
                    contentDescription = stringResource(id = R.string.game_menu_quit),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_QUIT, true) }
            },
        )

        if (!KlLinkSession.active) LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_restart)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_restart),
                    contentDescription = stringResource(id = R.string.game_menu_restart),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_RESET, true) }
            },
        )

        LemuroidSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.game_menu_mute_audio)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_mute),
                    contentDescription = stringResource(id = R.string.game_menu_mute_audio),
                )
            },
            state = rememberMemoryBooleanSettingState(!gameMenuRequest.audioEnabled),
            onCheckedChange = {
                onResult { putExtra(GameMenuContract.RESULT_ENABLE_AUDIO, !it) }
            },
        )

        if (gameMenuRequest.fastForwardSupported) {
            com.swordfish.lemuroid.app.mobile.feature.settings.general.KlFastForwardControl()
            LemuroidSettingsSwitch(
                title = { Text(text = stringResource(id = R.string.game_menu_fast_forward)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_fast_forward),
                        contentDescription = stringResource(id = R.string.game_menu_fast_forward),
                    )
                },
                state = rememberMemoryBooleanSettingState(gameMenuRequest.fastForwardEnabled),
                onCheckedChange = {
                    onResult { putExtra(GameMenuContract.RESULT_ENABLE_FAST_FORWARD, it) }
                },
            )
        }

        if (gameMenuRequest.numDisks > 1) {
            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_change_disk_button)) },
                items = (1..gameMenuRequest.numDisks).map { stringResource(R.string.game_menu_change_disk_disk, it) },
                useSelectedValueAsSubtitle = false,
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_disk),
                        contentDescription = stringResource(id = R.string.game_menu_change_disk_button),
                    )
                },
                state = rememberMemoryIntSettingState(gameMenuRequest.currentDisk),
                onItemSelected = { index, _ ->
                    onResult { putExtra(GameMenuContract.RESULT_CHANGE_DISK, index) }
                },
            )
        }

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_edit_touch_controls)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_controls),
                    contentDescription = stringResource(id = R.string.game_menu_edit_touch_controls),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true) }
            },
        )

        if (gameMenuRequest.advancedCoreOptions.isNotEmpty() || gameMenuRequest.coreOptions.isNotEmpty()) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_settings)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_settings),
                        contentDescription = stringResource(id = R.string.game_menu_settings),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.OPTIONS) },
            )
        }

        if (gameMenuRequest.allTiltConfigurations.isNotEmpty()) {
            val tiltConfigurationEntries =
                gameMenuRequest.allTiltConfigurations
                    .map { TiltConfigurationMenuEntry.fromTiltConfiguration(it) }

            val selectedIndex =
                gameMenuRequest.allTiltConfigurations
                    .indexOf(gameMenuRequest.currentTiltConfiguration)

            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_tilt_sensor)) },
                items = tiltConfigurationEntries.map { stringResource(it.descriptionId) },
                useSelectedValueAsSubtitle = false,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = stringResource(id = R.string.game_menu_tilt_sensor),
                    )
                },
                state = rememberMemoryIntSettingState(selectedIndex),
                onItemSelected = { index, _ ->
                    onResult {
                        putExtra(
                            GameMenuContract.RESULT_CHANGE_TILT_CONFIG,
                            tiltConfigurationEntries[index].configuration,
                        )
                    }
                },
            )
        }
    }
}


@Composable
private fun KlCheatsDialog(game: Game, onClose: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val loaded = remember(game.fileUri) { runCatching { KlCheats.read(context, game) } }
    var cheats by remember(game.fileUri) { mutableStateOf(loaded.getOrDefault(emptyList())) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var enabled by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val loadFailed = loaded.isFailure
    var savedTitleId by remember(game.fileUri) { mutableStateOf(KlCitraCheats.titleId(context, game)) }
    var titleId by rememberSaveable(game.fileUri) { mutableStateOf(savedTitleId) }
    val idReady = game.systemId != "3ds" ||
        (KlCitraCheats.validTitleId(titleId.trim()) && titleId.trim().equals(savedTitleId, ignoreCase = true))

    fun persist(next: List<KlCheats.Entry>, afterSave: () -> Unit = {}) {
        if (busy || loadFailed) return
        busy = true
        error = null
        scope.launch {
            try {
                KlCheats.save(context, game, next)
                cheats = next
                afterSave()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                error = exception.message ?: "Não foi possível salvar os cheats."
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cheats KL", Modifier.weight(1f))
                IconButton(enabled = !busy, onClick = onClose) {
                    Icon(Icons.Default.Close, "Fechar cheats")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(game.title)
                Text(KlCheatFormats.hint(game.systemId))
                Text("Use códigos feitos para a região e a versão exatas do seu jogo.")
                Text(if (game.systemId == "3ds") "Depois de salvar, saia do jogo e abra-o novamente pela biblioteca. O botão Reiniciar não recarrega este arquivo." else "As mudanças serão aplicadas quando você fechar o menu e voltar ao jogo.")
                if (game.systemId == "3ds") {
                    OutlinedTextField(
                        value = titleId, onValueChange = { titleId = it.take(16) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        label = { Text("Title ID do jogo 3DS") },
                        supportingText = { Text("Detectado ao abrir .3ds/.cci/.cxi. Se estiver vazio, informe os 16 dígitos do ID do seu jogo.") },
                        isError = titleId.isNotEmpty() && !KlCitraCheats.validTitleId(titleId.trim()),
                    )
                    if (!idReady) TextButton(
                        enabled = !busy && !loadFailed && KlCitraCheats.validTitleId(titleId.trim()),
                        onClick = {
                            busy = true; error = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { KlCitraCheats.storeTitleId(context, game, titleId) }
                                    savedTitleId = titleId.trim().uppercase(java.util.Locale.ROOT)
                                    titleId = savedTitleId
                                } catch (exception: CancellationException) { throw exception }
                                catch (exception: Exception) { error = exception.message }
                                finally { busy = false }
                            }
                        },
                    ) { Text("Salvar Title ID") }
                }
                if (loadFailed) {
                    Text("Não foi possível ler a lista salva. Feche e abra o menu novamente.")
                } else if (cheats.isEmpty()) {
                    Text("Nenhum cheat adicionado ainda.")
                }
                error?.let { Text(it) }
                if (busy) Text("Salvando…")
                cheats.forEach { entry ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(entry.name, modifier = Modifier.weight(1f))
                        Switch(
                            checked = entry.enabled,
                            enabled = !busy && !loadFailed && idReady,
                            onCheckedChange = { checked ->
                                persist(cheats.map { if (it.id == entry.id) it.copy(enabled = checked) else it })
                            },
                        )
                    }
                    Text(if (entry.enabled) "Ligado" else "Desligado")
                    Row {
                        TextButton(
                            enabled = !busy && !loadFailed && idReady,
                            onClick = {
                                editingId = entry.id
                                name = entry.name
                                code = entry.code
                                enabled = entry.enabled
                                error = null
                                showEditor = true
                            },
                        ) { Text("Editar") }
                        TextButton(
                            enabled = !busy && !loadFailed && idReady,
                            onClick = { deletingId = entry.id },
                        ) { Text("Excluir") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = onClose) { Text("Concluir") }
        },
        dismissButton = {
            TextButton(
                enabled = !busy && !loadFailed && idReady && cheats.size < KlCheats.MAX_ENTRIES,
                onClick = {
                    editingId = null
                    name = ""
                    code = ""
                    enabled = false
                    error = null
                    showEditor = true
                },
            ) { Text("Adicionar") }
        },
    )

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { if (!busy) showEditor = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (editingId == null) "Adicionar cheat" else "Editar cheat", Modifier.weight(1f))
                    IconButton(enabled = !busy, onClick = { showEditor = false }) { Icon(Icons.Default.Close, "Fechar edição do cheat") }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 80) name = it },
                        label = { Text("Nome do cheat") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 8192) code = it },
                        label = { Text("Código") },
                        supportingText = { Text("Cole todas as linhas do mesmo cheat juntas, incluindo o código mestre quando necessário.") },
                        minLines = 3,
                        maxLines = 8,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ativar este cheat", modifier = Modifier.weight(1f))
                        Switch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !busy)
                    }
                    error?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        try {
                            require(name.isNotBlank()) { "Dê um nome para o cheat." }
                            val normalized = KlCheats.normalizeCode(code, game.systemId)
                            val existing = cheats.firstOrNull { it.id == editingId }
                            val entry = existing?.copy(name = name.trim(), code = normalized, enabled = enabled)
                                ?: KlCheats.Entry(name = name.trim(), code = normalized, enabled = enabled)
                            val next = if (existing == null) cheats + entry else
                                cheats.map { if (it.id == existing.id) entry else it }
                            persist(next) { showEditor = false }
                        } catch (exception: IllegalArgumentException) {
                            error = exception.message
                        }
                    },
                ) { Text(if (busy) "Salvando…" else "Salvar") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showEditor = false }) { Text("Cancelar") }
            },
        )
    }

    val deleting = cheats.firstOrNull { it.id == deletingId }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) deletingId = null },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Excluir cheat?") },
            text = {
                Column {
                    Text(deleting.name)
                    error?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { persist(cheats.filter { it.id != deleting.id }) { deletingId = null } },
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { deletingId = null }) { Text("Cancelar") }
            },
        )
    }
}
