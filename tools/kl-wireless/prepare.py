from pathlib import Path
import shutil, sys
root = Path(sys.argv[1])
p = root / "libretro/libretro.c"
s = p.read_text()
changes = {
    "void retro_deinit(void)": '#include "kl-packets.h"\n\nvoid retro_deinit(void)',
    "   audio_batch_cb = cb;": "   kl_audio_parent = cb;\n   audio_batch_cb = kl_audio_batch;",
    "   environ_cb = cb;": "   kl_parent = cb;\n   environ_cb = kl_environment;",
    "void retro_run(void)\n{": "void retro_run(void)\n{\n   kl_tick();",
    "void retro_unload_game(void)\n{": "void retro_unload_game(void)\n{\n   kl_shutdown();",
    "void retro_deinit(void)\n{": "void retro_deinit(void)\n{\n   kl_shutdown();",
}
for old, new in changes.items():
    if s.count(old) != 1: raise SystemExit("Pinned gpSP boundary changed: " + old)
    s = s.replace(old, new, 1)
p.write_text(s)
shutil.copyfile(Path(__file__).with_name("kl-packets.h"), p.with_name("kl-packets.h"))
