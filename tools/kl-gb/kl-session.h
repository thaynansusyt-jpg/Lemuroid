/* SPDX-License-Identifier: GPL-2.0-or-later */
#include <arpa/inet.h>
#include <stdio.h>
#include <string.h>
static retro_environment_t kl_gb_parent;
char kl_gb_key[33];
static char kl_gb_status_path[4096];
static char kl_gb_digits[12][2];
static int kl_gb_role = -1;
void kl_gb_status(const char *message) {
    if (!kl_gb_status_path[0]) return;
    FILE *f = fopen(kl_gb_status_path, "w");
    if (f) { fputs(message, f); fclose(f); }
}
static void kl_gb_load_config() {
    const char *directory = NULL;
    if (!kl_gb_parent(RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY, &directory) || !directory) return;
    char path[4096], address[16], digits[13]; int role = -1;
    snprintf(path, sizeof(path), "%s/kl-link-session.cfg", directory);
    FILE *f = fopen(path, "r"); if (!f) return;
    int count = fscanf(f, "%d %15s %32s", &role, address, kl_gb_key); fclose(f);
    struct in_addr parsed;
    if (count != 3 || role < 0 || role > 1 || strlen(kl_gb_key) != 32 ||
        strspn(kl_gb_key, "0123456789abcdef") != 32 || inet_pton(AF_INET, address, &parsed) != 1) return;
    unsigned a,b,c,d;
    if (sscanf(address, "%u.%u.%u.%u", &a,&b,&c,&d) != 4) return;
    snprintf(digits, sizeof(digits), "%03u%03u%03u%03u", a,b,c,d);
    for (int i=0;i<12;i++) { kl_gb_digits[i][0] = digits[i]; kl_gb_digits[i][1] = 0; }
    kl_gb_role = role;
    snprintf(kl_gb_status_path, sizeof(kl_gb_status_path), "%s/kl-link-status.txt", directory);
}
static bool kl_gb_environment(unsigned command, void *data) {
    if (command == RETRO_ENVIRONMENT_GET_VARIABLE && data) {
        retro_variable *variable = (retro_variable *)data;
        if (variable->key && !strncmp(variable->key, "gambatte_gb_link_", 17)) {
            if (kl_gb_role < 0) kl_gb_load_config();
            if (kl_gb_role >= 0) {
                if (!strcmp(variable->key, "gambatte_gb_link_mode")) {
                    variable->value = kl_gb_role == 0 ? "Network Server" : "Network Client"; return true;
                }
                if (!strcmp(variable->key, "gambatte_gb_link_network_port")) { variable->value = "56400"; return true; }
                const char *prefix = "gambatte_gb_link_network_server_ip_";
                if (!strncmp(variable->key, prefix, strlen(prefix))) {
                    int digit = atoi(variable->key+strlen(prefix));
                    if (digit >= 1 && digit <= 12) { variable->value = kl_gb_digits[digit-1]; return true; }
                }
            }
        }
    }
    return kl_gb_parent(command, data);
}
