package io.github.edsuns.adfilter.util

import java.math.BigInteger
import java.security.MessageDigest

val ByteArray.sha256: String
    get() = sha("SHA-256", this)


fun ByteArray.verifySha256(sha256: String): Boolean {
    return this.sha256 == sha256
}

val String.sha1: String
    get() = sha("SHA-1", this.toByteArray())

fun String.verifySha1(sha1: String): Boolean {
    return this.sha1 == sha1
}

private fun sha(algorithm: String, bytes: ByteArray): String {
    val digest = hash(algorithm, bytes)
    return String.format("%0" + digest.size * 2 + "x", BigInteger(1, digest))
}

fun md5(bytes: ByteArray): ByteArray {
    return hash("MD5", bytes)
}

private fun hash(algorithm: String, bytes: ByteArray): ByteArray {
    val md = MessageDigest.getInstance(algorithm)
    return md.digest(bytes)
}