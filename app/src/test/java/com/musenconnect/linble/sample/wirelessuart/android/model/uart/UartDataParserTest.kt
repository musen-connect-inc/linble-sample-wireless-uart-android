package com.musenconnect.linble.sample.wirelessuart.android.model.uart

import com.musenconnect.linble.sample.wirelessuart.android.asHexStringToByteArray
import com.musenconnect.linble.sample.wirelessuart.android.model.PrintlnDebugLogger
import com.musenconnect.linble.sample.wirelessuart.android.model.UartDataParser
import com.musenconnect.linble.sample.wirelessuart.android.model.UartDataParserCallback
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartEventSensorSampling
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseConnectionTest
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseDeviceNameRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseDeviceNameWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseRegisterRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseRegisterWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseSensorSampling
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartResponseVersionRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartRxPacket
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.SamplingState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private val debugLogger = PrintlnDebugLogger

class UartDataParserTest : UartDataParserCallback {
    private lateinit var uartDataParser: UartDataParser

    private var parsedPacket: UartRxPacket? = null

    @Before
    fun before() {
        uartDataParser = UartDataParser(this, debugLogger)
    }

    override fun onParse(rxPacket: UartRxPacket) {
        this.parsedPacket = rxPacket
    }

    @Test
    fun `parse_導通テスト_レスポンス`() {
        uartDataParser.parse("0140".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseConnectionTest) throw AssertionError()
    }

    @Test
    fun `parse_レジスタ値アクセス_取得_レスポンス`() {
        uartDataParser.parse("034102FF".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseRegisterRead) throw AssertionError()

        assertEquals(2.toByte(), parsedPacket.registerNumber.value)
        assertEquals(0xFF.toByte(), parsedPacket.hexdecimal)
    }

    @Test
    fun `parse_レジスタ値アクセス_設定_レスポンス`() {
        uartDataParser.parse("0142".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseRegisterWrite) throw AssertionError()
    }

    @Test
    fun `parse_ホストマイコンバージョン確認_レスポンス`() {
        uartDataParser.parse("0D43312E322E332E393837363534".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseVersionRead) throw AssertionError()

        assertEquals("1.2.3.987654", parsedPacket.version.value)

    }

    @Test
    fun `parse_センササンプリング実行要求_レスポンス`() {
        uartDataParser.parse("0144".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseSensorSampling) throw AssertionError()
    }


    @Test
    fun `parse_センササンプリング実行要求_イベント`() {
        /*
        ```
        <length>84<state>[<value>]
        ```

        |パラメータ|サイズ|説明|
        |:---|:---:|:---|
        |`<state>`|1|サンプリング継続状態。`1`または`0`。`1`の場合、後続に`<value>`が出現する。|
        |`<value>`|4|取得したセンサ値のIEEE754単精度(float)表現の値。例：`42009062` (`32.141`)|

        **例:**

        ```
        Tx | 020405
        Rx | 0144
        Rx | 06840142F7D2F1 // 123.91199493408203125
        Rx | 06840142F85062 // 124.1569976806640625
        Rx | 06840142F82C8B // 124.08699798583984375
        Rx | 06840142F7FBE7 // 123.99199676513671875
        Rx | 06840142F7C51E // 123.8849945068359375
        Rx | 028400
        ```

         */
        run {
            uartDataParser.parse("06840142F7D2F1".asHexStringToByteArray())

            val parsedPacket = parsedPacket

            if (parsedPacket !is UartEventSensorSampling) throw AssertionError()

            assertEquals(SamplingState.Sampling, parsedPacket.state)
            assertEquals(123.91199493408203125f, parsedPacket.value!!, 0.000001f)
        }

        run {
            uartDataParser.parse("028400".asHexStringToByteArray())

            val parsedPacket = parsedPacket

            if (parsedPacket !is UartEventSensorSampling) throw AssertionError()

            assertEquals(SamplingState.Stopped, parsedPacket.state)
            assertEquals(null, parsedPacket.value)
        }
    }

    @Test
    fun `parse_デバイス名_取得_レスポンス`() {
        uartDataParser.parse("1A4553616D706C652D55617274436f6E74726F6C6C65722D303031".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseDeviceNameRead) throw AssertionError()

        assertEquals("Sample-UartController-001", parsedPacket.name.value)
    }


    @Test
    fun `parse_デバイス名_設定_レスポンス`() {
        uartDataParser.parse("0146".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseDeviceNameWrite) throw AssertionError()
    }


    @Test
    fun `parse_データなし`() {
        uartDataParser.parse("".asHexStringToByteArray())

        assertEquals(null, parsedPacket)
    }

    @Test
    fun `parse_大きすぎるlength`() {
        uartDataParser.parse("ff".asHexStringToByteArray())

        assertEquals(null, parsedPacket)
    }

    @Test
    fun `parse_未知のrxType`() {
        uartDataParser.parse("01ff".asHexStringToByteArray())

        assertEquals(null, parsedPacket)
    }

    @Test
    fun `parse_分割入力`() {
        uartDataParser.parse("1A4553616D706C652D55617274".asHexStringToByteArray())
        uartDataParser.parse("436f6E74726F".asHexStringToByteArray())
        uartDataParser.parse("6C6C65722D3030".asHexStringToByteArray())
        uartDataParser.parse("31".asHexStringToByteArray())

        val parsedPacket = parsedPacket

        if (parsedPacket !is UartResponseDeviceNameRead) throw AssertionError()

        assertEquals("Sample-UartController-001", parsedPacket.name.value)
    }

}