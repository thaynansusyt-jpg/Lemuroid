package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.saves.KlSaveImporter
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun KlImportSaveAction(game: Game) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable(game.id) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by rememberSaveable(game.id) { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> selected = uri?.toString() }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedButton(enabled = !busy, onClick = { picker.launch(arrayOf("*/*")) }) {
            Text(if (busy) "Importando save…" else "Importar save de outro emulador")
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
    selected?.let { value ->
        AlertDialog(onDismissRequest = { if (!busy) selected = null },
            title = { Text("Importar para ${game.title}?") },
            text = { Text("Escolha o save deste mesmo jogo e versão. O save atual terá backup e os estados automáticos antigos serão arquivados. Estados manuais continuam disponíveis e podem restaurar o progresso antigo. No DS, use o núcleo melonDS. Feche qualquer jogo antes de importar.") },
            confirmButton = { Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try { message = KlSaveImporter.importSave(context, game, Uri.parse(value)) }
                    catch (error: CancellationException) { throw error }
                    catch (error: Exception) { message = error.message ?: "Não foi possível importar." }
                    finally { busy = false; selected = null }
                }
            }) { Text("Importar com backup") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { selected = null }) { Text("Cancelar") } })
    }
}
