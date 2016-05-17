#include "webrtc_ns.h"

#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include <stdio.h>
#include "noise_suppression.h"
#ifdef __cplusplus
extern "C" {
#endif

#define TAG "COM_JABBER_AUDIO_ENCODER_AUDIOWEBRTCNS"
//return handle NsHandle pointer
JNIEXPORT jint JNICALL Java_xu_fyp_project_webrtcNS_initiate
        (JNIEnv *env, jobject obj, jint sample_rate){

    int ret ;
    NsHandle* NS_instance;

    if ((ret = WebRtcNs_Create(&NS_instance) )) {

        return ret;
    }
    if ((ret = WebRtcNs_Init(NS_instance, sample_rate) )) {

        return ret;
    }

    if ( ( ret =  WebRtcNs_set_policy(NS_instance, 2) ) ){

        return ret;
    }

    return (jint)NS_instance;
}

JNIEXPORT jfloatArray JNICALL Java_xu_fyp_project_webrtcNS_process
(JNIEnv *env, jobject obj, jint handle, jshortArray input_frame  ){
    //get handle
    NsHandle* NS_instance = (NsHandle* )handle;

    //create native shorts from java shorts
    jshort *native_input_frame = (*env)->GetShortArrayElements(env, input_frame, NULL);

    //allocate memory for output data
    jint length = (*env)->GetArrayLength(env, input_frame);
    jshortArray temp = (*env)->NewShortArray(env, length);
    jshort *native_output_frame = (*env)->GetShortArrayElements(env, temp, 0);

    //noise suppression
    if(WebRtcNs_Process(NS_instance ,native_input_frame ,NULL ,native_output_frame , NULL)){
    return NULL;
    }

    /*
     * int WebRtcNs_Process(NsHandle* NS_inst,
                     short* spframe,
                     short* spframe_H,
                     short* outframe,
                     short* outframe_H);
                     */
    //ret = WebRtcNs_Process(denoiser->NS_inst, pAudioFrame, tsk_null, denoiser->temp_rec_out, tsk_null)

    //convert native output to java layer output
    jshortArray output_shorts = (*env)->NewShortArray(env, length);
    (*env)->SetShortArrayRegion(env, output_shorts, 0, length, native_output_frame);

    //cleanup and return
    (*env)->ReleaseShortArrayElements(env, input_frame, native_input_frame, 0);
    (*env)->ReleaseShortArrayElements(env, temp, native_output_frame, 0);

    (*env)->DeleteLocalRef(env ,temp);
    return output_shorts;

}

JNIEXPORT jint JNICALL Java_xu_fyp_project_webrtcNS_free
  (JNIEnv *env, jobject obj, jint handle){
	NsHandle* NS_instance = (NsHandle* )handle;

	WebRtcNs_Free(NS_instance);
}

#ifdef __cplusplus
}
#endif
