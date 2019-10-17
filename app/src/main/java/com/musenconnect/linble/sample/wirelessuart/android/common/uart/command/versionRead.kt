package com.musenconnect.linble.sample.wirelessuart.android.common.uart.command

import com.musenconnect.linble.sample.wirelessuart.android.common.uart.datatype.AsciiString

/** ホストマイコンバージョン確認.コマンド */
class UartCommandVersionRead: UartCommand() {
    override val length: Byte = 1
    override val type: Byte = 0x03

    override fun createPayload(): ByteArray? = null
}

/** ホストマイコンバージョン確認.レスポンス（可変長） */
class UartResponseVersionRead(rxPayload: ByteArray): UartResponse(rxPayload) {
    val version = AsciiString(rxPayload)

    companion object {
        val type: Byte = 0x43
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}: version=$version>"
    }
}