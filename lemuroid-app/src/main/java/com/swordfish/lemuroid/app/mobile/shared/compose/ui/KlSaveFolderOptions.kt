package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.storage.KlPublicSaves
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun KlSaveFolderOptions() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(KlPublicSaves.status(context)) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if(uri!=null) scope.launch {
            busy=true
            val result=withContext(Dispatchers.IO) {runCatching {
                check(!GameProcessLock.isHeldByAnotherProcess(context)) {"Feche o jogo antes de mudar a pasta dos saves."}
                KlPublicSaves.configure(context,uri)
            }}
            busy=false;status=KlPublicSaves.status(context)
            Toast.makeText(context,result.fold({"Pasta conectada. Saves preservados."},{it.message ?: "Não foi possível conectar a pasta."}),Toast.LENGTH_LONG).show()
        }
    }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Saves na memória interna",style=MaterialTheme.typography.titleMedium)
        Text("Escolha ou crie Documentos/KL Play. Saves, estados, prévias e backups do cabo são copiados para essa pasta ao salvar. Você poderá acessá-los pelo gerenciador de arquivos.")
        Text("Os núcleos mantêm uma cópia de trabalho privada. Nenhum save antigo é apagado. Saves nativos dentro da pasta de saves do núcleo, como os do Citra, são copiados ao sair ou colocar o jogo em segundo plano. Não inclui ROMs, DLCs nem cache de shaders.",style=MaterialTheme.typography.bodySmall)
        Button(enabled=!busy,onClick={picker.launch(null)}) {Text(if(busy)"Copiando…" else "Escolher pasta dos saves")}
        TextButton(enabled=!busy && KlPublicSaves.selected(context)!=null,onClick={scope.launch {
            busy=true;withContext(Dispatchers.IO){KlPublicSaves.sync(context)};status=KlPublicSaves.status(context);busy=false
        }}) {Text("Copiar saves agora")}
        if(status.isNotBlank())Text(status,style=MaterialTheme.typography.bodySmall)
    }
}
