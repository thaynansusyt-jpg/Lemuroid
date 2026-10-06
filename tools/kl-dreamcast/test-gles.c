#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <dlfcn.h>
#include <stdarg.h>
#include <sys/stat.h>
#include "libretro.h"
// EGL ABI, used to exercise the core in a real GLES context without an Android device.
extern void* eglGetDisplay(void*);
extern unsigned eglInitialize(void*, int*, int*);
extern unsigned eglBindAPI(unsigned);
extern unsigned eglChooseConfig(void*,const int*,void**,int,int*);
extern void* eglCreatePbufferSurface(void*,void*,const int*);
extern void* eglCreateContext(void*,void*,void*,const int*);
extern unsigned eglMakeCurrent(void*,void*,void*,void*);
extern void* eglGetProcAddress(const char*);
static struct retro_hw_render_callback hw;
static const char* sysdir="/tmp/kl-flycast-smoke/system",*savedir="/tmp/kl-flycast-smoke/saves";
static unsigned videos=0;static size_t audioframes=0;
static struct {char* key;char* value;} options[200];static unsigned count=0;
static uintptr_t framebuffer(void){return 0;}
static retro_proc_address_t procedure(const char* name){return (retro_proc_address_t)eglGetProcAddress(name);}
static void log_cb(enum retro_log_level level,const char* format,...){if(level>=RETRO_LOG_WARN){va_list ap;va_start(ap,format);vfprintf(stderr,format,ap);va_end(ap);}}
static bool environment(unsigned cmd,void* data){
 switch(cmd){
 case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: ((struct retro_log_callback*)data)->log=log_cb;return true;
 case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: *(const char**)data=sysdir;return true;
 case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: *(const char**)data=savedir;return true;
 case RETRO_ENVIRONMENT_GET_CAN_DUPE: *(bool*)data=true;return true;
 case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER: *(unsigned*)data=RETRO_HW_CONTEXT_OPENGLES3;return true;
 case RETRO_ENVIRONMENT_SET_HW_RENDER:{
  struct retro_hw_render_callback* requested=data;
  if(requested->context_type!=RETRO_HW_CONTEXT_OPENGLES2 && requested->context_type!=RETRO_HW_CONTEXT_OPENGLES3)return false;
  requested->get_current_framebuffer=framebuffer;requested->get_proc_address=procedure;hw=*requested;return true;
 }
 case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION: *(unsigned*)data=0;return true;
 case RETRO_ENVIRONMENT_SET_VARIABLES:{
  const struct retro_variable* v=data;
  for(;v->key && count<200;v++){
   const char* values=strchr(v->value,';');if(!values)continue;values++;while(*values==' ')values++;
   options[count].key=strdup(v->key);options[count].value=strndup(values,strcspn(values,"|"));count++;
  }return true;
 }
 case RETRO_ENVIRONMENT_GET_VARIABLE:{
  struct retro_variable* v=data;
  if(!strcmp(v->key,"reicast_threaded_rendering"))v->value="disabled";
  else if(!strcmp(v->key,"reicast_hle_bios"))v->value="enabled";
  else if(!strcmp(v->key,"reicast_per_content_vmus"))v->value="All VMUs";
  else if(!strcmp(v->key,"reicast_alpha_sorting"))v->value="per-strip (fast, least accurate)";
  else{v->value=NULL;for(unsigned i=0;i<count;i++)if(!strcmp(v->key,options[i].key)){v->value=options[i].value;break;}}
  return v->value!=NULL;
 }
 case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: *(bool*)data=false;return true;
 case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
 case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:case RETRO_ENVIRONMENT_SET_GEOMETRY:
 case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE:case RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE:
 case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:return true;
 default:return false;
 }
}
static void video(const void* data,unsigned w,unsigned h,size_t pitch){if(data==RETRO_HW_FRAME_BUFFER_VALID && w && h)videos++;}
static size_t audio(const int16_t* data,size_t frames){audioframes+=frames;return frames;}
static void audiosample(int16_t l,int16_t r){audioframes++;}
static void poll_input(void){}
static int16_t input(unsigned p,unsigned d,unsigned i,unsigned id){return 0;}
#define FN(name) __typeof__(&name) name##_p=(__typeof__(&name))dlsym(lib,#name);if(!name##_p){fprintf(stderr,"Missing %s\n",#name);return 1;}
int main(int argc,char**argv){
 if(argc!=3)return 2;
 mkdir("/tmp/kl-flycast-smoke",0700);mkdir(sysdir,0700);mkdir(savedir,0700);
 void* display=eglGetDisplay(NULL);int major,minor;
 if(!eglInitialize(display,&major,&minor)||!eglBindAPI(0x30A0)){puts("No surfaceless EGL");return 3;}
 const int config_attrs[]={0x3033,1,0x3040,0x40,0x3024,8,0x3023,8,0x3022,8,0x3025,24,0x3026,8,0x3038};
 void* config=NULL;int configs;
 if(!eglChooseConfig(display,config_attrs,&config,1,&configs)||!configs)return 4;
 const int surface_attrs[]={0x3057,640,0x3056,480,0x3038};
 const int context_attrs[]={0x3098,3,0x3038};
 void* surface=eglCreatePbufferSurface(display,config,surface_attrs);
 void* context=eglCreateContext(display,config,NULL,context_attrs);
 if(!eglMakeCurrent(display,surface,surface,context))return 5;
 void* lib=dlopen(argv[1],RTLD_NOW|RTLD_LOCAL);if(!lib){puts(dlerror());return 6;}
 FN(retro_set_environment);FN(retro_set_video_refresh);FN(retro_set_audio_sample);FN(retro_set_audio_sample_batch);
 FN(retro_set_input_poll);FN(retro_set_input_state);FN(retro_init);FN(retro_load_game);FN(retro_run);
 FN(retro_unload_game);FN(retro_deinit);FN(retro_serialize_size);FN(retro_serialize);FN(retro_unserialize);
 retro_set_environment_p(environment);retro_set_video_refresh_p(video);retro_set_audio_sample_p(audiosample);retro_set_audio_sample_batch_p(audio);
 retro_set_input_poll_p(poll_input);retro_set_input_state_p(input);retro_init_p();
 struct retro_game_info game={.path=argv[2]};if(!retro_load_game_p(&game)){puts("Load failed");return 7;}
 if(!hw.context_reset)return 8;hw.context_reset();
 for(unsigned i=0;i<30;i++)retro_run_p();
 size_t state_size=retro_serialize_size_p();void* state=malloc(state_size);
 bool serialized=state_size && retro_serialize_p(state,state_size);
 bool restored=serialized && retro_unserialize_p(state,state_size);
 free(state);retro_unload_game_p();if(hw.context_destroy)hw.context_destroy();retro_deinit_p();
 printf("GLES core smoke: hardware frames=%u audio frames=%zu state bytes=%zu save/restore=%d/%d\n",videos,audioframes,state_size,serialized,restored);
 return !(videos>0 && audioframes>0 && restored);
}
