package com.swordfish.lemuroid.app.mobile.feature.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.swordfish.lemuroid.app.shared.profile.*
import kotlinx.coroutines.*
import java.time.LocalDate

private fun duration(ms: Long): String {
    val seconds = ms / 1000
    return if (seconds >= 3600) "${seconds/3600} h ${(seconds%3600)/60} min" else if (seconds >= 60) "${seconds/60} min" else "$seconds s"
}

@Composable
fun KlProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<KlProfileStore.Profile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var erase by remember { mutableStateOf(false) }
    var klLogin by remember { mutableStateOf(false) }
    var restoreCloud by remember { mutableStateOf(false) }
    var deleteCloud by remember { mutableStateOf(false) }
    var cloudBusy by remember { mutableStateOf(false) }
    var lastBackup by remember { mutableStateOf(0L) }
    var prompt by remember { mutableStateOf<KlAccountAuth.DevicePrompt?>(null) }
    var loginJob by remember { mutableStateOf<Job?>(null) }
    suspend fun refresh() { profile = withContext(Dispatchers.IO) { KlProfileStore.snapshot(context) } }
    fun action(block: suspend () -> Unit) {
        scope.launch {
            try { block(); refresh(); error = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Não foi possível salvar. Tente novamente." }
        }
    }
    fun cloudAction(block: suspend () -> Unit) {
        if (cloudBusy) return
        cloudBusy = true
        scope.launch {
            try { block(); refresh(); lastBackup = KlCloudAccount.lastBackup(context); error = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "A conexão falhou. Seus dados locais foram preservados." }
            finally { cloudBusy = false }
        }
    }
    fun login(block: suspend () -> Unit) {
        loginJob = scope.launch {
            try { block(); refresh(); error = null; message = "Conta conectada." }
            catch (e: TimeoutCancellationException) { error = "O código expirou. Tente entrar novamente." }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Não foi possível entrar. Tente novamente." }
            finally { prompt = null; loginJob = null }
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            try { refresh(); if (profile?.provider == "kl") lastBackup = KlCloudAccount.lastBackup(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { error = "Não foi possível abrir o perfil. Seus dados foram preservados." }
            delay(5000)
        }
    }
    val p = profile
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it) }
        if (p == null) { CircularProgressIndicator(); return@Column }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KlSii(p.sii, Modifier.fillMaxWidth().height(210.dp))
            Text(p.name, style = MaterialTheme.typography.headlineSmall)
            Text(p.id, style = MaterialTheme.typography.labelLarge)
            Text(when (p.provider) { "google" -> "Conta Google"; "github" -> "Conta GitHub"; "kl" -> "Conta KL autenticada"; else -> "Perfil neste aparelho" })
            Row { TextButton(onClick = { editName = true }) { Text("Editar nome") }; TextButton(onClick = { clipboard.setText(AnnotatedString(p.id)); message = "ID copiado." }) { Text("Copiar ID") } }
            Button(onClick = { editor = true }) { Text("Personalizar Sii") }
        } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sua conta", style = MaterialTheme.typography.titleLarge)
            if (p.provider == "local") {
                Button(onClick = { klLogin = true }, enabled = !cloudBusy, modifier = Modifier.fillMaxWidth()) { Text("Entrar com conta KL") }
                TextButton(onClick = { KlCloudAccount.openSite(context) }) { Text("Criar conta no site • grátis") }
                OutlinedButton(onClick = { login { KlAccountAuth.google(context as Activity) } }, enabled = KlAccountAuth.googleAvailable && loginJob == null, modifier = Modifier.fillMaxWidth()) { Text(if (KlAccountAuth.googleAvailable) "Entrar com Google" else "Google • em preparação") }
                OutlinedButton(onClick = { login { KlAccountAuth.github(context) { prompt = it } } }, enabled = KlAccountAuth.githubAvailable && loginJob == null, modifier = Modifier.fillMaxWidth()) { Text(if (KlAccountAuth.githubAvailable) "Entrar com GitHub" else "GitHub • em preparação") }
                if (!KlAccountAuth.googleAvailable && !KlAccountAuth.githubAvailable) Text("A conta KL salva seu perfil no site e libera o conjunto Sii+. Você também pode jogar sem conta.")
            } else if (p.provider == "kl") {
                Text("Backup: nome, Sii, diário e estatísticas. Saves dos jogos, ROMs, skins e capas ficam no aparelho.")
                Text(if (lastBackup > 0) "Último envio confirmado: " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(lastBackup * 1000)) else "Ainda sem envio confirmado.")
                Button(onClick = { cloudAction { KlCloudAccount.backup(context); message = "Backup confirmado pelo servidor." } }, enabled = !cloudBusy) { Text("Salvar perfil no site") }
                OutlinedButton(onClick = { restoreCloud = true }, enabled = !cloudBusy) { Text("Restaurar perfil do site") }
                TextButton(onClick = { cloudAction { KlCloudAccount.logout(context); message = "Você saiu. O backup permanece no site." } }, enabled = !cloudBusy) { Text("Sair da conta KL") }
                TextButton(onClick = { deleteCloud = true }, enabled = !cloudBusy) { Text("Excluir conta e backup do site") }
            } else TextButton(onClick = { action { KlAccountAuth.signOut(context) } }) { Text("Sair da conta") }
            if (cloudBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (loginJob != null) { LinearProgressIndicator(Modifier.fillMaxWidth()); TextButton(onClick = { loginJob?.cancel() }) { Text("Cancelar login") } }
            Text("O aplicativo tenta salvar o perfil KL ao pausar uma partida. Confira a data do último envio e use Salvar perfil para confirmar suas mudanças. A conexão é necessária para backup; jogar continua funcionando offline.", style = MaterialTheme.typography.bodySmall)
        } }
        val today = LocalDate.now().toString()
        val todayStats = p.days.firstOrNull { it.date == today }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Diário de jogo", style = MaterialTheme.typography.titleLarge)
            Text("Hoje: ${duration(todayStats?.millis ?: 0)} • ${todayStats?.sessions ?: 0} sessões")
            Text("Sequência: ${p.streak} dias • Recorde: ${p.bestStreak} dias")
            Text("Total jogado: ${duration(p.totalMillis)}")
            LinearProgressIndicator(progress = ((todayStats?.millis ?: 0).toFloat()/KlDiaryMath.DAILY_GOAL_MS).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
            Text("Jogue ao menos 1 minuto por dia para manter a sequência. Tempo em menus e em segundo plano não conta.", style = MaterialTheme.typography.bodySmall)
        } }
        val dates = (listOf(today) + p.days.map { it.date }).distinct().sortedDescending()
        var selected by remember(p.key) { mutableStateOf(today) }
        var visibleDays by remember { mutableStateOf(7) }
        Text("Seus dias", style = MaterialTheme.typography.titleMedium)
        dates.take(visibleDays).forEach { date ->
            val day = p.days.firstOrNull { it.date == date }
            OutlinedButton(onClick = { selected = date }, modifier = Modifier.fillMaxWidth()) { Text("${if (selected == date) "✓ " else ""}$date • ${duration(day?.millis ?: 0)}") }
        }
        if (dates.size > visibleDays) TextButton(onClick = { visibleDays += 14 }) { Text("Ver mais dias") }
        val day = p.days.firstOrNull { it.date == selected }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(selected, style = MaterialTheme.typography.titleMedium)
            day?.games?.entries?.sortedByDescending { it.value }?.forEach { (title, time) -> Text("$title • ${duration(time)}") }
            var note by remember(p.key, selected) { mutableStateOf(day?.note ?: "") }
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text("O que aconteceu na sua aventura?") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
            Button(onClick = { action { withContext(Dispatchers.IO) { KlProfileStore.note(context, selected, note) }; message = "Anotação salva." } }) { Text("Salvar anotação") }
        } }
        Text("KL Play • Fork do Lemuroid sob GPL-3.0. Créditos e licenças dos componentes preservados.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thaynansusyt-jpg/Lemuroid"))) }) { Text("Código-fonte e créditos") }
        TextButton(onClick = { erase = true }) { Text("Apagar este perfil local", color = MaterialTheme.colorScheme.error) }
    }
    if (p != null && editor) KlSiiEditor(p.sii, p.provider == "kl", { editor = false }) { sii -> editor = false; action { withContext(Dispatchers.IO) { KlProfileStore.update(context, p.name, sii) } } }
    if (p != null && editName) {
        var name by remember { mutableStateOf(p.name) }
        AlertDialog(onDismissRequest = { editName = false }, title = { Text("Seu nome") }, text = { OutlinedTextField(name, { name = it.take(32) }, singleLine = true) }, confirmButton = { Button(onClick = { editName = false; action { withContext(Dispatchers.IO) { KlProfileStore.update(context, name, p.sii) } } }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancelar") } })
    }
    if (erase) AlertDialog(onDismissRequest = { erase = false }, title = { Text("Apagar este perfil?") }, text = { Text("Nome, Sii, diário e sequência deste perfil serão apagados neste aparelho. Os jogos e saves serão preservados.") }, confirmButton = { TextButton(onClick = { erase = false; action { withContext(Dispatchers.IO) { KlProfileStore.eraseCurrent(context) }; message = "Perfil apagado." } }) { Text("Apagar") } }, dismissButton = { TextButton(onClick = { erase = false }) { Text("Cancelar") } })
    if (klLogin) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { if (!cloudBusy) klLogin = false }, properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Entrar com conta KL") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Copie o usuário e a senha gerados no site. Se já existir um backup, o perfil dessa conta será restaurado. O perfil sem conta fica preservado.")
                OutlinedTextField(username, { username = it.take(19) }, label = { Text("Usuário KL") }, singleLine = true, enabled = !cloudBusy)
                OutlinedTextField(password, { password = it.take(64) }, label = { Text("Senha") }, singleLine = true, enabled = !cloudBusy,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                TextButton(onClick = { KlCloudAccount.openSite(context) }) { Text("Criar conta no site") }
                if (cloudBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } }, confirmButton = { Button(onClick = { cloudAction { KlCloudAccount.login(context, username, password); password = ""; klLogin = false; message = "Conta autenticada. Conjunto Sii+ desbloqueado." } }, enabled = !cloudBusy && username.isNotBlank() && password.isNotBlank()) { Text("Entrar") } },
            dismissButton = { TextButton(onClick = { password = ""; klLogin = false }, enabled = !cloudBusy) { Text("Fechar") } })
    }
    if (restoreCloud) AlertDialog(onDismissRequest = { restoreCloud = false }, title = { Text("Restaurar backup?") }, text = { Text("O perfil salvo no site substituirá o nome, o Sii e o diário desta conta no aparelho. Alterações locais ainda não enviadas não serão mescladas. Saves dos jogos permanecem.") },
        confirmButton = { Button(onClick = { restoreCloud = false; cloudAction { KlCloudAccount.restore(context); message = "Perfil restaurado do site." } }) { Text("Restaurar") } }, dismissButton = { TextButton(onClick = { restoreCloud = false }) { Text("Cancelar") } })
    if (deleteCloud) AlertDialog(onDismissRequest = { deleteCloud = false }, title = { Text("Excluir conta KL?") }, text = { Text("A conta e o backup no site serão excluídos definitivamente. Seus jogos e saves locais permanecem. Você voltará ao perfil sem conta.") },
        confirmButton = { TextButton(onClick = { deleteCloud = false; cloudAction { KlCloudAccount.eraseRemote(context); message = "Conta e backup excluídos do servidor." } }) { Text("Excluir definitivamente") } }, dismissButton = { TextButton(onClick = { deleteCloud = false }) { Text("Cancelar") } })
    prompt?.let { device ->
        AlertDialog(onDismissRequest = { loginJob?.cancel() }, properties = DialogProperties(dismissOnClickOutside = false), title = { Text("Conectar GitHub") }, text = { Column { Text("Copie o código e confirme sua conta na página oficial do GitHub."); Text(device.code, style = MaterialTheme.typography.headlineMedium); TextButton(onClick = { clipboard.setText(AnnotatedString(device.code)) }) { Text("Copiar código") } } }, confirmButton = { Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(device.verificationUrl))) }) { Text("Abrir GitHub") } }, dismissButton = { TextButton(onClick = { loginJob?.cancel() }) { Text("Cancelar") } })
    }
}
