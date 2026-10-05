#include "kl_audio_bounds.h"
#include <cassert>
#include <vector>
#include <cstdio>
int main() {
 for (int cap : {4096, 8192}) for (double rate : {0.001, 0.4, 1.0, 2.02, 8.184, 16.2, 128.0})
 for (int callback : {1, 96, 192, 1024, 4096, 16384}) {
  double fractional=0; long total=0;
  for (int k=0;k<20;k++) {
   int remaining=callback;
   while(remaining>0) {
    auto c=klAudioChunk(remaining,cap,rate,fractional);
    assert(c.outputFrames>0 && c.outputFrames<=remaining && c.inputFrames<=cap && c.inputFrames>=0);
    std::vector<short> scratch(cap*2); std::fill(scratch.begin(),scratch.begin()+c.inputFrames*2,0);
    remaining-=c.outputFrames; total+=c.inputFrames;
   }
  }
  assert(std::abs(total-callback*20*rate)<=1.0);
 }
 puts("Audio bounds: 1x–8x, varying sample rates and oversized callbacks passed");
}
