package com.musenconnect.linble.sample.wirelessuart.android

val Any.logTag: String
    get() = this.javaClass.simpleName

fun ByteArray.description(): String {
    return "<size=${this.size}, hex=${this.joinToString("") { "%02X".format(it) }}>"
}

fun List<Byte>.description(): String {
    return this.toByteArray().description()
}

fun ByteArray.toHexString(): String {
    return this.joinToString("") { "%02X".format(it) }
}

fun String.asHexStringToByteArray(): ByteArray {
    return this.mapIndexed { index, c -> index }
        .filter { it.rem(2) == 0 }
        .map { (this[it].toString() + this[it+1].toString()).toInt(16).toByte() }
        .toByteArray()
}