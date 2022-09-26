//
// Created by 钟元杰 on 2022/9/19.
//

//#include "include/IccDemo.h"
#include "IccCmm.h"
#include "IccProfile.h"
#include <jni.h>
#include <android/log.h>

#define TAG "IccCmm_ceshi"
#define pri_debug(format, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, "[%s:%d]" format, basename(__FILE__), __LINE__, ##args)

CIccCmm *cmm = NULL;
icFloatNumber Pixels[16];

extern "C"{

    JNIEXPORT jint JNICALL Java_com_xsooy_icc_IccUtils_loadProfile(JNIEnv *env, jobject thiz, jstring path) {
        if (cmm != NULL) {
            delete cmm;
        }
        cmm = new CIccCmm;
        if (cmm->GetNumXforms()!=0) {
            pri_debug("profile已加载");
            return 1;
        }
        const char *nativeString = env->GetStringUTFChars(path, 0);
        if (cmm->AddXform(nativeString, (icRenderingIntent)0)) {
            pri_debug("合入失败%s",nativeString);
//            printf("Invalid Profile:  %s\n", szSrcProfile);
            return -1;
        }
        pri_debug("合入成功%s",nativeString);
        if (cmm->Begin() != icCmmStatOk) {
            pri_debug("合入失败222%s",nativeString);
//            printf("Invalid Profile:\n  %s\n  %s'\n", szSrcProfile, szDstProfile);
            return false;
        }
        return 0;
    }

icUInt8Number* ConvertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray)
{
    icUInt8Number *chars = NULL;
    jbyte *bytes;
    bytes = env->GetByteArrayElements(bytearray, 0);
    int chars_len = env->GetArrayLength(bytearray);
    chars = new icUInt8Number[chars_len + 1];
    memset(chars,0,chars_len + 1);
    memcpy(chars, bytes, chars_len);
    chars[chars_len] = 0;

    env->ReleaseByteArrayElements(bytearray, bytes, 0);
    return chars;
}

JNIEXPORT jint JNICALL Java_com_xsooy_icc_IccUtils_loadProfileByData(JNIEnv *env, jobject thiz, jbyteArray data) {
    if (cmm != NULL) {
        delete cmm;
    }
    cmm = new CIccCmm;
    icUInt8Number *pmsg = ConvertJByteaArrayToChars(env,data);
    int chars_len = env->GetArrayLength(data);
    CIccProfile* cIccProfile = OpenIccProfile(pmsg, chars_len);
    if (cIccProfile==NULL) {
        pri_debug("创建IccData失败");
        return -1;
    }
    if (cmm->AddXform(cIccProfile, (icRenderingIntent)0)) {
        pri_debug("读取IccData失败");
        return -1;
    }
    pri_debug("读取IccData成功");
    pri_debug("piex4444:%d",cmm->GetSourceSpace());
    if (cmm->Begin() != icCmmStatOk) {
        pri_debug("启动Icc失败");
        return false;
    }
    pri_debug("piex4444:%d",cmm->GetDestSpace());
    return cmm->GetSourceSpace();
}

    JNIEXPORT jfloat JNICALL Java_com_xsooy_icc_IccUtils_apply(JNIEnv *env, jobject thiz, jfloat pixel) {
        Pixels[0] = (float) pixel;
        cmm->Apply(Pixels, Pixels);
        return Pixels[0];
    }

JNIEXPORT void JNICALL Java_com_xsooy_icc_IccUtils_applyGray(JNIEnv *env, jobject thiz, jfloatArray array,jfloatArray outArray) {
    jboolean isCopy = JNI_FALSE;
    jfloat *parray = env->GetFloatArrayElements(array, &isCopy);
    Pixels[0] = float (parray[0]);

    cmm->Apply(Pixels, Pixels);

    env->SetFloatArrayRegion(outArray,0,3,Pixels);
}

    JNIEXPORT void JNICALL Java_com_xsooy_icc_IccUtils_applyCmyk(JNIEnv *env, jobject thiz, jfloatArray array,jfloatArray outArray) {
        jboolean isCopy = JNI_FALSE;
        jfloat *parray = env->GetFloatArrayElements(array, &isCopy);
//        jfloat *outparray = env->GetFloatArrayElements(outArray, &isCopy);
        Pixels[0] = float (parray[0]);
        Pixels[1] = float (parray[1]);
        Pixels[2] = float (parray[2]);
        Pixels[3] = float (parray[3]);
//    pri_debug("piex3333:%d",cmm->GetNumXforms());
//        pri_debug("piex4444:%f",*Pixels);
//        pri_debug("piex4444:%d",cmm->GetSourceSpace());
//        pri_debug("piex4444:%d",cmm->GetDestSpace());

        //change data to 'lab'
        cmm->Apply(Pixels, Pixels);

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