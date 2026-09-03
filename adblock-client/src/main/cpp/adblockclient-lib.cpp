#include <jni.h>
#include "third-party/ad-block/ad_block_client.h"

// Class/method lookups cached at load time: matches() runs once per filter
// list for every subresource, and FindClass/GetMethodID per call showed up
// on the request path.
static jclass gMatchResultClass = nullptr;
static jmethodID gMatchResultInit = nullptr;
static jclass gStringClass = nullptr;
static jmethodID gStringBytesInit = nullptr;
static jstring gUtf8Encoding = nullptr;

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass matchResult = env->FindClass("io/github/edsuns/adblockclient/MatchResult");
    gMatchResultClass = (jclass) env->NewGlobalRef(matchResult);
    gMatchResultInit = env->GetMethodID(gMatchResultClass, "<init>",
                                        "(ZLjava/lang/String;Ljava/lang/String;)V");
    jclass stringCls = env->FindClass("java/lang/String");
    gStringClass = (jclass) env->NewGlobalRef(stringCls);
    gStringBytesInit = env->GetMethodID(gStringClass, "<init>", "([BLjava/lang/String;)V");
    gUtf8Encoding = (jstring) env->NewGlobalRef(env->NewStringUTF("UTF-8"));
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jlong
JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_createClient(JNIEnv *env,
                                                               jobject) {
    auto *client = new AdBlockClient();
    return (long) client;
}

extern "C"
JNIEXPORT void
JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_releaseClient(JNIEnv *env,
                                                                jobject,
                                                                jlong clientPointer,
                                                                jlong rawDataPointer,
                                                                jlong processedDataPointer) {
    auto *client = (AdBlockClient *) clientPointer;
    delete client;

    char *rawData = (char *) rawDataPointer;
    delete[] rawData;

    char *processedData = (char *) processedDataPointer;
    delete[] processedData;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_isGenericElementHidingEnabled(JNIEnv *env,
                                                                                jobject /* this */,
                                                                                jlong clientPointer) {
    auto *client = (AdBlockClient *) clientPointer;
    return client->isGenericElementHidingEnabled;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_setGenericElementHidingEnabled(JNIEnv *env,
                                                                                 jobject /* this */,
                                                                                 jlong clientPointer,
                                                                                 jboolean enabled) {
    auto *client = (AdBlockClient *) clientPointer;
    client->isGenericElementHidingEnabled = enabled;
}

extern "C"
JNIEXPORT jlong
JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_loadBasicData(JNIEnv *env,
                                                                jobject,
                                                                jlong clientPointer,
                                                                jbyteArray data,
                                                                jboolean preserveRules) {
    int dataLength = env->GetArrayLength(data);
    char *dataChars = new char[dataLength];
    env->GetByteArrayRegion(data, 0, dataLength, reinterpret_cast<jbyte *>(dataChars));

    auto *client = (AdBlockClient *) clientPointer;
    client->parse(dataChars, preserveRules);

    return (long) dataChars;
}

extern "C"
JNIEXPORT jlong
JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_loadProcessedData(JNIEnv *env,
                                                                    jobject /* this */,
                                                                    jlong clientPointer,
                                                                    jbyteArray data,
                                                                    jint dataLength) {
    // Only the first dataLength bytes belong to the native engine; the Kotlin
    // side appends its own regex-rule section after them (see RegexFilterSet).
    char *dataChars = new char[dataLength];
    env->GetByteArrayRegion(data, 0, dataLength, reinterpret_cast<jbyte *>(dataChars));

    auto *client = (AdBlockClient *) clientPointer;
    client->deserialize(dataChars);

    // We cannot delete dataChars here as adblock keeps a ptr to it.
    // Instead we send back a ptr ref so we can delete it later in the release method
    return (long) dataChars;
}

extern "C"
JNIEXPORT jbyteArray
JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getProcessedData(JNIEnv *env,
                                                                   jobject /* this */,
                                                                   jlong clientPointer) {
    auto *client = (AdBlockClient *) clientPointer;

    int size;
    char *data = client->serialize(&size, false);

    jbyteArray dataBytes = env->NewByteArray(size);
    env->SetByteArrayRegion(dataBytes, 0, size, reinterpret_cast<jbyte *>(data));

    delete[] data;
    return dataBytes;
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getFiltersCount(JNIEnv *env, jobject /* this */,
                                                                  jlong clientPointer) {
    auto *client = (AdBlockClient *) clientPointer;
    int count = client->numFilters
                + client->numCosmeticFilters
                + client->numHtmlFilters
                + client->numScriptletFilters
                + client->numExceptionFilters
                + client->numNoFingerprintFilters
                + client->numNoFingerprintExceptionFilters
                + client->numNoFingerprintDomainOnlyFilters
                + client->numNoFingerprintAntiDomainOnlyFilters
                + client->numNoFingerprintDomainOnlyExceptionFilters
                + client->numNoFingerprintAntiDomainOnlyExceptionFilters
                + client->numHostAnchoredFilters
                + client->numHostAnchoredExceptionFilters;
    return count;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_matches(JNIEnv *env, jobject /* this */,
                                                          jlong clientPointer, jstring url,
                                                          jstring firstPartyDomain,
                                                          jint filterOption) {
    jboolean isUrlCopy;
    const char *urlChars = env->GetStringUTFChars(url, &isUrlCopy);

    jboolean isDocumentCopy;
    const char *firstPartyDomainChars = env->GetStringUTFChars(firstPartyDomain, &isDocumentCopy);

    auto *client = (AdBlockClient *) clientPointer;

    Filter *matchedFilter;
    Filter *matchedExceptionFilter;
    bool shouldBlock = client->matches(urlChars, (FilterOption) filterOption, firstPartyDomainChars,
                                       &matchedFilter, &matchedExceptionFilter);

    char *matchedRule = matchedFilter ? matchedFilter->ruleDefinition : nullptr;
    char *matchedExceptionRule = matchedExceptionFilter ?
                                 matchedExceptionFilter->ruleDefinition : nullptr;

    // create java MatchResult
    jobject matchResult = env->NewObject(gMatchResultClass, gMatchResultInit,
                                         shouldBlock,
                                         env->NewStringUTF(matchedRule),
                                         env->NewStringUTF(matchedExceptionRule));

    env->ReleaseStringUTFChars(url, urlChars);
    env->ReleaseStringUTFChars(firstPartyDomain, firstPartyDomainChars);

    return matchResult;
}

// replacement for NewStringUTF()
// won't throw JNI ERROR: input is not valid Modified UTF-8
jstring bytesToStringUTF(JNIEnv *env, const char *src) {

    if (!src) {
        return nullptr;
    }
    jsize len = strlen(src);
    jbyteArray bytes = env->NewByteArray(len);
    env->SetByteArrayRegion(bytes, 0, len, (jbyte *) src);

    return (jstring) env->NewObject(gStringClass, gStringBytesInit, bytes, gUtf8Encoding);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getElementHidingSelectors(JNIEnv *env,
                                                                            jobject /* this */,
                                                                            jlong clientPointer,
                                                                            jstring url) {
    jboolean isUrlCopy;
    const char *urlChars = env->GetStringUTFChars(url, &isUrlCopy);

    auto *client = (AdBlockClient *) clientPointer;
    const char *selectors = client->getElementHidingSelectors(urlChars);

    env->ReleaseStringUTFChars(url, urlChars);

    return bytesToStringUTF(env, selectors);
}

jobjectArray toStringArray(JNIEnv *env, const LinkedList<std::string> *rules) {
    if (!rules) {
        return nullptr;
    }
    auto array = env->NewObjectArray(rules->length(), gStringClass, nullptr);
    int i = 0;
    for (auto r : *rules) {
        env->SetObjectArrayElement(array, i, env->NewStringUTF(r.c_str()));
        i++;
    }
    return array;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getExtendedCssSelectors(JNIEnv *env,
                                                                          jobject /* this */,
                                                                          jlong clientPointer,
                                                                          jstring url) {
    jboolean isUrlCopy;
    const char *urlChars = env->GetStringUTFChars(url, &isUrlCopy);

    auto *client = (AdBlockClient *) clientPointer;
    const LinkedList<std::string> *rules = client->getExtendedCssSelectors(urlChars);

    env->ReleaseStringUTFChars(url, urlChars);

    return toStringArray(env, rules);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getCssRules(JNIEnv *env,
                                                              jobject /* this */,
                                                              jlong clientPointer,
                                                              jstring url) {
    jboolean isUrlCopy;
    const char *urlChars = env->GetStringUTFChars(url, &isUrlCopy);

    auto *client = (AdBlockClient *) clientPointer;
    const LinkedList<std::string> *rules = client->getCssRules(urlChars);

    env->ReleaseStringUTFChars(url, urlChars);

    return toStringArray(env, rules);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_github_edsuns_adblockclient_AdBlockClient_getScriptlets(JNIEnv *env,
                                                                jobject /* this */,
                                                                jlong clientPointer,
                                                                jstring url) {
    jboolean isUrlCopy;
    const char *urlChars = env->GetStringUTFChars(url, &isUrlCopy);

    auto *client = (AdBlockClient *) clientPointer;
    const LinkedList<std::string> *rules = client->getScriptlets(urlChars);

    env->ReleaseStringUTFChars(url, urlChars);

    return toStringArray(env, rules);
}
