# KL Play — 0.5.0-rc.1

Esta versão adiciona introdução, aba Perfil, avatar Sii, camiseta KL de lançamento e diário local com tempo por jogo, anotações e sequência diária. O login Google/GitHub está preparado, mas depende dos cadastros oficiais antes de ser ativado. [Como ativar contas e limites desta versão](docs/CONTAS-KL-PLAY.md).

# KL Play — beta experimental

KL Play é a evolução do KL GBA, baseada no Lemuroid. A versão 0.4.0-beta.1 mantém a instalação e os saves existentes e adiciona uma nova identidade visual e um cabo GBA Wi-Fi experimental para dois jogadores em Android ARM64.

O lobby usa a mesma rede Wi-Fi. Ative **Usar cabo Wi-Fi nos jogos de GBA** nos dois celulares, entre numa sala com exatamente duas pessoas e abra a mesma ROM. Espere **Cabo GBA sincronizado** antes de escolher multiplayer no jogo. Pokémon ainda não tem troca/batalha confirmadas; o adaptador sem fio não é suportado. DS e 3DS continuam sem multiplayer integrado.

[Fontes, limites e testes do núcleo experimental](tools/kl-link/README.md). Os créditos e licenças do Lemuroid e dos núcleos são mantidos abaixo e no projeto. A base singleplayer não foi substituída.

---

# Lemuroid

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.swordfish.lemuroid/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.swordfish.lemuroid)

## Description

Lemuroid is an open-source emulation project for Android based on Libretro. Its main goal is ease of use, good Android integration and a great user experience.

It originated from a rib of [Retrograde](https://github.com/retrograde/retrograde-android), but graduated to a standalone project integrating [LibretroDroid](https://github.com/Swordfish90/LibretroDroid).

|Screen 1|Screen 2|Screen 3|
|---|---|---|
|![Screen1](https://github.com/Swordfish90/Lemuroid/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg)|![Screen2](https://github.com/Swordfish90/Lemuroid/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg)|![Screen3](https://github.com/Swordfish90/Lemuroid/blob/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg)|

### Supported Systems:
- Atari 2600 (A26) ([stella](https://docs.libretro.com/library/stella/))
- Atari 7800 (A78) ([prosystem](https://docs.libretro.com/library/prosystem/))
- Atari Lynx (Lynx) ([handy](https://docs.libretro.com/library/handy/))
- Nintendo (NES) ([fceumm](https://docs.libretro.com/library/fceumm/))
- Super Nintendo (SNES) ([snes9x](https://docs.libretro.com/library/snes9x/))
- Game Boy (GB) ([gambatte](https://docs.libretro.com/library/gambatte/))
- Game Boy Color (GBC) ([gambatte](https://docs.libretro.com/library/gambatte/))
- Game Boy Advance (GBA) ([mgba](https://docs.libretro.com/library/mgba/))
- Sega Genesis (aka Megadrive) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Sega CD (aka Mega CD) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Sega Master System (SMS) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Sega Game Gear (GG) ([genesis_plus_gx](https://docs.libretro.com/library/genesis_plus_gx/))
- Nintendo 64 (N64) ([mupen64plus](https://docs.libretro.com/library/mupen64plus/))
- PlayStation (PSX) ([PCSX-ReARMed](https://docs.libretro.com/library/pcsx_rearmed/))
- PlayStation Portable (PSP) ([ppsspp](https://docs.libretro.com/library/ppsspp/))
- FinalBurn Neo (Arcade) ([fbneo](https://github.com/libretro/FBNeo/))
- Nintendo DS (NDS) ([desmume](https://docs.libretro.com/library/desmume/)/[MelonDS](https://docs.libretro.com/library/melonds/))
- NEC PC Engine (PCE) ([beetle_pce_fast](https://docs.libretro.com/library/beetle_pce_fast/))
- Neo Geo Pocket (NGP) ([mednafen_ngp](https://docs.libretro.com/library/beetle_neopop/))
- Neo Geo Pocket Color (NGC) ([mednafen_ngp](https://docs.libretro.com/library/beetle_neopop/))
- WonderSwan (WS) ([beetle_cygne](https://docs.libretro.com/library/beetle_cygne/))
- WonderSwan Color (WSC) ([beetle_cygne](https://docs.libretro.com/library/beetle_cygne/))
- Nintendo 3DS (3DS) ([citra](https://docs.libretro.com/library/citra/))

### Features:
- Android TV support
- Automatically save and restore game states.
- ROMs scanning and indexing
- Optimized touch controls
- Quick save/load
- Support for Zipped ROMs
- Display simulation (LCD/CRT)
- Gamepad support
- Local multiplayer
- Tilt input
- Customizable touch controls (size and position)
- Cloud save sync
- HD mode

### Languages:
You can help translate Lemuroid in your native language by going here: https://crowdin.com/project/lemuroid
