# GBA Pokémon / wireless LAN frontend

Pinned upstream: libretro/gpsp@5819380c2ffb0900219d700a382ee68c464ebb99; upstream license preserved.

prepare.py connects the core's actual libretro NETPACKET callbacks to a two-player TCP transport derived from KL's existing mGBA frontend. It supports gpSP serial modes mul_poke and rfu. The core protocol and selected mode are checked during the room-key handshake. Packet and queue sizes are bounded. The callbacks run on the emulator thread.

This is not internet matchmaking. The RFU implementation must be tested against each hack version on real Android devices. The automated tests cover the frontend transport, not Pokémon gameplay.

KLR4 pipelines up to three emulation frames, rather than requiring a peer
proposal for every display tick. Both peers must propose the first frame before
gameplay starts. When a peer pauses, the other can advance at most three frames
and then waits; resume never runs a batch of catch-up frames. KLR3 peers are
rejected. `test-pacing.c` exercises two frontends at 17 ms intervals with a 9 ms
phase offset; `test-transport.c` checks the pause bound and fragmented proposals.
