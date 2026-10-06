#pragma once
#include "libretro.h"

// The Android frontend owns an EGL OpenGL ES context, not Vulkan or desktop GL.
inline bool klSupportsHardwareContext(retro_hw_context_type type) {
    return type == RETRO_HW_CONTEXT_OPENGLES2 ||
           type == RETRO_HW_CONTEXT_OPENGLES3 ||
           type == RETRO_HW_CONTEXT_OPENGLES_VERSION;
}
