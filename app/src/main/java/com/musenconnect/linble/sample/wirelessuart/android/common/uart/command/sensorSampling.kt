package com.musenconnect.linble.sample.wirelessuart.android.common.uart.command

import com.musenconnect.linble.sample.wirelessuart.android.common.uart.datatype.DurationIntervalSeconds
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.datatype.SamplingState

private val Collection<Byte>.asCLanguageUnsignedLong: Int
    get() {
        /*
        {0x4d, 0x8e, 0xf3, 0xc2}
        のCollection<Byte>を
        0x4d8ef3c2
        のIntに変換
         */
        return this.map { it.toInt() }
            .mapIndexed { index, i -> i.shl((3 - index) * 8).and(0x0FF.shl((3 - index) * 8)) }
            .reduce { acc, i -> acc or i }
    }

/** センササンプリング実行要求.コマンド */
class UartCommandSensorSampling(val duration: DurationIntervalSeconds): UartCommand() {
    override val length: Byte = 2
    override val type: Byte = 0x04

    override fun createPayload(): ByteArray? {
        return arrayOf(duration.value).toByteArray()
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}: duration=$duration>"
    }
}

/** センササンプリング実行要求.レスポンス */
class UartResponseSensorSampling(rxPayload: ByteArray): UartResponse(rxPayload) {
    companion object {
        val length: Byte = 1
        val type: Byte = 0x44
    }
}

/** センササンプリング実行要求.イベント（可変長） */
class UartEventSensorSampling(rxPayload: ByteArray): UartEvent(rxPayload) {
    val state = SamplingState.from(rxPayload[0])
    val value: Float? = if (state == SamplingState.Sampling) java.lang.Float.intBitsToFloat(rxPayload.drop(1).take(4).asCLanguageUnsignedLong) else null

    companion object {
        val type: Byte = 0x84.toByte()
    }

    override fun toString(): String {
        return "<${javaClass.simpleName}: state=$state, value=$value>"
    }
}