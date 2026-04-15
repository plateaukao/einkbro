use adblock::lists::{FilterSet, ParseOptions};
use adblock::request::Request;
use adblock::Engine;
use jni::objects::{JByteArray, JClass, JObject, JObjectArray, JString, JValue};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jobject, jobjectArray, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use std::collections::HashSet;

struct EngineHandle {
    engine: Engine,
    generic_hide: bool,
}

impl EngineHandle {
    fn new_empty() -> Self {
        Self {
            engine: Engine::from_filter_set(FilterSet::new(false), true),
            generic_hide: true,
        }
    }

    fn from_rules_text(text: &str) -> Self {
        let mut filter_set = FilterSet::new(false);
        filter_set.add_filter_list(text, ParseOptions::default());
        Self {
            engine: Engine::from_filter_set(filter_set, true),
            generic_hide: true,
        }
    }

    fn from_serialized(bytes: &[u8]) -> Result<Self, String> {
        let mut engine = Engine::from_filter_set(FilterSet::new(false), true);
        engine
            .deserialize(bytes)
            .map_err(|e| format!("{e:?}"))?;
        Ok(Self {
            engine,
            generic_hide: true,
        })
    }
}

fn into_raw(handle: EngineHandle) -> jlong {
    Box::into_raw(Box::new(handle)) as jlong
}

unsafe fn as_ref<'a>(ptr: jlong) -> Option<&'a mut EngineHandle> {
    if ptr == 0 {
        None
    } else {
        Some(&mut *(ptr as *mut EngineHandle))
    }
}

fn throw(env: &mut JNIEnv, msg: &str) {
    let _ = env.throw_new("java/lang/RuntimeException", msg);
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeCreateEngine(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    into_raw(EngineHandle::new_empty())
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeReleaseEngine(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) {
    if ptr == 0 {
        return;
    }
    unsafe {
        drop(Box::from_raw(ptr as *mut EngineHandle));
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeLoadRules(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    data: JByteArray,
) -> jlong {
    let bytes = match env.convert_byte_array(&data) {
        Ok(b) => b,
        Err(e) => {
            throw(&mut env, &format!("convert_byte_array failed: {e}"));
            return ptr;
        }
    };
    let text = match std::str::from_utf8(&bytes) {
        Ok(s) => s,
        Err(e) => {
            throw(&mut env, &format!("filter data not valid UTF-8: {e}"));
            return ptr;
        }
    };
    let prev_generic_hide = unsafe { as_ref(ptr) }
        .map(|h| h.generic_hide)
        .unwrap_or(true);
    if ptr != 0 {
        unsafe {
            drop(Box::from_raw(ptr as *mut EngineHandle));
        }
    }
    let mut handle = EngineHandle::from_rules_text(text);
    handle.generic_hide = prev_generic_hide;
    into_raw(handle)
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeLoadSerialized(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    data: JByteArray,
) -> jlong {
    let bytes = match env.convert_byte_array(&data) {
        Ok(b) => b,
        Err(e) => {
            throw(&mut env, &format!("convert_byte_array failed: {e}"));
            return ptr;
        }
    };
    let prev_generic_hide = unsafe { as_ref(ptr) }
        .map(|h| h.generic_hide)
        .unwrap_or(true);
    let mut handle = match EngineHandle::from_serialized(&bytes) {
        Ok(h) => h,
        Err(e) => {
            throw(&mut env, &format!("failed to deserialize adblock engine data: {e}"));
            return ptr;
        }
    };
    if ptr != 0 {
        unsafe {
            drop(Box::from_raw(ptr as *mut EngineHandle));
        }
    }
    handle.generic_hide = prev_generic_hide;
    into_raw(handle)
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeSerialize(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) -> jbyteArray {
    let handle = match unsafe { as_ref(ptr) } {
        Some(h) => h,
        None => {
            throw(&mut env, "engine pointer is null");
            return std::ptr::null_mut();
        }
    };
    let bytes: Vec<u8> = handle.engine.serialize();
    match env.byte_array_from_slice(&bytes) {
        Ok(arr) => arr.into_raw(),
        Err(e) => {
            throw(&mut env, &format!("byte_array_from_slice failed: {e}"));
            std::ptr::null_mut()
        }
    }
}

fn resource_type_to_request_type(filter_option: i32) -> &'static str {
    match filter_option {
        1 => "script",
        2 => "image",
        4 => "stylesheet",
        0x10 => "xhr",
        0x40 => "sub_frame",
        0x80000 => "font",
        0x100000 => "media",
        _ => "other",
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeMatches(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    url: JString,
    document_url: JString,
    filter_option: jint,
) -> jobject {
    let handle = match unsafe { as_ref(ptr) } {
        Some(h) => h,
        None => {
            throw(&mut env, "engine pointer is null");
            return std::ptr::null_mut();
        }
    };
    let url_str: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(e) => {
            throw(&mut env, &format!("url GetStringUTFChars failed: {e}"));
            return std::ptr::null_mut();
        }
    };
    let doc_str: String = match env.get_string(&document_url) {
        Ok(s) => s.into(),
        Err(e) => {
            throw(&mut env, &format!("documentUrl GetStringUTFChars failed: {e}"));
            return std::ptr::null_mut();
        }
    };
    let request_type = resource_type_to_request_type(filter_option);
    let request = match Request::new(&url_str, &doc_str, request_type) {
        Ok(r) => r,
        Err(_) => {
            return build_match_result(&mut env, false, None, None);
        }
    };
    let result = handle.engine.check_network_request(&request);
    let matched_rule = result.filter.clone();
    let exception_rule = result.exception.clone();
    let should_block = result.matched && exception_rule.is_none();
    build_match_result(&mut env, should_block, matched_rule, exception_rule)
}

fn build_match_result(
    env: &mut JNIEnv,
    should_block: bool,
    matched_rule: Option<String>,
    exception_rule: Option<String>,
) -> jobject {
    let class = match env.find_class("io/github/edsuns/adblockclient/MatchResult") {
        Ok(c) => c,
        Err(e) => {
            throw(env, &format!("find_class MatchResult failed: {e}"));
            return std::ptr::null_mut();
        }
    };
    let matched_jobj: JObject = match matched_rule {
        Some(s) => env
            .new_string(s)
            .map(|j| j.into())
            .unwrap_or_else(|_| JObject::null()),
        None => JObject::null(),
    };
    let exception_jobj: JObject = match exception_rule {
        Some(s) => env
            .new_string(s)
            .map(|j| j.into())
            .unwrap_or_else(|_| JObject::null()),
        None => JObject::null(),
    };
    let obj = env.new_object(
        &class,
        "(ZLjava/lang/String;Ljava/lang/String;)V",
        &[
            JValue::Bool(if should_block { JNI_TRUE } else { JNI_FALSE }),
            JValue::Object(&matched_jobj),
            JValue::Object(&exception_jobj),
        ],
    );
    match obj {
        Ok(o) => o.into_raw(),
        Err(e) => {
            throw(env, &format!("new_object MatchResult failed: {e}"));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeGetElementHidingSelectors(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    url: JString,
) -> jstring {
    let handle = match unsafe { as_ref(ptr) } {
        Some(h) => h,
        None => return std::ptr::null_mut(),
    };
    if !handle.generic_hide {
        return std::ptr::null_mut();
    }
    let url_str: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let resources = handle.engine.url_cosmetic_resources(&url_str);
    if resources.hide_selectors.is_empty() {
        return std::ptr::null_mut();
    }
    let selectors: Vec<String> = resources.hide_selectors.into_iter().collect();
    let joined = selectors.join(",");
    match env.new_string(joined) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeGetExtendedCssSelectors(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    url: JString,
) -> jobjectArray {
    let handle = match unsafe { as_ref(ptr) } {
        Some(h) => h,
        None => return std::ptr::null_mut(),
    };
    let url_str: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let resources = handle.engine.url_cosmetic_resources(&url_str);
    if resources.procedural_actions.is_empty() {
        return std::ptr::null_mut();
    }
    let selectors: Vec<String> = resources.procedural_actions.into_iter().collect();
    strings_to_java_array(&mut env, &selectors)
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeGetCssRules(
    _env: JNIEnv,
    _class: JClass,
    _ptr: jlong,
    _url: JString,
) -> jobjectArray {
    std::ptr::null_mut()
}

fn java_string_array_to_vec(env: &mut JNIEnv, array: &JObjectArray) -> Vec<String> {
    let len = match env.get_array_length(array) {
        Ok(l) => l,
        Err(_) => return Vec::new(),
    };
    let mut out = Vec::with_capacity(len as usize);
    for i in 0..len {
        let obj = match env.get_object_array_element(array, i) {
            Ok(o) => o,
            Err(_) => continue,
        };
        let jstr = JString::from(obj);
        let s: String = match env.get_string(&jstr) {
            Ok(js) => js.into(),
            Err(_) => continue,
        };
        out.push(s);
    }
    out
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeGetHiddenClassIdSelectors(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    classes: JObjectArray,
    ids: JObjectArray,
    exceptions: JObjectArray,
) -> jobjectArray {
    let handle = match unsafe { as_ref(ptr) } {
        Some(h) => h,
        None => return std::ptr::null_mut(),
    };
    let classes_vec = java_string_array_to_vec(&mut env, &classes);
    let ids_vec = java_string_array_to_vec(&mut env, &ids);
    let exceptions_set: HashSet<String> =
        java_string_array_to_vec(&mut env, &exceptions).into_iter().collect();
    let selectors = handle
        .engine
        .hidden_class_id_selectors(&classes_vec, &ids_vec, &exceptions_set);
    if selectors.is_empty() {
        return std::ptr::null_mut();
    }
    strings_to_java_array(&mut env, &selectors)
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeGetScriptlets(
    mut env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    url: JString,
) -> jobjectArray {
    let handle = match unsafe { as_ref(ptr) } {
        Some(h) => h,
        None => return std::ptr::null_mut(),
    };
    let url_str: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let resources = handle.engine.url_cosmetic_resources(&url_str);
    if resources.injected_script.is_empty() {
        return std::ptr::null_mut();
    }
    let scriptlets = vec![resources.injected_script];
    strings_to_java_array(&mut env, &scriptlets)
}

fn strings_to_java_array(env: &mut JNIEnv, strings: &[String]) -> jobjectArray {
    let class = match env.find_class("java/lang/String") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };
    let array = match env.new_object_array(strings.len() as i32, &class, JObject::null()) {
        Ok(a) => a,
        Err(_) => return std::ptr::null_mut(),
    };
    for (i, s) in strings.iter().enumerate() {
        let jstr = match env.new_string(s) {
            Ok(j) => j,
            Err(_) => continue,
        };
        let _ = env.set_object_array_element(&array, i as i32, &jstr);
    }
    array.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeSetGenericHideEnabled(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
    enabled: jboolean,
) {
    if let Some(handle) = unsafe { as_ref(ptr) } {
        handle.generic_hide = enabled != JNI_FALSE;
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_edsuns_adblockrust_AdBlockRustClient_nativeIsGenericHideEnabled(
    _env: JNIEnv,
    _class: JClass,
    ptr: jlong,
) -> jboolean {
    match unsafe { as_ref(ptr) } {
        Some(h) => {
            if h.generic_hide {
                JNI_TRUE
            } else {
                JNI_FALSE
            }
        }
        None => JNI_TRUE,
    }
}
