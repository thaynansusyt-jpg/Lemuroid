#include "kl-packets.h"
#include <assert.h>
#include <sys/wait.h>

/* Two independently paced frontends, offset by half a display interval.
 * A per-frame barrier turns this harmless phase offset into dropped frames. */
static void run_frontend(int fd, unsigned phase_ms) {
    kl_shutdown();
    kl_fd = fd; assert(kl_nonblocking(fd));
    kl_enabled = kl_started = kl_authenticated = true;
    usleep(phase_ms * 1000);
    uint64_t began = kl_now();
    for (unsigned tick = 0; tick < 240 && kl_frame < 120; ++tick) {
        uint64_t next = kl_now() + 17;
        kl_frame_ready();
        assert(!kl_failed);
        while (kl_now() < next) usleep(500);
    }
    uint64_t elapsed = kl_now() - began;
    printf("Pacing: %llu frames in %llu ms (phase %u ms)\n",
        (unsigned long long)kl_frame, (unsigned long long)elapsed, phase_ms);
    fflush(stdout);
    assert(kl_frame == 120 && elapsed < 2500);
    /* Keep the socket open until the other frontend finishes. */
    usleep(100000);
    close(fd);
}
int main(void) {
    int sockets[2]; assert(!socketpair(AF_UNIX, SOCK_STREAM, 0, sockets));
    pid_t child = fork(); assert(child >= 0);
    if (!child) { close(sockets[0]); run_frontend(sockets[1], 9); _exit(0); }
    close(sockets[1]); run_frontend(sockets[0], 0);
    int status; assert(waitpid(child, &status, 0) == child);
    assert(WIFEXITED(status) && WEXITSTATUS(status) == 0);
    return 0;
}
