package com.musenconnect.linble.sample.wirelessuart.android.common

import com.musenconnect.linble.sample.wirelessuart.android.asHexStringToByteArray
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.command.UartCommand
import com.musenconnect.linble.sample.wirelessuart.android.toHexString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private val debugLogger = PrintlnDebugLogger

class WirelessUartControllerTest {
    private lateinit var bluetoothCentralController: StubBluetoothCentralController
    private lateinit var wirelessUartController: WirelessUartController

    @Before
    fun before() {
        bluetoothCentralController = StubBluetoothCentralController()
        wirelessUartController = WirelessUartController(bluetoothCentralController, debugLogger)
    }

    @Test
    fun startAndStop() {
        assertEquals(false, bluetoothCentralController.isConnected)

        wirelessUartController.start()

        assertEquals(true, bluetoothCentralController.scanAdvertisementsCalled)
        assertEquals(2, bluetoothCentralController.scanAdvertisementsCalledCount)
        assertEquals(true, bluetoothCentralController.connectCalled)
        assertEquals(true, bluetoothCentralController.discoverServicesAndCharacteristicsCalled)
        assertEquals(true, bluetoothCentralController.enableNotificationCalled)

        assertEquals(true, bluetoothCentralController.isConnected)

        wirelessUartController.stop()

        assertEquals(true, bluetoothCentralController.cancelScanCalled)
        assertEquals(true, bluetoothCentralController.cancelConnectionCalled)

        assertEquals(false, bluetoothCentralController.isConnected)
    }

    @Test
    fun write() {
        wirelessUartController.start()

        val command43byte = object : UartCommand() {
            override val length: Byte = 0x0A

            override val type: Byte = 0x0B

            override fun createPayload(): ByteArray? {
                return "0102030405060708091011121314151617180102030405060708091011121314151617181920AABBCC".asHexStringToByteArray()
            }
        }

        wirelessUartController.write(command43byte)

        assertEquals(3, bluetoothCentralController.writeCalledCount)
        assertEquals("AABBCC", bluetoothCentralController.lastWroteData!!.toHexString())
    }
}