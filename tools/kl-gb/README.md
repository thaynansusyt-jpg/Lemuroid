# GB/GBC LAN cable

Pinned upstream: libretro/gambatte-libretro@d9d6cd06382d1ced30de34d56d3609452323dab1 (GPL-2.0-or-later).

prepare.py enables the frontend handoff through existing network core options, authenticates peers with the room key and bounds TCP operations. Private copies are used only for multiplayer; the bundled singleplayer core is retained.

The serial receive fix consumes incoming bits after each partial shift. Otherwise, a game polling SC repeatedly can receive a byte containing copies of the first bit instead of the full peer byte.

The two-process test generates original minimal GB ROMs and checks actual emulated serial exchanges. No commercial game or BIOS is included. Passing it does not establish Pokémon compatibility.
