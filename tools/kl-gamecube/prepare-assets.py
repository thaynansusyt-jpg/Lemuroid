#!/usr/bin/env python3
"""Package only upstream Dolphin system resources, never games or user saves."""
from pathlib import Path
import shutil
import sys
source = Path(sys.argv[1]).resolve() / "Data" / "Sys"
assert (source / "GC").is_dir(), "Dolphin Sys resources are missing"
target = Path(__file__).resolve().parents[2] / "lemuroid-app/src/main/assets/dolphin-emu/Sys"
if target.exists():
    shutil.rmtree(target)
shutil.copytree(source, target)
print("Packaged Dolphin system resources")
