#include <stdio.h>
#include <string.h>
#include <jni.h>

/* need  it */
jstring Java_com_geekfocus_zuweie_showfm_Myfunc_getResourceHost (JNIEnv* env, jobject thiz)
{
    return (*env)->NewStringUTF(env, "http://dl.showfm.net");
}

jstring Java_com_geekfocus_zuweie_showfm_Myfunc_getNovelApi (JNIEnv* env, jobject thiz)
{
    return (*env)->NewStringUTF(env, "http://www.showfm.net/api/novel.asp");
}

jstring Java_com_geekfocus_zuweie_showfm_Myfunc_getRecordApi (JNIEnv* env, jobject thiz)
{
    return (*env)->NewStringUTF(env, "http://www.showfm.net/api/record.asp");
}
/* need it */


JNIEXPORT jstring JNICALL Java_com_geekfocus_zuweie_showfm_Myfunc_getValidText(JNIEnv* env, jobject obj, jstring txt){

    const char *str = (*env)->GetStringUTFChars(env, txt, NULL);

    size_t sz = strlen(str);
    int i=0;
    int aindex = 0;
    int bindex = sz / 2;
    char decode[sz+10];
    memset(decode, '\0', sz+10);

    for(i=0; i<sz; ++i){
       if (i % 2 == 0){
          decode[bindex++] = str[i];
       }else{
          decode[aindex++] = str[i];
       }
    }
    (*env)->ReleaseStringUTFChars(env, txt, str);
    return (*env)->NewStringUTF(env, decode);
}
