package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings
import kotlin.math.roundToInt

@Composable
fun KlDualScreenEditor(system: String, onClose: () -> Unit, onSaved: () -> Unit) {
    val context=LocalContext.current
    val prefs=remember { KlPlaySettings.preferences(context) }
    val prefix=if(system=="3DS") "kl_dual_3ds_" else "kl_dual_nds_"
    val fields=listOf("top_x","top_y","top_w","bottom_x","bottom_y","bottom_w")
    val defaults=listOf(50,0,90,50,100,70)
    var portrait by remember { mutableStateOf(true) }
    var bottom by remember { mutableStateOf(false) }
    val p=remember { mutableStateListOf(*fields.mapIndexed { i,k -> prefs.getInt(prefix+"p_"+k,defaults[i]).coerceIn(0,100).toFloat() }.toTypedArray()) }
    val l=remember { mutableStateListOf(*fields.mapIndexed { i,k -> prefs.getInt(prefix+"l_"+k,defaults[i]).coerceIn(0,100).toFloat() }.toTypedArray()) }
    val values=if(portrait)p else l
    val colorTop=MaterialTheme.colorScheme.primary
    val colorBottom=MaterialTheme.colorScheme.tertiary
    Dialog(onDismissRequest={}, properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.94f).heightIn(max=840.dp).fillMaxHeight(.95f), shape=MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(18.dp)) {
                Row {
                    Text("Editar telas de $system",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                    TextButton(onClick=onClose) { Text("X") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(portrait,{portrait=true},label={Text("Vertical")})
                        FilterChip(!portrait,{portrait=false},label={Text("Horizontal")})
                    }
                    Text("Prévia • azul: superior • outra cor: inferior",style=MaterialTheme.typography.bodySmall)
                    Box(Modifier.fillMaxWidth().height(if(portrait)300.dp else 190.dp)) {
                        Canvas(Modifier.aspectRatio(if(portrait)9f/16 else 16f/9).fillMaxHeight()) {
                            drawRect(Color.Black)
                            for(i in 0..1) {
                                val width=values[i*3+2].coerceIn(10f,100f)/100
                                val aspect=if(system=="3DS" && i==0)5f/3 else 4f/3
                                var w=width*size.width; var h=w/aspect
                                if(h>size.height) {w*=size.height/h;h=size.height}
                                val x=values[i*3]/100*(size.width-w)
                                val y=values[i*3+1]/100*(size.height-h)
                                drawRect(if(i==0)colorTop else colorBottom,Offset(x,y),Size(w,h))
                            }
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(!bottom,{bottom=false},label={Text("Superior")})
                        FilterChip(bottom,{bottom=true},label={Text("Inferior")})
                    }
                    val start=if(bottom)3 else 0
                    listOf("Posição horizontal","Posição vertical","Largura").forEachIndexed { i,label ->
                        Text("$label: ${values[start+i].roundToInt()}%")
                        Slider(values[start+i],{values[start+i]=it},valueRange=if(i==2)10f..100f else 0f..100f)
                    }
                    Text("Cada tela mantém sua proporção. Ajustes de vertical e horizontal são independentes. Se as telas se sobrepuserem, a inferior fica na frente. O toque acompanha a posição dela.",style=MaterialTheme.typography.bodySmall)
                    Text("O layout personalizado usa filtro simples. Saia e abra o jogo depois de salvar.",style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={defaults.forEachIndexed { i,v -> values[i]=v.toFloat() }}) {Text("Restaurar esta orientação")}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    TextButton(onClick=onClose){Text("Cancelar")}
                    Button(onClick={
                        val edit=prefs.edit()
                        fields.forEachIndexed { i,k -> edit.putInt(prefix+"p_"+k,p[i].roundToInt());edit.putInt(prefix+"l_"+k,l[i].roundToInt()) }
                        edit.putString(if(system=="3DS")KlPlaySettings.THREEDS_LAYOUT else KlPlaySettings.NDS_LAYOUT,"CUSTOM").apply()
                        onSaved()
                    }) {Text("Salvar e usar")}
                }
            }
        }
    }
}
