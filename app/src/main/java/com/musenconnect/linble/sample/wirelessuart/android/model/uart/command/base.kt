package com.musenconnect.linble.sample.wirelessuart.android.model.uart.command

/**
 * コマンド
 *
 * 端末からのデータ送信時に利用
 */
abstract class UartCommand {
    protected abstract val length: Byte
    protected abstract val type: Byte

    protected abstract fun createPayload(): ByteArray?

    private val payload: ByteArray? by lazy { createPayload() }

    fun toByteArray(): ByteArray {
        var byteArray = arrayOf(length, type).toByteArray()

        payload?.let { byteArray += it }

        return byteArray
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}>"
    }
}


/**
 * 受信パケット.
 *
 * LINBLEからのデータ受信時に利用
 */
abstract class UartRxPacket(rxPayload: ByteArray) {
    override fun toString(): String {
        return "<${javaClass.simpleName}>"
    }
}


/** レスポンス */
abstract class UartResponse(rxPayload: ByteArray) : UartRxPacket(rxPayload)

abstract class UartEvent(rxPayload: ByteArray) : UartRxPacket(rxPayload)


