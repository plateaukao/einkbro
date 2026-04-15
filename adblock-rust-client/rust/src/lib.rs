use jni::objects::JClass;
use jni::sys::jlong;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeCreateEngine(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    todo!("implemented in step 2")
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeReleaseEngine(
    _env: JNIEnv,
    _class: JClass,
    _ptr: jlong,
) {
    todo!("implemented in step 2")
}
