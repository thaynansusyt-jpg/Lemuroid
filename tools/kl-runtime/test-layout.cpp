#include "kl_dual_layout.h"
#include <cassert>
#include <cstdio>
int main() {
 for(float aspect: {4.f/3,5.f/3}) for(float canvas: {9.f/16,16.f/9})
 for(float size:{.1f,.5f,1.f}) for(float x:{0.f,.5f,1.f}) {
  auto r=klScreenRect(x,x,size,aspect,canvas);
  assert(r.x>=0 && r.y>=0 && r.x+r.w<=1.00001f && r.y+r.h<=1.00001f);
  assert(std::abs(r.w*canvas/r.h-aspect)<.0001f);
 }
 KlDualLayout l; l.destination[1]=klScreenRect(.8f,.8f,.35f,4.f/3,16.f/9);
 l.source[1]={.1f,.5f,.8f,.5f};
 auto d=l.destination[1]; auto p=klMapTouch(l,d.x+d.w/2,d.y+d.h/2);
 assert(std::abs(p.first-.5f)<.00001 && std::abs(p.second-.75f)<.00001);
 assert(klMapTouch(l,-1,-1).first<0);
 auto normal=klCoordinates(l.source[1],false), flipped=klCoordinates(l.source[1],true);
 assert(normal[1]==.5f && normal[3]==1.f && flipped[1]==.5f && flipped[3]==0.f);
 puts("Dual screens: bounds, aspect ratios, touch remapping and texture origins passed");
}
