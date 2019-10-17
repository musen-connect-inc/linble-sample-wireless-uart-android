package com.musenconnect.linble.sample.wirelessuart.android.common

interface DebugLogger {
    fun logd(tag: String, message: String)

    fun logw(tag: String, message: String)

    fun loge(tag: String, message: String)
}
