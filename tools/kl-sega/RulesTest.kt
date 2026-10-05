import com.swordfish.lemuroid.app.shared.profile.KlAchievements as A
import com.swordfish.lemuroid.app.shared.game.KlPlaySettings as S
fun main() {
 val zero=A.Facts(0,0,0,0,0,0,0,0,0,emptySet())
 check(A.medals.size==128 && A.medals.map{it.id}.toSet()==(1..128).toSet())
 check(A.medals.map{it.color}.toSet().size==128)
 check(A.unlock(zero,emptySet()).isEmpty())
 check(1 !in A.unlock(zero,emptySet()))
 check(1 in A.unlock(zero.copy(minutes=1),emptySet()))
 val ninetyEight=(1..99).filter{it!=99}.toSet()
 check(100 !in A.unlock(zero,ninetyEight))
 check(100 in A.unlock(zero,(1..99).toSet()))
 check(A.unlock(zero,setOf(1,2))==setOf(1,2))
 val max=A.Facts(Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,setOf("sonic_theme","wiiu_theme","sonic_skin","fullscreen","nds_layout","3ds_layout","profile_name","sii_edit"))
 check(A.unlock(max,emptySet()).size==128)
 check(A.unlock(zero,setOf(-1,0,129)).isEmpty())
 check(S.screenOptions("melonds","LARGE",9)["melonds_hybrid_ratio"]=="3")
 check(S.screenOptions("citra","BOTTOM",2)["citra_swap_screen"]=="enabled")
 check(S.screenOptions("desmume","TOP",2)["desmume_screens_layout"]=="top only")
 check(S.screenOptions("citra","DEFAULT",2).isEmpty())
 check(S.screenOptions("mgba","LARGE",2).isEmpty())
 println("128 medals: unique rewards, real thresholds, sticky unlocks, Super Sonic gate and screen presets passed")
}
