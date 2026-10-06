// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <algorithm>
#include <array>
#include <cmath>
#include <utility>
struct KlRect { float x=0, y=0, w=1, h=1; };
struct KlDualLayout {
 bool enabled=false;
 std::array<KlRect,2> destination;
 std::array<KlRect,2> source;
};
inline KlRect klScreenRect(float x, float y, float width, float aspect, float canvasAspect) {
 width=std::clamp(width,0.1f,1.0f);
 float height=width*canvasAspect/aspect;
 if(height>1) { width/=height; height=1; }
 return {std::clamp(x,0.f,1.f)*(1-width),std::clamp(y,0.f,1.f)*(1-height),width,height};
}
inline std::array<float,12> klVertices(KlRect r) {
 float l=2*r.x-1, t=1-2*r.y, right=2*(r.x+r.w)-1, b=1-2*(r.y+r.h);
 return {l,t,l,b,right,t,right,t,l,b,right,b};
}
inline std::array<float,12> klCoordinates(KlRect r,bool flipped) {
 float t=flipped?1-r.y:r.y, b=flipped?1-r.y-r.h:r.y+r.h;
 return {r.x,t,r.x,b,r.x+r.w,t,r.x+r.w,t,r.x,b,r.x+r.w,b};
}
inline std::pair<float,float> klMapTouch(const KlDualLayout& layout,float x,float y) {
 // Bottom screen is drawn last and owns an overlapping touch.
 const auto& d=layout.destination[1]; const auto& s=layout.source[1];
 if(x<d.x || y<d.y || x>d.x+d.w || y>d.y+d.h) return {-10,-10};
 return {s.x+(x-d.x)/d.w*s.w,s.y+(y-d.y)/d.h*s.h};
}
