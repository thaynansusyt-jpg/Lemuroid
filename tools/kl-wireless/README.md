# GBA Pokémon / wireless LAN frontend

Pinned upstream: libretro/gpsp@5819380c2ffb0900219d700a382ee68c464ebb99; upstream license preserved.

prepare.py connects the core's actual libretro NETPACKET callbacks to a two-player TCP transport derived from KL's existing mGBA frontend. It supports gpSP serial modes mul_poke and rfu. The core protocol and selected mode are checked during the room-key handshake. Packet and queue sizes are bounded. The callbacks run on the emulator thread.

This is not internet matchmaking. The RFU implementation must be tested against each hack version on real Android devices. The automated tests cover the frontend transport, not Pokémon gameplay.
