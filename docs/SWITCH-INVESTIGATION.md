# Switch integration investigation — 2026-10-05

Switch is NOT supported by KL Play 0.5.0s-plus.2. This document records the actual upstream interfaces inspected, not a working engine or a promise of performance.

## Inspected source

Official Eden mirror: https://github.com/eden-emulator/mirror/tree/10bcd2d849843b146a79c61a21df615e1bdcfcde

- src/android/app/build.gradle.kts builds an Android application, ARM64, SDK 36, NDK 28.2.13676358. Mainline requires API 33. It is not an Android library plug-in for KL.
- NativeLibrary.kt loads yuzu-android and exposes run, surfaceChanged, initializeSystem, initializeGpuDriver, pause, stop and input methods. It depends on Eden Android activities, models, file helpers and applet dialogs.
- src/android/app/src/main/jni/native.cpp exports Java_org_yuzu_yuzu_1emu_* JNI methods tied to these classes. Loading this library with LibretroDroid's retro_* interface is not an integration.
- CMakeLists.txt requires CMake 3.31 and its own native dependency tree. KL currently builds with a different NDK/CMake/runtime.
- SupportsCustomDriver checks Android API >=28 and the KGSL device. Driver installation through adrenotools does not create Mali support. Mali experiments should use the system Vulkan driver; Turnip targets Adreno.

Official requirements: https://eden-emu.dev/system-requirements/ — Android Vulkan 1.1, 8 GB RAM minimum, including Mali G78 among listed minimum GPUs. The user's 4 GB device is below this RAM requirement. This is a limitation to test, not proof that every title will always fail or a reason to invent memory settings.

## Required first experiment

Before porting the engine, establish whether the unmodified official Eden can boot one user-owned Let's Go title on the actual device. Record exact phone model, Android version, GPU/driver, engine build, resolution, last log, whether it reaches a playable scene, and Android low-memory termination. Start with system driver and lower internal resolution available in Eden. Change one real upstream option at a time. Try BDSP only after the first playable baseline.

If the baseline is viable, build a pinned ARM64 engine separately, then implement an isolated JNI-backed Android module and process in KL: Surface lifecycle, game file access, user-supplied system files, input, audio, memory reporting and crash logs. Keep all existing LibretroDroid consoles intact. Every exposed setting must be forwarded to the real engine and hardware-specific driver options must be filtered. A launcher button or an installed external Eden APK is not embedded Switch emulation.

No ROMs, console keys or firmware are included. Preserve Eden GPLv3-or-later, notices and dependency licenses when redistributing a derived engine. Original Android signing keys must remain private.

## Alternative core check

Suyu's current release workflow at https://github.com/suyu-emu/suyu-main/blob/mk8-recomp/.github/workflows/release.yml publishes libretro artifacts for Linux x86_64, Windows x86_64 and macOS ARM64. A standalone Android application build is different from a working Android libretro core. This inspected workflow does not supply an Android libretro artifact for KL.

KL uses LibretroDroid 0.13.2 at 0ebd299624bfd51a0a1336dd0a2c56fe7ddbc0e3. Its environment.cpp advertises RETRO_HW_CONTEXT_OPENGLES3; hardware callbacks return an EGL framebuffer and eglGetProcAddress. Its video backend links GLESv3. It does not provide the frontend Vulkan render interface needed for direct reuse of a Switch Vulkan core. A future implementation must explicitly negotiate a supported context and isolate the new Vulkan surface/backend, instead of pretending an incompatible Vulkan library is a GLES core.

The investigation covers interface and published build evidence, not proof that no experimental Android fork exists anywhere. No custom Mali driver or Switch-capable APK has been produced by this work. Phone testing remains outstanding.
