#include "kl-packets.h"
#include <assert.h>
static const char *test_directory;
static unsigned starts, stops, disconnects, receives;
static bool RETRO_CALLCONV test_environment(unsigned command, void *data) {
    if (command != RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY) return false;
    *(const char**)data = test_directory; return true;
}
static void RETRO_CALLCONV test_start(uint16_t id, retro_netpacket_send_t send_packet, retro_netpacket_poll_receive_t poll_receive) {
    (void)id; (void)send_packet; (void)poll_receive; ++starts;
}
static void RETRO_CALLCONV test_stop(void) { ++stops; }
static void RETRO_CALLCONV test_disconnect(uint16_t id) { (void)id; ++disconnects; }
static bool RETRO_CALLCONV test_connected(uint16_t id) { return id == 1; }
static void RETRO_CALLCONV test_receive(const void *data, size_t size, uint16_t id) {
    assert(size == 2 && id == 1 && !memcmp(data, "ok", 2)); ++receives;
}
static void run_case(unsigned kind) {
    starts = stops = disconnects = receives = 0;
    struct retro_netpacket_callback callbacks = {
        .start = test_start, .receive = test_receive, .stop = test_stop,
        .connected = test_connected, .disconnected = test_disconnect,
    };
    assert(kl_register(&callbacks));
    assert(!kl_audio_muted);
    int peer = socket(AF_INET, SOCK_STREAM, 0); assert(peer >= 0);
    struct sockaddr_in address = {0}; address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK); address.sin_port = htons(55343);
    assert(!connect(peer, (struct sockaddr*)&address, sizeof(address)));
    unsigned char hello[41]; memcpy(hello, "KLR3", 4); memcpy(hello+4, kl_key, 32); hello[36] = 1; uint32_t protocol = htonl(kl_protocol()); memcpy(hello+37, &protocol, 4);
    if (kind == 0) hello[4] ^= 1;
    if (kind == 3) hello[37] ^= 1;
    // Split header deliberately: TCP is a stream, not a packet transport.
    assert(send(peer, hello, 7, MSG_NOSIGNAL) == 7); kl_tick();
    assert(!starts);
    assert(send(peer, hello+7, 34, MSG_NOSIGNAL) == 34);
    uint32_t length = htonl(kind == 1 ? 65537 : 2);
    assert(send(peer, &length, 4, MSG_NOSIGNAL) == 4);
    if (kind == 2) assert(send(peer, "ok", 2, MSG_NOSIGNAL) == 2);
    for (unsigned i=0; i<100 && !kl_failed; ++i) {
        kl_tick(); usleep(1000);
        if (kind == 2 && receives) { close(peer); peer = -1; }
    }
    assert(kl_failed && kl_audio_muted);
    assert(starts == (kind == 1 || kind == 2)); assert(stops == starts && disconnects == starts);
    assert(receives == (kind == 2)); assert(kl_fd == -1 && kl_listener == -1);
    if (peer >= 0) close(peer);
    kl_shutdown();
    assert(!kl_audio_muted);
}
static void test_frame_pause(void) {
    int sockets[2]; assert(!socketpair(AF_UNIX, SOCK_STREAM, 0, sockets));
    kl_shutdown(); kl_fd = sockets[0]; assert(kl_nonblocking(kl_fd));
    kl_enabled = kl_started = kl_authenticated = true;
    kl_callbacks.receive = test_receive;
    assert(!kl_frame_ready()); assert(kl_frame == 0 && kl_proposal == 1);
    unsigned char proposal[12]; assert(read(sockets[1], proposal, 12) == 12);
    for (unsigned i=0;i<4;i++) assert(!kl_frame_ready());
    assert(kl_frame == 0 && kl_queued == 0); // no catch-up frames or duplicate proposals
    assert(kl_nonblocking(sockets[1]));
    assert(recv(sockets[1], proposal, 12, 0) == -1 && errno == EAGAIN);
    uint32_t control[3] = {htonl(0x80000008u), 0, htonl(1)};
    assert(write(sockets[1], control, 5) == 5); assert(!kl_frame_ready());
    assert(write(sockets[1], ((char*)control)+5, 7) == 7);
    assert(kl_frame_ready()); assert(kl_frame == 1);
    assert(!kl_frame_ready()); assert(kl_frame == 1 && kl_proposal == 2);
    control[2] = htonl(2); assert(write(sockets[1], control, 12) == 12);
    assert(kl_frame_ready()); assert(kl_frame == 2);
    control[2] = htonl(99); assert(write(sockets[1], control, 12) == 12);
    assert(!kl_frame_ready()); assert(kl_failed);
    close(sockets[1]); kl_shutdown(); assert(!kl_frame && !kl_peer_frame);
}
int main(void) {
    char temporary[] = "/tmp/kl-transport-XXXXXX"; test_directory = mkdtemp(temporary); assert(test_directory);
    char config[4096]; snprintf(config, sizeof(config), "%s/kl-link-session.cfg", test_directory);
    FILE *f = fopen(config, "w"); assert(f);
    fputs("0 127.0.0.1 0123456789abcdef0123456789abcdef WIRELESS\n", f); fclose(f);
    kl_parent = test_environment;
    test_frame_pause();
    run_case(0); run_case(1); run_case(2); run_case(3);
    starts = stops = disconnects = 0;
    kl_role = 1; kl_started = true; kl_failed = false;
    kl_callbacks.stop = test_stop; kl_callbacks.disconnected = test_disconnect;
    kl_send(RETRO_NETPACKET_FLUSH_HINT, NULL, 0, RETRO_NETPACKET_BROADCAST);
    assert(!kl_failed);
    kl_fail("test disconnect"); assert(stops == 1 && disconnects == 0); kl_shutdown();
    unlink(config); snprintf(config, sizeof(config), "%s/kl-link-status.txt", test_directory); unlink(config); rmdir(test_directory);
    puts("Transport: split TCP header, room key, oversized packet, disconnect and bounded teardown passed.");
    return 0;
}
