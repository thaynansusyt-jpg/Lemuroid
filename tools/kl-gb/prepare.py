from pathlib import Path
import shutil, sys
root=Path(sys.argv[1]); folder=root/'libgambatte/libretro'
p=folder/'libretro.cpp'; s=p.read_text()
old='void retro_set_environment(retro_environment_t cb)'
assert s.count(old)==1
s=s.replace(old,'#include "kl-session.h"\n\n'+old,1)
old='environ_cb = cb;'; assert s.count(old)==1
s=s.replace(old,'kl_gb_parent = cb;\n   environ_cb = kl_gb_environment;',1)
s=s.replace("void retro_run()\n{", "void retro_run()\n{\n#ifdef HAVE_NETWORK\n   gb_net_serial.tick();\n#endif", 1);p.write_text(s)
h=folder/"net_serial.h"; t=h.read_text().replace("bool start(bool is_server", "void tick();\n\t\tbool start(bool is_server",1);h.write_text(t)
p=folder/'net_serial.cpp';s=p.read_text()
s=s.replace('NetSerial::NetSerial()', '#include "kl-socket.h"\nextern char kl_gb_key[33];\nextern void kl_gb_status(const char *message);\nstatic bool kl_authenticate(int fd) {\n int yes=1; setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &yes, sizeof(yes));\n char peer[32];\n return kl_gb_transfer(fd, kl_gb_key, 32, true, 1000) && kl_gb_transfer(fd, peer, 32, false, 1000) && !memcmp(peer, kl_gb_key, 32);\n}\n\nNetSerial::NetSerial()',1)
s=s.replace('clock_t now = clock();', 'clock_t now = (clock_t)kl_gb_now();').replace('((now - lastConnectAttempt_) / CLOCKS_PER_SEC) < 5', '(now - lastConnectAttempt_) < 100').replace('lastConnectAttempt_ = clock();','lastConnectAttempt_ = (clock_t)kl_gb_now();')
s=s.replace('if (bind(fd,', 'int reuse = 1; setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));\n\t\tif (bind(fd,',1)
s=s.replace('if (connect(fd,', 'if (kl_gb_connect(fd,',1)
s=s.replace('\t\tsockfd_ = fd;', '\t\tif (!kl_authenticate(fd)) { close(fd); return false; }\n\t\tsockfd_ = fd;\n        kl_gb_status("Cabo GB/GBC conectado • entre no multiplayer do jogo.");',1)
s=s.replace('\t\tgambatte_log(RETRO_LOG_INFO, "GameLink network server connected', '\t\tif (!kl_authenticate(sockfd_)) { close(sockfd_); sockfd_ = -1; return false; }\n        kl_gb_status("Cabo GB/GBC conectado • entre no multiplayer do jogo.");\n\t\tgambatte_log(RETRO_LOG_INFO, "GameLink network server connected',1)
s=s.replace('if (write(sockfd_, buffer, 2) <= 0)', 'if (!kl_gb_transfer(sockfd_, buffer, 2, true))').replace('if (read(sockfd_, buffer, 2) <= 0)', 'if (!kl_gb_transfer(sockfd_, buffer, 2, false))')
s=s.replace('gambatte_log(RETRO_LOG_ERROR, "Error writing to socket:', 'kl_gb_status("Conexão GB/GBC perdida. Saia do jogo e reconecte.");\n        gambatte_log(RETRO_LOG_ERROR, "Error writing to socket:')
s=s.replace('gambatte_log(RETRO_LOG_ERROR, "Error reading from socket:', 'kl_gb_status("Conexão GB/GBC perdida. Saia do jogo e reconecte.");\n        gambatte_log(RETRO_LOG_ERROR, "Error reading from socket:')
s += '\nvoid NetSerial::tick() { if (!is_stopped_ && sockfd_ < 0) checkAndRestoreConnection(true); }\n'
p.write_text(s)
for name in ('kl-session.h','kl-socket.h'): shutil.copyfile(Path(__file__).with_name(name), folder/name)

p=root/'libgambatte/src/gambatte-memory.cpp'; s=p.read_text()
old='\t\t\tioamhram_[0x101] = ((ioamhram_[0x101] << (serialCnt_ - targetCnt)) |\n\t\t\t\t\t    (serialize_value_ >> (8 - (serialCnt_ - targetCnt)))) & 0xFF;'
assert s.count(old)==1
s=s.replace(old, old+'\n\t\t\tserialize_value_ = (serialize_value_ << (serialCnt_ - targetCnt)) & 0xFF;',1)
p.write_text(s)
