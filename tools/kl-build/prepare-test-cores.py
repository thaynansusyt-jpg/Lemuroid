#!/usr/bin/env python3
"""Prepare verified official ARM64 binaries for a local debug build.
Release builds compile the cores from source in the workflow.
"""
from pathlib import Path
import hashlib
import io
import subprocess
import zipfile

root = Path(__file__).resolve().parents[2]
cache = root / ".kl-downloads"
cache.mkdir(exist_ok=True)
output = root / "lemuroid-app/src/main/jniLibs/arm64-v8a"
output.mkdir(parents=True, exist_ok=True)
base = "https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/"
cores = [
    ("dolphin_libretro_android.so", "libdolphin_libretro_android.so", "5646d7b5670a0a84f235ef037eb6644d5434a21eaf0dfec7a1b0ea2d9ccf2a5d"),
    ("melondsds_libretro_android.so", "libkl_melondsds.so", "4f1db7047b6c206ff2898c1a4db4510c02a043a1089f8e115ddfce8c717d5fca"),
]
def fetch(url, path):
    if not path.exists():
        subprocess.run(["curl", "-fL", "--retry", "2", "--max-time", "180", url, "-o", str(path)], check=True)
for name, destination, digest in cores:
    if (output / destination).is_file():
        continue
    archive = cache / (name + ".zip")
    fetch(base + name + ".zip", archive)
    if hashlib.sha256(archive.read_bytes()).hexdigest() != digest:
        raise SystemExit("Upstream nightly changed. Use the pinned source workflow or review and update its checksum; do not bypass verification.")
    with zipfile.ZipFile(archive) as stream:
        data = stream.read(name)
        assert data[:4] == b"\x7fELF" and data[18:20] == b"\xb7\x00", "Expected an ARM64 ELF core"
        (output / destination).write_bytes(data)
resources = root / "lemuroid-app/src/main/assets/dolphin-emu/Sys"
if not (resources / "GC").is_dir():
    revision = "3aac3f052f733f8df2f1616843a4d2e14002ed11"
    archive = cache / ("dolphin-" + revision + ".zip")
    fetch("https://github.com/libretro/dolphin/archive/" + revision + ".zip", archive)
    prefix = "dolphin-" + revision + "/Data/Sys/"
    with zipfile.ZipFile(archive) as stream:
        for item in stream.infolist():
            if item.filename.startswith(prefix) and not item.is_dir():
                relative = Path(item.filename[len(prefix):])
                assert not relative.is_absolute() and ".." not in relative.parts
                target = resources / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(stream.read(item))
    assert (resources / "GC").is_dir()
print("GameCube and DS debug inputs ready")
