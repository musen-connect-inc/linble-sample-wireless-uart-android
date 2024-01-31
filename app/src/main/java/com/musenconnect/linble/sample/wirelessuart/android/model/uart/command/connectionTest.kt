package com.musenconnect.linble.sample.wirelessuart.android.model.uart.command

/** 導通テスト.コマンド */
class UartCommandConnectionTest : UartCommand() {
    override val length: Byte = 1
    override val type: Byte = 0x00

    override fun createPayload(): ByteArray? = null
}

/** 導通テスト.レスポンス */
class UartResponseConnectionTest(rxPayload: ByteArray) : UartResponse(rxPayload) {
    companion object {
        val length: Byte = 1
        val type: Byte = 0x40
    }
}