from pathlib import Path
import sys
p=Path(sys.argv[1]); cpp=p/'libretrodroid/src/main/cpp'
def change(file, old, new):
 s=file.read_text(); assert old in s, f'Pinned runtime changed: {file}'; file.write_text(s.replace(old,new,1))
change(cpp/'audio.h', '#include <array>', '#include <array>\n#include <atomic>')
change(cpp/'rumble.h', '#include <array>', '#include <array>\n#include <functional>')
change(cpp/'utils/javautils.h', '#include <jni.h>', '#include <jni.h>\n#include <functional>')
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
change(cpp/'libretrodroid.cpp','    for (size_t i = 0; i < frames * frameSpeed; i++)\n        core->retro_run();','    const size_t count = frames * frameSpeed;\n    const auto started = std::chrono::steady_clock::now();\n    size_t executed = 0;\n    for (size_t i = 0; i < count; i++) {\n        const bool budgetReached = frameSpeed > 1 && i > 0 &&\n            std::chrono::steady_clock::now() - started >= std::chrono::milliseconds(16);\n        presentFrame = (i + 1 == count) || budgetReached;\n        core->retro_run();\n        executed++;\n        if (budgetReached) break;\n    }\n    presentFrame = true;\n    if (audio) audio->setPlaybackSpeed(std::max(1.0, static_cast<double>(executed) / frames));')
change(cpp/'libretrodroid.cpp','    if (video) {\n        video->onNewFrame(data, width, height, pitch);','    if (video && (presentFrame || video->rendersInVideoCallback())) {\n        video->onNewFrame(data, width, height, pitch);')
(p/'libretrodroid/build.gradle').write_text('''apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'
android {
 namespace 'com.swordfish.libretrodroid'
 compileSdk 35
 ndkVersion '27.2.12479018'
 kotlinOptions { jvmTarget = '17' }
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
# Custom dual-screen presentation is GPU-side; no frame copies/readbacks.
change(cpp/'video.h', '#include "videolayout.h"', '#include "videolayout.h"\n#include "kl_dual_layout.h"')
change(cpp/'video.h', '    VideoLayout& getLayout() { return videoLayout; }', '    VideoLayout& getLayout() { return videoLayout; }\n    std::pair<float,float> getPointerPosition(float x,float y);\n    void setKlOrientation(bool portrait) { klPortrait=portrait; isDirty=true; }\n    void invalidateKlLayout() { klSettingsLoaded=false; isDirty=true; }')
change(cpp/'video.h', '    void updateProgram();', '    void updateProgram();\n    void updateKlLayout();\n    KlDualLayout klLayout;\n    bool klPortrait=true;\n    bool klSettingsLoaded=false;\n    std::string klKind;\n    std::array<float,12> klPositions;')
change(cpp/'videolayout.h', '    int getScreenWidth()', '    Rect getViewportRect() const { return viewportRect; }\n\n    int getScreenWidth()')
change(cpp/'video.cpp', '#include "video.h"', '#include "video.h"\n#include "environment.h"\n#include <cstdlib>')
change(cpp/'video.cpp', 'void Video::updateProgram() {', '''void Video::updateKlLayout() {
    auto variable = [](const std::string& key, const char* fallback) {
        retro_variable v {key.c_str(), nullptr};
        Environment::callback_environment(RETRO_ENVIRONMENT_GET_VARIABLE, &v);
        return std::string(v.value ? v.value : fallback);
    };
    if (!klSettingsLoaded) {
        klSettingsLoaded=true;
        klKind=variable("kl_dual_kind","off");
        klPortrait=variable("kl_dual_orientation","p")=="p";
        const char* fields[]={"top_x","top_y","top_w","bottom_x","bottom_y","bottom_w"};
        const char* defaults[]={"50","0","90","50","100","70"};
        for(int orientation=0;orientation<2;orientation++) for(int i=0;i<6;i++) {
            auto value=variable(std::string(orientation ? "kl_dual_l_" : "kl_dual_p_")+fields[i],defaults[i]);
            char* end=nullptr; float n=std::strtof(value.c_str(),&end);
            klPositions[orientation*6+i]=std::isfinite(n) && end && !*end ? std::clamp(n,0.f,100.f)/100 : std::strtof(defaults[i],nullptr)/100;
        }
    }
    const auto& kind=klKind;
    klLayout.enabled = kind == "nds" || kind == "3ds";
    if (!klLayout.enabled || !videoLayout.getScreenWidth() || !videoLayout.getScreenHeight()) return;
    auto viewport=videoLayout.getViewportRect();
    float canvas = float(videoLayout.getScreenWidth())*viewport.getWidth() /
                   (float(videoLayout.getScreenHeight())*viewport.getHeight());
    int offset=klPortrait ? 0 : 6;
    for (int i=0;i<2;i++) {
        auto rect=klScreenRect(klPositions[offset+i*3], klPositions[offset+i*3+1], klPositions[offset+i*3+2],
                               kind=="3ds" && !i ? 5.f/3 : 4.f/3,canvas);
        rect.x=viewport.getX()+rect.x*viewport.getWidth(); rect.y=viewport.getY()+rect.y*viewport.getHeight();
        rect.w*=viewport.getWidth(); rect.h*=viewport.getHeight();
        klLayout.destination[i]=rect;
    }
    klLayout.source[0]={0,0,1,.5f};
    klLayout.source[1]=kind=="3ds" ? KlRect{.1f,.5f,.8f,.5f} : KlRect{0,.5f,1,.5f};
}
std::pair<float,float> Video::getPointerPosition(float x,float y) {
    updateKlLayout();
    return klLayout.enabled ? klMapTouch(klLayout,(x+1)/2,(y+1)/2) : videoLayout.getRelativePosition(x,y);
}
void Video::updateProgram() {
    auto effective = klLayout.enabled ? ShaderManager::Config { ShaderManager::Type::SHADER_DEFAULT } : requestedShaderConfig;''')
change(cpp/'video.cpp', 'loadedShaderType.value() == requestedShaderConfig', 'loadedShaderType.value() == effective')
change(cpp/'video.cpp', 'loadedShaderType = requestedShaderConfig;', 'loadedShaderType = effective;')
change(cpp/'video.cpp', 'ShaderManager::getShader(requestedShaderConfig)', 'ShaderManager::getShader(effective)')
change(cpp/'video.cpp', '    if (immersiveModeEnabled) {', '    updateKlLayout();\n    if (immersiveModeEnabled && !klLayout.enabled) {')
change(cpp/'video.cpp', '        glDrawArrays(GL_TRIANGLES, 0, 6);', '''        if (isLastPass && klLayout.enabled) {
            for (int screen=0;screen<2;screen++) {
                auto v=klVertices(klLayout.destination[screen]);
                auto uv=klCoordinates(klLayout.source[screen],Environment::getInstance().isBottomLeftOrigin());
                glVertexAttribPointer(shader.gvPositionHandle,2,GL_FLOAT,GL_FALSE,0,v.data());
                glVertexAttribPointer(shader.gvCoordinateHandle,2,GL_FLOAT,GL_FALSE,0,uv.data());
                glDrawArrays(GL_TRIANGLES,0,6);
            }
        } else glDrawArrays(GL_TRIANGLES, 0, 6);''')
change(cpp/'libretrodroid.cpp', 'video->getLayout().getRelativePosition(xAxis, yAxis)', 'video->getPointerPosition(xAxis, yAxis)')
(cpp/'kl_dual_layout.h').write_text((Path(__file__).parent/'kl_dual_layout.h').read_text())

change(cpp/'libretrodroid.cpp', '    Environment::getInstance().updateVariable(variable.key, variable.value);', '    Environment::getInstance().updateVariable(variable.key, variable.value);\n    if (video && variable.key == "kl_dual_orientation") video->setKlOrientation(variable.value == "p");\n    if (video && variable.key.rfind("kl_dual_", 0) == 0) video->invalidateKlLayout();')
change(p/'libretrodroid/src/main/java/com/swordfish/libretrodroid/GLRetroView.kt', '            LibretroDroid.onSurfaceChanged(width, height)', '            LibretroDroid.updateVariable(Variable("kl_dual_orientation", if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) "l" else "p"))\n            LibretroDroid.onSurfaceChanged(width, height)')
