#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>
#include <cassert>
#include <thread>
#include <cstring>
#include <cstdio>
#include "kl-socket.h"
int main() {
 int fd[2]; assert(socketpair(AF_UNIX, SOCK_STREAM, 0, fd)==0);
 std::thread sender([&]{ unsigned char first=0x12, second=0x34; assert(send(fd[1], &first, 1, 0)==1); usleep(5000); assert(send(fd[1], &second, 1, 0)==1); });
 unsigned char bytes[2]={}; assert(kl_gb_transfer(fd[0],bytes,2,false)); assert(bytes[0]==0x12 && bytes[1]==0x34); sender.join();
 long long start=kl_gb_now(); assert(!kl_gb_transfer(fd[0],bytes,2,false,20)); assert(kl_gb_now()-start < 200);
 close(fd[1]); assert(!kl_gb_transfer(fd[0],bytes,2,false)); assert(!kl_gb_transfer(fd[0],bytes,2,true)); close(fd[0]);
 puts("GB/GBC: fragmented serial message, bounded timeout, disconnect and SIGPIPE protection passed.");
}
