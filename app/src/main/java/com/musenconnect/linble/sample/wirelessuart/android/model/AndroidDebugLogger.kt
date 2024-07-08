package com.musenconnect.linble.sample.wirelessuart.android.model

import android.util.Log

object AndroidDebugLogger : DebugLogger {
    override fun logd(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun logw(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun loge(tag: String, message: String) {
        Log.e(tag, message)
    }
}
