from pathlib import Path
import sys
p=Path(sys.argv[1]); cpp=p/'libretrodroid/src/main/cpp'
def change(file, old, new):
 s=file.read_text(); assert old in s, f'Pinned runtime changed: {file}'; file.write_text(s.replace(old,new,1))
change(cpp/'audio.h', '#include <array>', '#include <array>\n#include <atomic>')
change(cpp/'audio.h', 'double playbackSpeed = 1.0;', 'std::atomic<double> playbackSpeed {1.0};\n    int32_t temporaryCapacityFrames = 0;')
change(cpp/'audio.cpp', 'temporaryAudioBuffer = std::unique_ptr<int16_t[]>(new int16_t[audioBufferSize]);', 'temporaryCapacityFrames = std::max(audioBufferSize, 4096);\n        temporaryAudioBuffer = std::make_unique<int16_t[]>(temporaryCapacityFrames * 2);')
change(cpp/'audio.cpp', '#include <memory>', '#include <memory>\n#include <algorithm>\n#include <cstring>\n#include "kl_audio_bounds.h"')
change(cpp/'audio.cpp', 'fifoBuffer->write(data, frames * 2);', 'if (fifoBuffer && data && frames > 0) fifoBuffer->write(data, frames * 2);')
change(cpp/'audio.cpp', 'playbackSpeed = newPlaybackSpeed;', 'playbackSpeed.store(std::clamp(newPlaybackSpeed, 1.0, 8.0), std::memory_order_relaxed);')
s=(cpp/'audio.cpp').read_text(); start=s.index('    double dynamicBufferFactor',s.index('Audio::onAudioReady')); end=s.index('    return oboe::DataCallbackResult::Continue;',start)
s=s[:start]+'''    if (numFrames <= 0) return oboe::DataCallbackResult::Continue;
    auto output = reinterpret_cast<int16_t *>(audioData);
    if (!fifoBuffer || !temporaryAudioBuffer) {
        std::memset(output, 0, numFrames * 2 * sizeof(int16_t));
        return oboe::DataCallbackResult::Continue;
    }
    const double ratio = std::clamp(baseConversionFactor *
        computeDynamicBufferConversionFactor(0.001 * numFrames) *
        playbackSpeed.load(std::memory_order_relaxed), 0.001, 128.0);
    int32_t remaining = numFrames;
    while (remaining > 0) {
        const auto chunk = klAudioChunk(remaining, temporaryCapacityFrames, ratio, framesToSubmit);
        const int32_t submitted = chunk.inputFrames;
        // Always clear before the FIFO read: underruns must produce silence, not stale samples.
        std::memset(temporaryAudioBuffer.get(), 0, submitted * 2 * sizeof(int16_t));
        fifoBuffer->readNow(temporaryAudioBuffer.get(), submitted * 2);
        if (submitted > 0) resampler.resample(temporaryAudioBuffer.get(), submitted, output, chunk.outputFrames);
        else std::memset(output, 0, chunk.outputFrames * 2 * sizeof(int16_t));
        output += chunk.outputFrames * 2;
        remaining -= chunk.outputFrames;
    }
    if (latencyTuner) latencyTuner->tune();

'''+s[end:]; (cpp/'audio.cpp').write_text(s)
(cpp/'kl_audio_bounds.h').write_text((Path(__file__).parent/'kl_audio_bounds.h').read_text())
change(cpp/'libretrodroid.cpp', '#include \"libretrodroid.h\"', '#include \"libretrodroid.h\"\n#include <chrono>')
change(cpp/'libretrodroid.cpp','    frameSpeed = speed;\n    updateAudioSampleRateMultiplier();','    std::lock_guard<std::mutex> lock(coreLock);\n    frameSpeed = std::clamp(speed, 1u, 8u);\n    updateAudioSampleRateMultiplier();')
change(cpp/'resamplers/linearresampler.cpp','    double outputTime = 0;', '    if (sinkFrames <= 0) return;\n    if (inputFrames <= 0) {\n        std::fill(sink, sink + sinkFrames * 2, 0);\n        return;\n    }\n    double outputTime = 0;')
# Software cores only upload the last accelerated frame; all emulation/audio frames still execute.
change(cpp/'libretrodroid.h','unsigned int frameSpeed = 1;', 'unsigned int frameSpeed = 1;\n    bool presentFrame = true;')
change(cpp/'libretrodroid.cpp','    for (size_t i = 0; i < frames * frameSpeed; i++)\n        core->retro_run();','    const size_t count = frames * frameSpeed;\n    for (size_t i = 0; i < count; i++) {\n        presentFrame = (i + 1 == count);\n        core->retro_run();\n    }\n    presentFrame = true;')
change(cpp/'libretrodroid.cpp','    if (video) {\n        video->onNewFrame(data, width, height, pitch);','    if (video && (presentFrame || video->rendersInVideoCallback())) {\n        video->onNewFrame(data, width, height, pitch);')
(p/'libretrodroid/build.gradle').write_text('''apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'
android {
 namespace 'com.swordfish.libretrodroid'
 compileSdk 35
 ndkVersion '27.2.12479018'
 defaultConfig {
  minSdk 23
  externalNativeBuild { cmake { arguments '-DANDROID_STL=c++_static' } }
 }
 externalNativeBuild { cmake { version '3.22.1'; path 'src/main/cpp/CMakeLists.txt' } }
 buildTypes { release { minifyEnabled false } }
}
dependencies {
 implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.0.21'
 implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.5.1'
}
''')
print('Prepared pinned LibretroDroid 0.13.2 with bounded audio and synchronized speed changes')
