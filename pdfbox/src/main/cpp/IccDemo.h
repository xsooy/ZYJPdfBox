////
//// Created by 钟元杰 on 2022/9/19.
////
#include <jni.h>

#ifndef TEST_ICCDEMO_H
#define TEST_ICCDEMO_H

//#ifdef __cplusplus
extern "C" {
//#endif
JNIEXPORT jint JNICALL Java_com_xsooy_icc_IccUtils_loadProfile
        (JNIEnv *, jobject, jstring);
JNIEXPORT void JNICALL Java_com_xsooy_icc_IccUtils_apply(JNIEnv *, jobject, jint);
//#ifdef __cplusplus
}
//#endif
#endif //TEST_ICCDEMO_H
