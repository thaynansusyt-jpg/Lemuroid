/* SPDX-License-Identifier: GPL-2.0-or-later
 * Bounded, exact TCP transfers: a read() may return only part of a serial message. */
#include <time.h>
#include <stdint.h>
#include <poll.h>
#include <fcntl.h>
#include <netinet/tcp.h>
static long long kl_gb_now() {
    timespec now; clock_gettime(CLOCK_MONOTONIC, &now);
    return (long long)now.tv_sec * 1000 + now.tv_nsec / 1000000;
}
static bool kl_gb_transfer(int fd, void *data, size_t size, bool writing, int timeout=300) {
    unsigned char *bytes = (unsigned char*)data;
    long long deadline = kl_gb_now() + timeout;
    while (size) {
        int remaining = (int)(deadline - kl_gb_now());
        if (remaining <= 0) return false;
        pollfd ready = {fd, (short)(writing ? POLLOUT : POLLIN), 0};
        int result = poll(&ready, 1, remaining);
        if (result < 0 && errno == EINTR) continue;
        if (result <= 0) return false;
        ssize_t count = writing ? ::send(fd, bytes, size, MSG_NOSIGNAL | MSG_DONTWAIT) : recv(fd, bytes, size, MSG_DONTWAIT);
        if (count < 0 && (errno == EINTR || errno == EAGAIN || errno == EWOULDBLOCK)) continue;
        if (count <= 0) return false;
        bytes += count; size -= (size_t)count;
    }
    return true;
}
static int kl_gb_connect(int fd, const sockaddr *address, socklen_t size) {
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags < 0 || fcntl(fd, F_SETFL, flags | O_NONBLOCK) < 0) return -1;
    int result = connect(fd, address, size);
    if (result < 0 && errno == EINPROGRESS) {
        pollfd ready = {fd, POLLOUT, 0};
        if (poll(&ready, 1, 300) > 0) {
            int error = 0; socklen_t length = sizeof(error);
            if (!getsockopt(fd, SOL_SOCKET, SO_ERROR, &error, &length) && !error) result = 0;
        }
    }
    fcntl(fd, F_SETFL, flags);
    return result;
}
