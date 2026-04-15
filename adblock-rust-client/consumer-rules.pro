# JNI-crossing types and methods used by libadblock_rust_client.so.
# The Rust JNI layer constructs MatchResult via its 3-arg constructor
# and looks up native methods on AdBlockRustClient by their exact names.
-keepclassmembers class io.github.edsuns.adblockclient.MatchResult {
    <init>(boolean, java.lang.String, java.lang.String);
}

-keepclasseswithmembernames class io.github.edsuns.adblockrust.AdBlockRustClient {
    native <methods>;
}
