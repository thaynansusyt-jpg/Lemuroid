#!/usr/bin/env python3
"""Apply the KL LAN frontend to an immutable, separately bundled mGBA core."""
from pathlib import Path
import shutil
import sys

root = Path(sys.argv[1])
source = root / "src/platform/libretro/libretro.c"
text = source.read_text()
changes = {
    'void retro_set_audio_sample_batch(retro_audio_sample_batch_t audioBatch) {\n\taudioCallback = audioBatch;':
    'void retro_set_audio_sample_batch(retro_audio_sample_batch_t audioBatch) {\n\tkl_audio_parent = audioBatch;\n\taudioCallback = kl_audio_batch;',
    'void retro_set_environment(retro_environment_t env) {\n\tenvironCallback = env;':
    '#include "kl-lan.h"\n\nvoid retro_set_environment(retro_environment_t env) {\n\tkl_parent = env;\n\tenvironCallback = kl_environment;',
    '\tmLibretroGBAWifiLinkRunBegin();': '\tkl_tick();\n\tmLibretroGBAWifiLinkRunBegin();',
    'void retro_unload_game(void) {': 'void retro_unload_game(void) {\n\tkl_shutdown();',
    'void retro_deinit(void) {': 'void retro_deinit(void) {\n\tkl_shutdown();',
}
for old, new in changes.items():
    if text.count(old) != 1:
        raise SystemExit("Pinned core boundary changed: " + old)
    text = text.replace(old, new, 1)
source.write_text(text)
shutil.copyfile(Path(__file__).with_name("kl-lan.h"), source.with_name("kl-lan.h"))
