package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings
import kotlin.math.roundToInt

/** Fullscreen gesture editor. Positions retain the existing orientation-specific format. */
@Composable
fun KlDualScreenEditor(system: String, onClose: () -> Unit, onSaved: () -> Unit,
    live: Boolean = false, onPreview: (Map<String, String>) -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { KlPlaySettings.preferences(context) }
    val prefix = if (system == "3DS") "kl_dual_3ds_" else "kl_dual_nds_"
    val fields = listOf("top_x", "top_y", "top_w", "bottom_x", "bottom_y", "bottom_w")
    val defaults = listOf(50f, 0f, 90f, 50f, 100f, 70f)
    val p = remember { mutableStateListOf(*fields.mapIndexed { i,k -> prefs.getInt(prefix+"p_"+k,defaults[i].toInt()).toFloat() }.toTypedArray()) }
    val l = remember { mutableStateListOf(*fields.mapIndexed { i,k -> prefs.getInt(prefix+"l_"+k,defaults[i].toInt()).toFloat() }.toTypedArray()) }
    val portrait = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE
    val values = if (portrait) p else l
    var selected by remember { mutableStateOf(0) }
    var resizing by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val topColor = MaterialTheme.colorScheme.primary
    val bottomColor = MaterialTheme.colorScheme.tertiary
    val preview by rememberUpdatedState(onPreview)
    LaunchedEffect(p.toList(), l.toList(), portrait) {
        val options = mutableMapOf("kl_dual_kind" to if(system=="3DS")"3ds" else "nds",
            "kl_dual_orientation" to if(portrait)"p" else "l")
        fields.forEachIndexed { i,k -> options["kl_dual_p_$k"] = p[i].roundToInt().toString(); options["kl_dual_l_$k"] = l[i].roundToInt().toString() }
        preview(options)
    }
    BackHandler(onBack=onClose)
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))) {
        Canvas(Modifier.fillMaxSize().pointerInput(portrait) {
            fun rect(i: Int): Rect {
                var w = values[i*3+2].coerceIn(10f,100f)/100*size.width
                val aspect = if(system=="3DS" && i==0)5f/3 else 4f/3
                var h = w/aspect
                if(h>size.height) { w *= size.height/h; h = size.height.toFloat() }
                val x = values[i*3]/100*(size.width-w)
                val y = values[i*3+1]/100*(size.height-h)
                return Rect(x,y,x+w,y+h)
            }
            detectDragGestures(onDragStart={ pos ->
                val handle = 30.dp.toPx()
                val hit = listOf(selected, 1-selected).firstOrNull { i ->
                    val r = rect(i)
                    r.contains(pos) || (pos-r.bottomRight).getDistance() < handle
                }
                dragging = hit != null
                if(hit!=null) { selected=hit; resizing=(pos-rect(hit).bottomRight).getDistance()<handle }
            }, onDragEnd={dragging=false}, onDragCancel={dragging=false}) { change, delta ->
                change.consume()
                if(dragging) {
                    val r = rect(selected); val k = selected*3
                    val aspect = if(system=="3DS" && selected==0)5f/3 else 4f/3
                    val w = if(resizing) (r.width + (delta.x + delta.y*aspect)/2).coerceIn(size.width*.1f, minOf(size.width.toFloat(),size.height*aspect)) else r.width
                    val h = w/aspect
                    val x = (r.left + if(resizing)0f else delta.x).coerceIn(0f,(size.width-w).coerceAtLeast(0f))
                    val y = (r.top + if(resizing)0f else delta.y).coerceIn(0f,(size.height-h).coerceAtLeast(0f))
                    values[k] = if(size.width-w>0.5f)x/(size.width-w)*100 else 50f
                    values[k+1] = if(size.height-h>0.5f)y/(size.height-h)*100 else 50f
                    values[k+2] = w/size.width*100
                }
            }
        }) {
            if(!live) drawRect(Color(0xff091521))
            for(i in 0..1) {
                var w = values[i*3+2].coerceIn(10f,100f)/100*size.width
                val aspect = if(system=="3DS" && i==0)5f/3 else 4f/3
                var h = w/aspect
                if(h>size.height) { w*=size.height/h;h=size.height }
                val origin = Offset(values[i*3]/100*(size.width-w),values[i*3+1]/100*(size.height-h))
                val color = if(i==0)topColor else bottomColor
                if(!live) drawRect(color.copy(alpha=.15f),origin,Size(w,h))
                drawRect(color,origin,Size(w,h),style=Stroke(if(selected==i)4.dp.toPx() else 2.dp.toPx()))
                drawCircle(color,12.dp.toPx(),origin+Offset(w,h))
                drawCircle(Color.White,4.dp.toPx(),origin+Offset(w,h))
            }
        }
        Surface(Modifier.align(Alignment.TopCenter),color=MaterialTheme.colorScheme.surface.copy(alpha=.94f)) {
            Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("Arraste a tela • puxe o canto para redimensionar",style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected==0,{selected=0},label={Text("Superior")})
                    FilterChip(selected==1,{selected=1},label={Text("Inferior")})
                    TextButton(onClick={defaults.forEachIndexed { i,v ->values[i]=v }}) {Text("Restaurar")}
                }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter),color=MaterialTheme.colorScheme.surface.copy(alpha=.94f)) {
            Row(Modifier.padding(8.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                TextButton(onClick=onClose) {Text("Cancelar")}
                Button(onClick={
                    val edit=prefs.edit()
                    fields.forEachIndexed { i,k ->edit.putInt(prefix+"p_"+k,p[i].roundToInt()); edit.putInt(prefix+"l_"+k,l[i].roundToInt()) }
                    if(live) edit.putBoolean(KlPlaySettings.FULL_SCREEN,true)
                    edit.putString(if(system=="3DS")KlPlaySettings.THREEDS_LAYOUT else KlPlaySettings.NDS_LAYOUT,"CUSTOM").commit()
                    onSaved()
                }) {Text("Salvar")}
            }
        }
    }
}
