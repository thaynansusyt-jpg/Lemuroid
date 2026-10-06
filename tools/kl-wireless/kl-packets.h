/* KL Play LAN frontend for the pinned gpSP wireless core.
 * SPDX-License-Identifier: MPL-2.0
 * All callbacks run on the libretro emulation thread. No background callbacks.
 */
#ifndef KL_PACKETS_H
#define KL_PACKETS_H
#include "libretro.h"
#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netinet/tcp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <time.h>
#include <unistd.h>

#define KL_MAX_PACKET 65536u
#define KL_MAX_QUEUE (4u * 1024u * 1024u)
#define KL_FRAME_WINDOW 3u
struct kl_chunk { struct kl_chunk *next; size_t size, offset; unsigned char bytes[]; };
static retro_environment_t kl_parent;
static retro_audio_sample_batch_t kl_audio_parent;
static bool kl_audio_muted;
static size_t RETRO_CALLCONV kl_audio_batch(const int16_t *data, size_t frames) {
    return kl_audio_muted || !kl_audio_parent ? frames : kl_audio_parent(data, frames);
}
static struct retro_netpacket_callback kl_callbacks;
static int kl_fd = -1, kl_listener = -1, kl_role, kl_connecting;
static bool kl_enabled, kl_started, kl_authenticated, kl_failed;
static char kl_mode[16];
static char kl_key[33], kl_address[16], kl_status_path[4096], kl_last_status[256];
static unsigned char kl_rx[KL_MAX_PACKET + 4];
static size_t kl_rx_size, kl_queued;
static struct kl_chunk *kl_head, *kl_tail;
static uint64_t kl_began, kl_attempt, kl_connected_at;
static uint64_t kl_frame, kl_proposal, kl_peer_frame, kl_waiting_since;

static uint64_t kl_now(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000 + (uint64_t)ts.tv_nsec / 1000000;
}
static void kl_status(const char *message) {
    if (!kl_status_path[0] || !message || !strcmp(message, kl_last_status)) return;
    snprintf(kl_last_status, sizeof(kl_last_status), "%s", message);
    char temporary[4120];
    snprintf(temporary, sizeof(temporary), "%s.tmp", kl_status_path);
    FILE *f = fopen(temporary, "w");
    if (f) { fputs(kl_last_status, f); fclose(f); rename(temporary, kl_status_path); }
}
static void kl_close_sockets(void) {
    if (kl_fd >= 0) close(kl_fd);
    if (kl_listener >= 0) close(kl_listener);
    kl_fd = kl_listener = -1;
    while (kl_head) { struct kl_chunk *next = kl_head->next; free(kl_head); kl_head = next; }
    kl_tail = NULL; kl_queued = kl_rx_size = 0;
}
static void kl_fail(const char *message) {
    if (kl_failed) return;
    kl_failed = true;
    kl_audio_muted = true;
    kl_close_sockets();
    if (kl_started) {
        kl_started = false;
        if (!kl_role && kl_callbacks.disconnected) kl_callbacks.disconnected((uint16_t)(1-kl_role));
        if (kl_callbacks.stop) kl_callbacks.stop();
    }
    kl_status(message);
}
static void kl_shutdown(void) {
    if (kl_started && kl_callbacks.stop) kl_callbacks.stop();
    kl_started = false;
    kl_close_sockets();
    kl_enabled = kl_authenticated = kl_failed = kl_audio_muted = false;
    kl_connecting = 0;
    kl_frame = kl_proposal = kl_peer_frame = kl_waiting_since = 0;
    memset(&kl_callbacks, 0, sizeof(kl_callbacks));
}
static bool kl_nonblocking(int fd) {
    int flags = fcntl(fd, F_GETFL, 0), yes = 1;
    if (flags < 0 || fcntl(fd, F_SETFL, flags | O_NONBLOCK) < 0) return false;
    setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &yes, sizeof(yes));
    return true;
}
static void kl_flush(void) {
    while (kl_head && kl_fd >= 0 && !kl_connecting) {
        struct kl_chunk *p = kl_head;
        ssize_t n = send(kl_fd, p->bytes + p->offset, p->size - p->offset, MSG_NOSIGNAL);
        if (n < 0 && errno == EINTR) continue;
        if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) return;
        if (n <= 0) { kl_fail("Conexão perdida. Saia do jogo e crie outra sala."); return; }
        p->offset += (size_t)n;
        if (p->offset == p->size) {
            kl_queued -= p->size; kl_head = p->next;
            if (!kl_head) kl_tail = NULL;
            free(p);
        }
    }
}
static void kl_enqueue(const void *data, size_t size, bool framed) {
    size_t total = size + (framed ? 4 : 0);
    if (kl_failed || kl_fd < 0) return;
    if (!size || (framed && size > KL_MAX_PACKET) || total > KL_MAX_QUEUE - kl_queued) {
        kl_fail("A rede não acompanhou o jogo. Reconecte os celulares."); return;
    }
    struct kl_chunk *p = malloc(sizeof(*p) + total);
    if (!p) { kl_fail("Memória insuficiente para o cabo Wi-Fi."); return; }
    p->next = NULL; p->offset = 0; p->size = total;
    if (framed) { uint32_t length = htonl((uint32_t)size); memcpy(p->bytes, &length, 4); }
    memcpy(p->bytes + (framed ? 4 : 0), data, size);
    if (kl_tail) kl_tail->next = p; else kl_head = p;
    kl_tail = p; kl_queued += total; kl_flush();
}
static void RETRO_CALLCONV kl_send(int flags, const void *data, size_t size, uint16_t peer) {
    (void)flags;
    if (!size) { kl_flush(); return; }
    if (!data) { kl_fail("Pacote do núcleo inválido."); return; }
    if (peer != (uint16_t)(1-kl_role) && peer != RETRO_NETPACKET_BROADCAST) return;
    kl_enqueue(data, size, true);
}
static void RETRO_CALLCONV kl_poll(void);
static uint32_t kl_protocol(void) {
    uint32_t hash = 2166136261u;
    const unsigned char *p = (const unsigned char *)(kl_callbacks.protocol_version ? kl_callbacks.protocol_version : "unknown");
    while (*p) { hash ^= *p++; hash *= 16777619u; }
    p = (const unsigned char *)kl_mode;
    while (*p) { hash ^= *p++; hash *= 16777619u; }
    return hash;
}
static void kl_handshake(void) {
    unsigned char hello[41];
    memcpy(hello, "KLR4", 4); memcpy(hello + 4, kl_key, 32); hello[36] = (unsigned char)kl_role;
    uint32_t protocol = htonl(kl_protocol()); memcpy(hello+37, &protocol, 4);
    kl_connected_at = kl_now();
    kl_enqueue(hello, sizeof(hello), false);
}
static void kl_parse(void) {
    if (!kl_authenticated) {
        if (kl_rx_size < 41) return;
        uint32_t protocol; memcpy(&protocol, kl_rx+37, 4);
        if (memcmp(kl_rx, "KLR4", 4) || memcmp(kl_rx+4, kl_key, 32) || kl_rx[36] != 1-kl_role || ntohl(protocol) != kl_protocol()) {
            kl_fail("Sala incompatível. Use a mesma versão do KL Play nos dois celulares."); return;
        }
        kl_rx_size -= 41; memmove(kl_rx, kl_rx+41, kl_rx_size);
        kl_authenticated = true; kl_started = true;
        kl_status("Adaptador conectado • entre no multiplayer dentro do jogo.");
        kl_callbacks.start((uint16_t)kl_role, kl_send, kl_poll);
        if (!kl_role && kl_callbacks.connected && !kl_callbacks.connected(1)) {
            kl_fail("O núcleo recusou a conexão do visitante."); return;
        }
    }
    unsigned delivered = 0;
    while (kl_rx_size >= 4 && delivered++ < 32 && !kl_failed) {
        uint32_t size; memcpy(&size, kl_rx, 4); size = ntohl(size);
        bool frame_control = size == 0x80000008u;
        if (frame_control) size = 8;
        if (!size || size > KL_MAX_PACKET) { kl_fail("Pacote de rede inválido."); return; }
        if (kl_rx_size < size + 4) break;
        if (frame_control) {
            uint32_t hi, lo; memcpy(&hi, kl_rx+4, 4); memcpy(&lo, kl_rx+8, 4);
            uint64_t frame = ((uint64_t)ntohl(hi) << 32) | ntohl(lo);
            if (!frame || frame < kl_peer_frame || frame > kl_frame + KL_FRAME_WINDOW + 1) {
                kl_fail("Sincronização inválida. Reabra o jogo nos dois celulares."); return;
            }
            kl_peer_frame = frame;
        } else kl_callbacks.receive(kl_rx+4, size, (uint16_t)(1-kl_role));
        if (kl_failed) return;
        kl_rx_size -= size+4; memmove(kl_rx, kl_rx+size+4, kl_rx_size);
    }
}
static void RETRO_CALLCONV kl_poll(void) {
    if (kl_failed || kl_fd < 0 || kl_connecting) return;
    kl_flush();
    kl_parse();
    for (unsigned i = 0; i < 32 && !kl_failed; ++i) {
        size_t available = sizeof(kl_rx) - kl_rx_size;
        if (!available) { kl_fail("Fila de entrada cheia."); break; }
        ssize_t n = recv(kl_fd, kl_rx+kl_rx_size, available, 0);
        if (n < 0 && errno == EINTR) continue;
        if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) break;
        if (n <= 0) { kl_fail("O outro celular saiu. Saia do jogo e reconecte."); break; }
        kl_rx_size += (size_t)n; kl_parse();
    }
    if (!kl_authenticated && kl_connected_at && kl_now()-kl_connected_at > 5000)
        kl_fail("O outro celular não confirmou a sala. Reconecte.");
}
static void kl_tick(void) {
    if (!kl_enabled || kl_failed) return;
    uint64_t now = kl_now();
    if (!kl_started && now-kl_began > 90000) { kl_fail("Tempo esgotado. Abra o mesmo jogo nos dois celulares e tente novamente."); return; }
    if (kl_role == 0 && kl_fd < 0) {
        kl_fd = accept(kl_listener, NULL, NULL);
        if (kl_fd >= 0) {
            if (!kl_nonblocking(kl_fd)) { kl_fail("Falha no socket Wi-Fi."); return; }
            close(kl_listener); kl_listener = -1; kl_handshake();
        } else if (errno != EAGAIN && errno != EWOULDBLOCK && errno != EINTR) {
            kl_fail("Não foi possível aceitar o visitante."); return;
        }
    } else if (kl_role == 1 && kl_fd < 0 && now >= kl_attempt) {
        kl_attempt = now+500;
        kl_fd = socket(AF_INET, SOCK_STREAM, 0);
        if (kl_fd < 0 || !kl_nonblocking(kl_fd)) { kl_fail("Falha no socket Wi-Fi."); return; }
        struct sockaddr_in address = {0}; address.sin_family = AF_INET; address.sin_port = htons(55343);
        inet_pton(AF_INET, kl_address, &address.sin_addr);
        int result = connect(kl_fd, (struct sockaddr*)&address, sizeof(address));
        if (!result) kl_handshake();
        else if (errno == EINPROGRESS) { kl_connecting = 1; kl_connected_at = now; }
        else { close(kl_fd); kl_fd = -1; }
    }
    if (kl_connecting) {
        fd_set writable; FD_ZERO(&writable); FD_SET(kl_fd, &writable); struct timeval timeout = {0};
        int ready = select(kl_fd+1, NULL, &writable, NULL, &timeout);
        if (ready > 0) {
            int error = 0; socklen_t size = sizeof(error); getsockopt(kl_fd, SOL_SOCKET, SO_ERROR, &error, &size);
            kl_connecting = 0;
            if (error) { close(kl_fd); kl_fd = -1; } else kl_handshake();
        } else if (now-kl_connected_at > 3000) { close(kl_fd); kl_fd = -1; kl_connecting = 0; }
    }
    kl_poll();
    if (kl_started && !kl_failed && kl_callbacks.poll) kl_callbacks.poll();
}
/* Pipeline a bounded number of frames instead of a network barrier on every
 * display tick. Independent Android display phases must not halve game speed.
 * Require the peer's first proposal before starting; a paused peer then limits
 * local progress to three frames. Core packets remain ordered and callbacks
 * stay on this thread. Never execute catch-up frames when resuming. */
static bool kl_frame_ready(void) {
    if (!kl_enabled) return true;
    if (kl_failed || !kl_started) return false;
    if (kl_proposal == kl_frame) {
        uint32_t message[3];
        kl_proposal = kl_frame + 1;
        message[0] = htonl(0x80000008u);
        message[1] = htonl((uint32_t)(kl_proposal >> 32));
        message[2] = htonl((uint32_t)kl_proposal);
        kl_enqueue(message, sizeof(message), false);
    }
    uint64_t deadline = kl_now() + 8;
    do {
        kl_poll();
        if (kl_failed) return false;
        if (kl_peer_frame && kl_proposal <= kl_peer_frame + KL_FRAME_WINDOW - 1) {
            kl_frame = kl_proposal;
            if (kl_waiting_since) kl_status("Cabo GBA sincronizado • 2 jogadores.");
            kl_waiting_since = 0;
            return true;
        }
        if (kl_fd < 0) return false;
        fd_set readable; FD_ZERO(&readable); FD_SET(kl_fd, &readable);
        struct timeval timeout = {0, 1000};
        select(kl_fd+1, &readable, NULL, NULL, &timeout);
    } while (kl_now() < deadline);
    if (!kl_waiting_since) kl_waiting_since = kl_now();
    if (kl_now() - kl_waiting_since >= 500)
        kl_status("Aguardando o outro jogador • pausa ou rede lenta.");
    return false;
}
static bool kl_register(const struct retro_netpacket_callback *callbacks) {
    const char *directory = NULL;
    if (!callbacks || !callbacks->start || !callbacks->receive || !kl_parent(RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY, &directory) || !directory) return false;
    char path[4096]; snprintf(path, sizeof(path), "%s/kl-link-session.cfg", directory);
    FILE *f = fopen(path, "r"); if (!f) return false;
    int role = -1; char ip[16] = {0}, key[33] = {0}, mode[16] = {0};
    int fields = fscanf(f, "%d %15s %32s %15s", &role, ip, key, mode); fclose(f);
    if (fields != 4 || (strcmp(mode, "POKEMON") && strcmp(mode, "WIRELESS")) || (role != 0 && role != 1) || strlen(key) != 32 || strspn(key, "0123456789abcdef") != 32) return false;
    struct in_addr parsed; if (inet_pton(AF_INET, ip, &parsed) != 1) return false;
    kl_shutdown(); kl_role = role; strcpy(kl_key, key); strcpy(kl_address, ip); strcpy(kl_mode, mode);
    kl_callbacks = *callbacks; kl_enabled = true; kl_began = kl_now();
    snprintf(kl_status_path, sizeof(kl_status_path), "%s/kl-link-status.txt", directory);
    kl_last_status[0] = 0;
    if (!role) {
        kl_listener = socket(AF_INET, SOCK_STREAM, 0); int yes = 1;
        if (kl_listener >= 0) setsockopt(kl_listener, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(yes));
        struct sockaddr_in address = {0}; address.sin_family = AF_INET; address.sin_port = htons(55343); address.sin_addr.s_addr = htonl(INADDR_ANY);
        if (kl_listener < 0 || !kl_nonblocking(kl_listener) || bind(kl_listener, (struct sockaddr*)&address, sizeof(address)) || listen(kl_listener, 1)) {
            kl_fail("Não foi possível abrir o cabo Wi-Fi. Saia do jogo e tente novamente."); return true;
        }
    }
    kl_status(role ? "Aguardando o anfitrião abrir o mesmo jogo…" : "Aguardando o visitante abrir o mesmo jogo…");
    return true;
}
static bool RETRO_CALLCONV kl_environment(unsigned command, void *data) {
    if (command == RETRO_ENVIRONMENT_SET_NETPACKET_INTERFACE) return kl_register(data);
    if (kl_enabled && command == RETRO_ENVIRONMENT_SET_MESSAGE_EXT && data) {
        const struct retro_message_ext *message = data;
        if (message->level == RETRO_LOG_ERROR) {
            kl_audio_muted = true;
            kl_status(message->msg);
        }
        else if (message->msg && strstr(message->msg, "GBA Wi-Fi Link ready:"))
            kl_status("Cabo GBA sincronizado • 2 jogadores. Abra o multiplayer dentro do jogo.");
    }
    return kl_parent(command, data);
}
#endif
