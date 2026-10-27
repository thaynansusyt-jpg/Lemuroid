# GameCube / DS native inputs

Release workflow: libretro/dolphin@3aac3f052f733f8df2f1616843a4d2e14002ed11 (GPL-2.0-or-later) and its Data/Sys resources; JesseTG/melonds-ds@f394adbacb5722ee97c1b37c8064da9a25818310 (GPL-3.0).

The local debug candidate uses the official Android ARM64 buildbot binaries obtained on 2026-10-27. These are not claimed to be byte-identical to the workflow source builds:
- https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/dolphin_libretro_android.so.zip — SHA256 5646d7b5670a0a84f235ef037eb6644d5434a21eaf0dfec7a1b0ea2d9ccf2a5d
- https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/melondsds_libretro_android.so.zip — SHA256 4f1db7047b6c206ff2898c1a4db4510c02a043a1089f8e115ddfce8c717d5fca

No games or proprietary BIOS dumps are included. Preserve upstream notices when redistributing a build; source for each core is available from the corresponding upstream repository, and the release workflow pins its inputs.
