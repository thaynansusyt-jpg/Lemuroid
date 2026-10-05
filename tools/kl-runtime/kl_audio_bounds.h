#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
struct KlAudioChunk { int32_t outputFrames; int32_t inputFrames; };
inline KlAudioChunk klAudioChunk(int32_t remaining, int32_t capacity, double ratio, double& fractional) {
    const int32_t safeOutput = std::max(1, static_cast<int32_t>(std::floor((capacity - 1.0) / ratio)));
    const int32_t output = std::min(remaining, safeOutput);
    const double wanted = output * ratio + fractional;
    const int32_t input = std::clamp(static_cast<int32_t>(std::round(wanted)), 0, capacity);
    fractional = wanted - input;
    return {output, input};
}
