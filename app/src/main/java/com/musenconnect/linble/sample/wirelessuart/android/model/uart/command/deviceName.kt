package com.musenconnect.linble.sample.wirelessuart.android.model.uart.command

import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.AsciiString


/** デバイス名.取得.コマンド */
class UartCommandDeviceNameRead : UartCommand() {
    override val length: Byte = 1
    override val type: Byte = 0x05

    override fun createPayload(): ByteArray? {
        return null
    }
}

/** デバイス名.取得.レスポンス */
class UartResponseDeviceNameRead(rxPayload: ByteArray) : UartResponse(rxPayload) {
    val name = AsciiString(rxPayload)

    companion object {
        val type: Byte = 0x45
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}: name=$name>"
    }
}


/** デバイス名.設定.コマンド */
class UartCommandDeviceNameWrite(val name: AsciiString) : UartCommand() {
    override val length: Byte = (1 + name.value.length).toByte()
    override val type: Byte = 0x06

    override fun createPayload(): ByteArray {
        return name.toByteArray()
    }
}

/** デバイス名.設定.レスポンス */
class UartResponseDeviceNameWrite(rxPayload: ByteArray) : UartResponse(rxPayload) {
    companion object {
        val length: Byte = 1
        val type: Byte = 0x46
    }
}
