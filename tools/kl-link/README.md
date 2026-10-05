# KL Play experimental GBA cable

The normal singleplayer core remains untouched. The separate ARM64
`libkl_mgba_link.so` is reproducibly built from
[Aelvryx/mgba-wifi-link](https://github.com/Aelvryx/mgba-wifi-link), commit
`9e919b0cfbb93af7d1171570dfc6745d00eeebab`, licensed under MPL-2.0.
The KL frontend adapter is also MPL-2.0. The original repository carries
the upstream license and complete sources; modifications are in this folder.

This frontend accepts Netpacket registration only when an explicit game
launch writes its session configuration. A random per-room key is handed
to the separate game process via Intent. TCP port 55343 transports framed,
bounded packets and verifies the room key before calling the core callbacks.
It is a LAN transport, not an Internet matchmaking service or encrypted VPN.
All native callbacks execute on the emulation thread; sockets are nonblocking.

Limits: exactly two GBA players, identical effective ROMs, Android ARM64,
Multi-Pak/MULTI serial mode. No RFU/wireless adapter or Single-Pak.
Pokémon compatibility is unverified. DS/3DS services are not integrated.
Pause/background may interrupt the session: keep both games open.
Native save states, cheats, reset and fast forward are disabled for a cable
launch. Each device retains its own SRAM. A pre-session SRAM backup is kept
in the app's internal `kl-link-backups` directory. Singleplayer settings and
the existing app package/signing identity are retained.

CI runs two separate native frontends and the upstream CC0 continuous test
ROM over real TCP, requiring >200 transfers per side with zero data errors
and timeouts. This does not replace a physical Android test with commercial
games. It then builds and bundles the same patched core for Android ARM64.
