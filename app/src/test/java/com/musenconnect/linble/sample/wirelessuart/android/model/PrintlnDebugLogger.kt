package com.musenconnect.linble.sample.wirelessuart.android.model

object PrintlnDebugLogger : DebugLogger {
    override fun logd(tag: String, message: String) {
        println("D/$tag: $message")
    }

    override fun logw(tag: String, message: String) {
        println("W/$tag: $message")
    }

    override fun loge(tag: String, message: String) {
        println("E/$tag: $message")
    }
}