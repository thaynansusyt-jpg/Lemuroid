#!/usr/bin/env bash
set -euo pipefail
source_dir="$1"
build_dir="$2"
output_file="$3"
cmake -S "$source_dir" -B "$build_dir" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_HOME/ndk/27.2.12479018/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-23 -DANDROID_STL=c++_static \
  -DLIBRETRO=ON -DUSE_VULKAN=OFF -DUSE_OPENGL=ON -DUSE_GLES=ON \
  -DUSE_OPENMP=OFF -DUSE_LUA=OFF -DUSE_HOST_LIBZIP=OFF
cmake --build "$build_dir" --parallel 2 --target flycast_libretro
toolchain="$ANDROID_HOME/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin"
library="$build_dir/flycast_libretro.so"
"$toolchain/llvm-readelf" -h "$library" | grep 'Machine:.*AArch64'
for symbol in retro_init retro_load_game retro_run retro_serialize retro_unserialize; do
  "$toolchain/llvm-nm" -D --defined-only "$library" > "$build_dir/exported-symbols.txt"
  grep -E " [TW] $symbol$" "$build_dir/exported-symbols.txt"
done
if "$toolchain/llvm-readelf" -d "$library" | grep -q 'libc++_shared.so'; then
  echo 'Dreamcast must include its own C++ runtime.'
  exit 1
fi
mkdir -p "$(dirname "$output_file")"
"$toolchain/llvm-strip" --strip-unneeded "$library"
cp "$library" "$output_file"
