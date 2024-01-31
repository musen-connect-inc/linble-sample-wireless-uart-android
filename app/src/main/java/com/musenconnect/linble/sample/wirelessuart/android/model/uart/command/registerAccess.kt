package com.musenconnect.linble.sample.wirelessuart.android.model.uart.command

import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.RegisterNumber


/** レジスタ値アクセス.取得.コマンド */
class UartCommandRegisterRead(val registerNumber: RegisterNumber) : UartCommand() {
    override val length: Byte = 2
    override val type: Byte = 0x01

    override fun createPayload(): ByteArray {
        return arrayOf(registerNumber.value).toByteArray()
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}: registerNumber=$registerNumber>"
    }
}

/** レジスタ値アクセス.取得.レスポンス */
class UartResponseRegisterRead(rxPayload: ByteArray) : UartResponse(rxPayload) {
    val registerNumber = RegisterNumber(rxPayload[0])
    val hexdecimal = rxPayload[1]

    companion object {
        val length: Byte = 3
        val type: Byte = 0x41
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}: registerNumber=$registerNumber, hexdecimal=${
            "0x%02X".format(
                hexdecimal
            )
        }>"
    }
}


/** レジスタ値アクセス.設定.コマンド */
class UartCommandRegisterWrite(val registerNumber: RegisterNumber, val hexdecimal: Byte) :
    UartCommand() {
    override val length: Byte = 3
    override val type: Byte = 0x02

    override fun createPayload(): ByteArray {
        return arrayOf(registerNumber.value, hexdecimal).toByteArray()
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}: registerNumber=$registerNumber, hexdecimal=${
            "0x%02X".format(
                hexdecimal
            )
        }>"
    }
}

/** レジスタ値アクセス.設定.レスポンス */
class UartResponseRegisterWrite(rxPayload: ByteArray) : UartResponse(rxPayload) {
    companion object {
        val length: Byte = 1
        val type: Byte = 0x42
    }
}
