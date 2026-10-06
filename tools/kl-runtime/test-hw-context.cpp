#include "kl_hw_context.h"
#include <cassert>
#include <cstdio>
int main() {
    assert(klSupportsHardwareContext(RETRO_HW_CONTEXT_OPENGLES2));
    assert(klSupportsHardwareContext(RETRO_HW_CONTEXT_OPENGLES3));
    assert(klSupportsHardwareContext(RETRO_HW_CONTEXT_OPENGLES_VERSION));
    // Cemu's two render paths cannot be backed by this frontend.
    assert(!klSupportsHardwareContext(RETRO_HW_CONTEXT_VULKAN));
    assert(!klSupportsHardwareContext(RETRO_HW_CONTEXT_OPENGL_CORE));
    assert(!klSupportsHardwareContext(RETRO_HW_CONTEXT_OPENGL));
    puts("Hardware context contract: GLES accepted; Cemu Vulkan/desktop GL rejected");
}
