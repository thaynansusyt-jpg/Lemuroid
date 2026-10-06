/* Real core + real TCP integration; fixture is upstream CC0 homebrew. */
#include "libretro.h"
#include <dlfcn.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
static const char *directory;
static unsigned frames, errors;
static void RETRO_CALLCONV log_message(enum retro_log_level level, const char *format, ...) {
    char message[2048];
    va_list args; va_start(args, format); vsnprintf(message, sizeof(message), format, args); va_end(args);
    fputs(message, stderr);
    (void)level;
}
static bool RETRO_CALLCONV environment(unsigned command, void *data) {
    switch (command) {
    case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
    case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: *(const char**)data = directory; return true;
    case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: ((struct retro_log_callback*)data)->log = log_message; return true;
    case RETRO_ENVIRONMENT_GET_CAN_DUPE: *(bool*)data = true; return true;
    case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: *(bool*)data = false; return true;
    case RETRO_ENVIRONMENT_SET_MESSAGE_EXT: fprintf(stderr, "MESSAGE: %s\n", ((struct retro_message_ext*)data)->msg); return true;
    case RETRO_ENVIRONMENT_SET_MESSAGE: fprintf(stderr, "MESSAGE: %s\n", ((struct retro_message*)data)->msg); return true;
    case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
    case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
    case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: return true;
    default: return false;
    }
}
static void RETRO_CALLCONV video(const void *data, unsigned width, unsigned height, size_t pitch) {
    (void)data; (void)width; (void)height; (void)pitch; ++frames;
}
static size_t RETRO_CALLCONV audio(const int16_t *data, size_t frames) { (void)data; return frames; }
static void RETRO_CALLCONV input_poll(void) {}
static int16_t RETRO_CALLCONV input(unsigned port, unsigned device, unsigned index, unsigned id) {
    (void)port; (void)device; (void)index; (void)id; return 0;
}
#define SYMBOL(name) __typeof__(&name) name = dlsym(library, #name); if (!name) return 3
int main(int argc, char **argv) {
    if (argc != 4) return 2;
    directory = argv[2];
    void *library = dlopen(argv[1], RTLD_NOW | RTLD_LOCAL);
    if (!library) { fprintf(stderr, "%s\n", dlerror()); return 3; }
    SYMBOL(retro_set_environment); SYMBOL(retro_set_video_refresh); SYMBOL(retro_set_audio_sample_batch);
    SYMBOL(retro_set_input_poll); SYMBOL(retro_set_input_state); SYMBOL(retro_init); SYMBOL(retro_load_game);
    SYMBOL(retro_get_memory_data); SYMBOL(retro_run); SYMBOL(retro_unload_game); SYMBOL(retro_deinit);
    retro_set_environment(environment); retro_set_video_refresh(video); retro_set_audio_sample_batch(audio);
    retro_set_input_poll(input_poll); retro_set_input_state(input); retro_init();
    FILE *f = fopen(argv[3], "rb"); if (!f) return 4;
    fseek(f, 0, SEEK_END); long size = ftell(f); rewind(f);
    void *rom = malloc((size_t)size); if (!rom || fread(rom, 1, (size_t)size, f) != (size_t)size) return 4;
    fclose(f);
    struct retro_game_info info = {.path = argv[3], .data = rom, .size = (size_t)size};
    if (!retro_load_game(&info)) return 5;
    // Pace both independent frontends; the core polls TCP while waiting for remote input.
    for (unsigned i = 0; i < 400; ++i) { retro_run(); usleep(16000); }
    unsigned char *ram = retro_get_memory_data(RETRO_MEMORY_SYSTEM_RAM);
    if (!ram || ram[1] != 0xA5 || ram[0] != (strstr(argv[3], "host") ? 0x72 : 0x31)) {
        fprintf(stderr, "Serial failed: received=%02x flag=%02x\n", ram ? ram[0] : 0, ram ? ram[1] : 0); return 7;
    }
    fprintf(stderr, "GB serial exchanged actual emulated bytes: %02x\n", ram[0]);
    retro_unload_game(); retro_deinit(); dlclose(library); free(rom);
    fprintf(stderr, "Rendered %u frames\n", frames);
    return errors ? 6 : 0;
}
