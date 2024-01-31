package com.musenconnect.linble.sample.wirelessuart.android.model.uart

import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandConnectionTest
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandDeviceNameRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandDeviceNameWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandRegisterRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandRegisterWrite
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandSensorSampling
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommandVersionRead
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.AsciiString
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.DurationIntervalSeconds
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype.RegisterNumber
import com.musenconnect.linble.sample.wirelessuart.android.toHexString
import org.junit.Assert.assertEquals
import org.junit.Test

class UartCommandCreationTest {
    @Test
    fun `create_導通テスト_コマンド`() {
        assertEquals("0100", UartCommandConnectionTest().toByteArray().toHexString())
    }

    @Test
    fun `create_レジスタ値アクセス_取得_コマンド`() {
        assertEquals(
            "020102",
            UartCommandRegisterRead(RegisterNumber(2)).toByteArray().toHexString()
        )
    }

    @Test
    fun `create_レジスタ値アクセス_設定_コマンド`() {
        assertEquals(
            "0302040F",
            UartCommandRegisterWrite(RegisterNumber(4), 0x0F).toByteArray().toHexString()
        )
    }

    @Test
    fun `create_ホストマイコンバージョン確認_コマンド`() {
        assertEquals("0103", UartCommandVersionRead().toByteArray().toHexString())
    }

    @Test
    fun `create_センササンプリング実行要求_コマンド`() {
        assertEquals(
            "020405",
            UartCommandSensorSampling(DurationIntervalSeconds(5)).toByteArray().toHexString()
        )
    }

    @Test
    fun `create_デバイス名_取得_コマンド`() {
        assertEquals("0105", UartCommandDeviceNameRead().toByteArray().toHexString())
    }

    @Test
    fun `create_デバイス名_設定_コマンド`() {
        assertEquals(
            "1A0653616D706C652D55617274436F6E74726F6C6C65722D303031",
            UartCommandDeviceNameWrite(AsciiString("Sample-UartController-001")).toByteArray()
                .toHexString()
        )
    }
}