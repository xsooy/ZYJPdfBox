//
// Created by 钟元杰 on 2022/9/19.
//

//#include "include/IccDemo.h"
#include "IccCmm.h"
#include <jni.h>
#include <android/log.h>

#define TAG "IccCmm_ceshi"
#define pri_debug(format, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, "[%s:%d]" format, basename(__FILE__), __LINE__, ##args)

CIccCmm cmm;
icFloatNumber Pixels[16];

extern "C"{

    JNIEXPORT jint JNICALL Java_com_xsooy_icc_IccUtils_loadProfile(JNIEnv *env, jobject thiz, jstring path) {
        if (cmm.GetNumXforms()!=0) {
            pri_debug("profile已加载");
            return 1;
        }
        const char *nativeString = env->GetStringUTFChars(path, 0);
        if (cmm.AddXform(nativeString, (icRenderingIntent)4)) {
            pri_debug("合入失败%s",nativeString);
//            printf("Invalid Profile:  %s\n", szSrcProfile);
            return -1;
        }
        pri_debug("合入成功%s",nativeString);
        if (cmm.Begin() != icCmmStatOk) {
            pri_debug("合入失败222%s",nativeString);
//            printf("Invalid Profile:\n  %s\n  %s'\n", szSrcProfile, szDstProfile);
            return false;
        }
        return 0;
    }

JNIEXPORT jint JNICALL Java_com_xsooy_icc_IccUtils_loadProfile2(JNIEnv *env, jobject thiz, jstring path,jstring path2) {
    if (cmm.GetNumXforms()!=0) {
        pri_debug("profile已加载");
        return 1;
    }
    const char *nativeString = env->GetStringUTFChars(path, 0);
    const char *nativeString2 = env->GetStringUTFChars(path2, 0);
    if (cmm.AddXform(nativeString, (icRenderingIntent)0)) {
        pri_debug("合入失败%s",nativeString);
//            printf("Invalid Profile:  %s\n", szSrcProfile);
        return -1;
    }
    if (cmm.AddXform(nativeString2)) {
        pri_debug("合入失败%s",nativeString2);
//            printf("Invalid Profile:  %s\n", szSrcProfile);
        return -1;
    }
    if (cmm.Begin() != icCmmStatOk) {
//            printf("Invalid Profile:\n  %s\n  %s'\n", szSrcProfile, szDstProfile);
        return false;
    }
    return 0;
}

    JNIEXPORT jint JNICALL Java_com_xsooy_icc_IccUtils_apply(JNIEnv *env, jobject thiz, jint pixel) {
        float b = float (pixel&0xff)/255;
        float g = float (pixel>>8&0xff)/255;
        float r = float (pixel>>16&0xff)/255;
//        jint *r = (*env).GetIntArrayElements(array,0);
//        int a = (int) r;
//        Pixel[0] = ;
//        pri_debug("piex:%d",pixel&0xff);
//        pri_debug("piex22:%f",float (pixel&0xff)/255);
        Pixels[0] = float (pixel&0xff)/255;
        Pixels[1] = float (pixel>>8&0xff)/255;
        Pixels[2] = float (pixel>>16&0xff)/255;
        Pixels[3] = float (pixel>>24&0xff)/255;
//        pri_debug("piex3333:%d",cmm.GetNumXforms());
//        pri_debug("piex4444:%f",*Pixels);
//        pri_debug("piex4444:%d",cmm.GetSourceSpace());

//    Pixel[0] = (*env)->GetObjectArrayElement(env, array, 0);
//    Pixel[1] = (*env)->GetObjectArrayElement(env, array, 0);
//    Pixel[2] = (*env)->GetObjectArrayElement(env, array, 0);
//        icFloatNumber ff[3];
        cmm.Apply(Pixels, Pixels);

        int resutl = ((int)(Pixels[3]*255) <<24) | ((int)(Pixels[2]*255) <<16) | ((int)(Pixels[1]*255) <<8) | ((int)(Pixels[0]*255) & 0xff);
//        pri_debug("piex4444:%f,%f,%f",Pixels[2] * 255,Pixels[1] * 255,Pixels[0] * 255);
        return resutl;
    }

    JNIEXPORT void JNICALL Java_com_xsooy_icc_IccUtils_applyCmyk(JNIEnv *env, jobject thiz, jfloatArray array,jfloatArray outArray) {
        jboolean isCopy = JNI_FALSE;
        jfloat *parray = env->GetFloatArrayElements(array, &isCopy);
        jfloat *outparray = env->GetFloatArrayElements(outArray, &isCopy);
        Pixels[0] = float (parray[0]);
        Pixels[1] = float (parray[1]);
        Pixels[2] = float (parray[2]);
        Pixels[3] = float (parray[3]);
//    pri_debug("piex3333:%d",cmm.GetNumXforms());
//        pri_debug("piex4444:%f",*Pixels);
//        pri_debug("piex4444:%d",cmm.GetSourceSpace());
//        pri_debug("piex4444:%d",cmm.GetDestSpace());

        //change data to 'lab'
        cmm.Apply(Pixels, Pixels);

//        pri_debug("result:%f,%f,%f,%f",Pixels[0],Pixels[1],Pixels[2],Pixels[3]);
        env->SetFloatArrayRegion(outArray,0,3,Pixels);
//        parray[0] = Pixels[0];
//        parray[1] = Pixels[1];
//        parray[2] = Pixels[2];
//        parray[3] = Pixels[3];
//        pri_debug("result2222:%f,%f,%f,%f",parray[0],parray[1],parray[2],parray[3]);
//    int resutl = ((int)(Pixels[3]*255) <<24) | ((int)(Pixels[2]*255) <<16) | ((int)(Pixels[1]*255) <<8) | ((int)(Pixels[0]*255) & 0xff);
//        pri_debug("piex4444:%f,%f,%f",Pixels[2] * 255,Pixels[1] * 255,Pixels[0] * 255);
    }

}