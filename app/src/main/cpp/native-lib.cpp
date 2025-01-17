#include <jni.h>
#include <string>
#include "WlCallJava.h"
#include "WlFFmpeg.h"
#include "RtmpUpdate.h"


WlCallJava *wlCallJava;
JavaVM *javaVm;
WlFFmpeg *fFmpeg;
WlPlaystatus *playstatus = NULL;
RtmpUpdate *rtmpUpdate = NULL;

bool nexit = true;
pthread_t thread_start;

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jint result = -1;
    javaVm = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) env, JNI_VERSION_1_6) != JNI_OK) {
        return result;
    }
    return JNI_VERSION_1_6;
};


void *startCallBack(void *data) {
    WlFFmpeg *fFmpeg = (WlFFmpeg *) data;
    fFmpeg->start();
    return 0;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1update_1file(JNIEnv *env, jobject thiz, jstring name,
                                                           jstring url) {
    LOGE("start");
    const char* fileName = env->GetStringUTFChars(name, 0);
    const char* rtmpUrl = env->GetStringUTFChars(url, 0);
    if (rtmpUpdate == NULL){
        LOGE("one");
        rtmpUpdate = new RtmpUpdate();
    }
    rtmpUpdate->upDateFile(fileName, rtmpUrl);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1prepared(JNIEnv *env, jobject instance,
                                                       jstring source_) {
    const char *source = env->GetStringUTFChars(source_, 0);
    if (fFmpeg == NULL) {
        if (wlCallJava == NULL) {
            wlCallJava = new WlCallJava(javaVm, env, &instance);
        }
        playstatus = new WlPlaystatus();
        fFmpeg = new WlFFmpeg(playstatus, wlCallJava, source);
        fFmpeg->prepared();
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1start(JNIEnv *env, jobject thiz) {
    if (fFmpeg != NULL) {
        pthread_create(&thread_start, NULL, startCallBack, fFmpeg);
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1pause(JNIEnv *env, jobject thiz) {
    if (fFmpeg != NULL) {
        fFmpeg->pause();
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1resume(JNIEnv *env, jobject thiz) {
    if (fFmpeg != NULL) {
        fFmpeg->resume();
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1stop(JNIEnv *env, jobject thiz) {
    if (!nexit) {
        return;
    }

    jclass clz = env->GetObjectClass(thiz);
    jmethodID jmid_next = env->GetMethodID(clz, "onCallNext", "()V");
    nexit = false;
    if (fFmpeg != NULL) {
        fFmpeg->release();
        pthread_join(thread_start, NULL);
        delete (fFmpeg);
        fFmpeg = NULL;
        if (wlCallJava != NULL) {
            delete (wlCallJava);
            wlCallJava = NULL;
        }
        if (playstatus != NULL) {
            delete (playstatus);
            playstatus = NULL;
        }
    }
    nexit = true;
    env->CallVoidMethod(thiz, jmid_next);
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1seek(JNIEnv *env, jobject thiz, jint secds) {
    if (fFmpeg != NULL) {
        fFmpeg->seek(secds);
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1volume(JNIEnv *env, jobject thiz, jint percent) {
    if (fFmpeg != NULL) {
        fFmpeg->setVolume(percent);
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1mute(JNIEnv *env, jobject thiz, jint mute) {
    if (fFmpeg != NULL) {
        fFmpeg->setMute(mute);
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1pitch(JNIEnv *env, jobject thiz, jfloat pitch) {
    if (fFmpeg != NULL) {
        fFmpeg->setPitch(pitch);
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_simpo_promusic_player_MusicPlayer_n_1speed(JNIEnv *env, jobject thiz, jfloat speed) {
    if (fFmpeg != NULL) {
        fFmpeg->setSpeed(speed);
    }
}