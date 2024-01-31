package com.musenconnect.linble.sample.wirelessuart.android.model

import com.musenconnect.linble.sample.wirelessuart.android.description
import com.musenconnect.linble.sample.wirelessuart.android.logTag
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartEventSensorSampling
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseConnectionTest
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseDeviceNameRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseDeviceNameWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseRegisterRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseRegisterWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseSensorSampling
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseVersionRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartRxPacket

interface UartDataParserCallback {
    fun onParse(rxPacket: UartRxPacket)
}

class RxPacketFactory {
    companion object {
        fun create(rxType: Byte, rxPayload: ByteArray): UartRxPacket? {
            return when (rxType) {
                UartResponseConnectionTest.type -> UartResponseConnectionTest(rxPayload)
                UartResponseRegisterWrite.type -> UartResponseRegisterWrite(rxPayload)
                UartResponseRegisterRead.type -> UartResponseRegisterRead(rxPayload)
                UartResponseVersionRead.type -> UartResponseVersionRead(rxPayload)
                UartResponseSensorSampling.type -> UartResponseSensorSampling(rxPayload)
                UartResponseDeviceNameRead.type -> UartResponseDeviceNameRead(rxPayload)
                UartResponseDeviceNameWrite.type -> UartResponseDeviceNameWrite(rxPayload)
                UartEventSensorSampling.type -> UartEventSensorSampling(rxPayload)

                else -> null
            }
        }
    }
}

class UartDataParser(
    private val callback: UartDataParserCallback,
    private val debugLogger: DebugLogger
) {
    private var rxDataBuffer = ByteArray(0)

    fun clear() {
        rxDataBuffer = ByteArray(0)
    }

    fun parse(data: ByteArray) {
        debugLogger.logd(logTag, "parse: data=${data.description()}")

        try {
            // rxDataBufferの末尾に新規受信データを追加する
            rxDataBuffer += data
            debugLogger.logd(logTag, "parse: rxDataBuffer=${rxDataBuffer.description()}")

            while (rxDataBuffer.isNotEmpty()) {
                // rxDataBufferのindex=0から1個のデータを参照し、lengthとする
                val length = rxDataBuffer[0]
                debugLogger.logd(logTag, "parse: length=$length")

                // rxDataBufferのindex=1からlength個のデータを参照し、followingとする
                val following = rxDataBuffer.slice(1..length)
                debugLogger.logd(logTag, "parse: following=${following.description()}")

                // rxDataBufferからlengthとfollowing部分のデータを削除する
                rxDataBuffer = this.rxDataBuffer.drop(length + 1).toByteArray()

                // followingのindex=0から1個のデータを取り出し、rxTypeとする
                val rxType = following[0]
                debugLogger.logd(logTag, "parse: rxType=${"0x%02X".format(rxType)}")

                // followingのindex=1からlength-1個のデータを取り出し、rxPayloadとする
                val rxPayload = following.slice(1 until length).toByteArray() // 1..(length-1)
                debugLogger.logd(logTag, "parse: rxPayload=${rxPayload.description()}")

                // rxTypeと一致するtypeを持つUartRxPacketのサブクラスを特定し、そのオブジェクトを生成する
                RxPacketFactory.create(rxType, rxPayload)?.let { rxPacket ->
                    debugLogger.logd(
                        logTag,
                        "parse: rxPacketClass=${rxPacket.javaClass.simpleName}"
                    )

                    // 生成オブジェクトを解析結果として上層へ通知
                    callback.onParse(rxPacket)
                }
            }
        } catch (e: Exception) {
            when (e) {
                is IndexOutOfBoundsException -> {
                    // 準正常系, printも不要
                }

                is ClassNotFoundException, is InstantiationException -> {
                    // 準正常系
                    e.printStackTrace()
                }

                else -> {
                    // 異常系

                    e.printStackTrace()

                    throw e
                }
            }
        }
    }
}
