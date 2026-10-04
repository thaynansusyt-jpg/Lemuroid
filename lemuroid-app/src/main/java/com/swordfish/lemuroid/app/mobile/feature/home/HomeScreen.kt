package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.swordfish.lemuroid.app.shared.multiplayer.KlWifiRoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.updatePermissions(applicationContext)
            }
            else -> { }
        }
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                context.displayDetailsSettingsScreen()
            }
        }

    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState())
    HomeScreen(
        modifier,
        state.value,
        onGameClick,
        onGameLongClick,
        onOpenCoreSelection,
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@HomeScreen
            }

            permissionsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        { viewModel.changeLocalStorageFolder(context) },
    ) // TODO COMPOSE We need to understand what's going to happen here.
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UIState,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
) {
    var showWifiRoom by rememberSaveable { mutableStateOf(false) }
    if (showWifiRoom) {
        KlWifiRoomDialog(onClose = { showWifiRoom = false })
    }
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF122D23)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("KL GBA", color = Color(0xFF86EFAC), style = MaterialTheme.typography.headlineLarge)
                OutlinedButton(onClick = { showWifiRoom = true }) {
                    Text("Sala Wi-Fi • teste de conexão", color = Color(0xFF86EFAC))
                }
                Text("Sua aventura começa aqui.", color = Color(0xFFF0FFF4), style = MaterialTheme.typography.titleMedium)
                Text(
                    "Seus jogos, suas cores, suas capinhas. Segure um jogo para personalizar a capa.",
                    color = Color(0xFFC4DCCD),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        AnimatedVisibility(state.showNoNotificationPermissionCard) {
            HomeNotification(
                titleId = R.string.home_notification_title,
                messageId = R.string.home_notification_message,
                actionId = R.string.home_notification_action,
                onAction = onEnableNotificationsClicked,
            )
        }
        AnimatedVisibility(state.showNoGamesCard) {
            HomeNotification(
                titleId = R.string.home_empty_title,
                messageId = R.string.home_empty_message,
                actionId = R.string.home_empty_action,
                onAction = onSetDirectoryClicked,
                enabled = !state.indexInProgress,
            )
        }
        AnimatedVisibility(state.showNoMicrophonePermissionCard) {
            HomeNotification(
                titleId = R.string.home_microphone_title,
                messageId = R.string.home_microphone_message,
                actionId = R.string.home_microphone_action,
                onAction = onEnableMicrophoneClicked,
            )
        }
        AnimatedVisibility(state.showDesmumeDeprecatedCard) {
            HomeNotification(
                titleId = R.string.home_notification_desmume_deprecated_title,
                messageId = R.string.home_notification_desmume_deprecated_message,
                actionId = R.string.home_notification_desmume_deprecated_action,
                onAction = onOpenCoreSelection,
            )
        }
        HomeRow(
            stringResource(id = R.string.recent),
            state.recentGames,
            onGameClicked,
            onGameLongClick,
        )
        HomeRow(
            stringResource(id = R.string.favorites),
            state.favoritesGames,
            onGameClicked,
            onGameLongClick,
        )
        HomeRow(
            stringResource(id = R.string.discover),
            state.discoveryGames,
            onGameClicked,
            onGameLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRow(
    title: String,
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    if (games.isEmpty()) {
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        )
        LazyRow(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(games.size, key = { games[it].id }) { index ->
                val game = games[index]
                LemuroidGameCard(
                    modifier =
                        Modifier
                            .widthIn(0.dp, 144.dp)
                            .animateItem(),
                    game = game,
                    onClick = { onGameClicked(game) },
                    onLongClick = { onGameLongClick(game) },
                )
            }
        }
    }
}

@Composable
private fun HomeNotification(
    titleId: Int,
    messageId: Int,
    actionId: Int,
    enabled: Boolean = true,
    onAction: () -> Unit = { },
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(titleId),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(messageId),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onAction,
                enabled = enabled,
            ) {
                Text(stringResource(id = actionId))
            }
        }
    }
}


@Composable
private fun KlWifiRoomDialog(onClose: () -> Unit) {
    val room = remember { KlWifiRoom() }
    DisposableEffect(room) { onDispose { room.close() } }
    val state by room.state.collectAsState()
    var playerName by rememberSaveable { mutableStateOf(Build.MODEL.take(32)) }
    var hostIp by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val connected = state.phase == KlWifiRoom.Phase.HOSTING || state.phase == KlWifiRoom.Phase.JOINED
    val connecting = state.phase == KlWifiRoom.Phase.CONNECTING

    AlertDialog(
        onDismissRequest = { room.close(); onClose() },
        title = { Text("Sala Wi-Fi KL") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Multiplayer Wi-Fi: somente jogos compatíveis com o adaptador wireless do GBA. Nesta etapa, a sala ainda não conecta os jogos.")
                Text("Até 3 pessoas: 1 anfitrião + 2 visitantes. Use o mesmo Wi-Fi nos celulares. A quantidade de participantes dentro do jogo depende do próprio jogo.")
                if (!connected && !connecting) {
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it.take(32) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Seu nome na sala") },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = { room.host(playerName) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Criar sala • anfitrião") }
                    OutlinedTextField(
                        value = hostIp,
                        onValueChange = { hostIp = it.take(15) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("IP do anfitrião") },
                        placeholder = { Text("192.168.1.10") },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = { room.join(hostIp, playerName) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Entrar na sala • visitante") }
                }
                if (state.message.isNotBlank()) Text(state.message)
                if (state.phase == KlWifiRoom.Phase.HOSTING) {
                    Text("No outro celular, toque em Entrar na sala e digite este IP:")
                    if (state.addresses.isEmpty()) {
                        Text("Não encontrei um IP local. Confira se o Wi-Fi está conectado.")
                    }
                    state.addresses.forEach { ip ->
                        Text(ip, style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(ip)) }) {
                            Text("Copiar IP")
                        }
                    }
                }
                if (connected) {
                    Text("Pessoas na sala", style = MaterialTheme.typography.titleMedium)
                    state.members.forEach { member ->
                        Text(
                            (if (member.id == 0) "Anfitrião: " else "Visitante ${member.id}: ") + member.name,
                        )
                    }
                    Text("Conexão da sala pronta. Este teste ainda não conecta as ROMs.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { room.close(); onClose() }) {
                Text(if (connected) "Sair e fechar" else "Fechar")
            }
        },
        dismissButton = {
            if (connected || connecting) {
                TextButton(onClick = { room.leave() }) {
                    Text(if (connecting) "Cancelar conexão" else "Sair da sala")
                }
            }
        },
    )
}
