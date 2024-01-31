package com.musenconnect.linble.sample.wirelessuart.android.model

enum class OperationStep {
    Initializing,
    Scanning,
    Connecting,
    Connected;

    override fun toString(): String {
        return when (this) {
            Initializing -> "初期化中"
            Scanning -> "スキャン中"
            Connecting -> "接続中"
            Connected -> "通信可能"
        }
    }
}