package com.musenconnect.linble.sample.wirelessuart.android.common

import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.*
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.command.UartCommand
import com.musenconnect.linble.sample.wirelessuart.android.common.uart.command.UartRxPacket
import com.musenconnect.linble.sample.wirelessuart.android.logTag
import java.util.*

class WirelessUartController(
    private val bluetoothCentralController: BluetoothCentralController,
    private val debugLogger: DebugLogger
) {
    private var connected: Linble? = null

    companion object {
        val defaultTargetLinbleAddress: String = "FFFFFFFFFFFF" // `BTM` コマンドで確認できるBDアドレス文字列をここに貼り付けてください。
            .insertColons()
    }

    val targetLinbleAddress = defaultTargetLinbleAddress

    var onChangeDeviceBluetoothState: ((DeviceBluetoothState) -> Unit)? = null

    fun start() {
        debugLogger.logd(this@WirelessUartController.logTag, "start:")

        bluetoothCentralController.startDeviceBluetoothStateMonitoring(deviceBluetoothStateMonitoringCallback)

        /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.startDeviceBluetoothStateMonitoring */
    }

    private val deviceBluetoothStateMonitoringCallback = object : BluetoothCentralController.DeviceBluetoothStateMonitoringCallback {
        override fun onChange(deviceBluetoothState: DeviceBluetoothState) {
            onChangeDeviceBluetoothState?.invoke(deviceBluetoothState)

            when (deviceBluetoothState) {
                DeviceBluetoothState.PoweredOn -> {
                    startScan()
                }
                else -> {
                    // NOP
                }
            }
        }
    }

    private fun startScan(toReconnect: Boolean = false) {
        debugLogger.logd(this@WirelessUartController.logTag, "startScan: toReconnect=$toReconnect")

        if (toReconnect) {
            connected = null
            bluetoothCentralController.cancelConnection()

            /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.cancelConnection */
        }

        if (connected != null) {
            // 接続済みである場合、スキャンは行わせません。
            debugLogger.loge(this@WirelessUartController.logTag, "startScan: ignored")
            return
        }

        operationStep = OperationStep.Scanning
        bluetoothCentralController.scanAdvertisements(scanCallback)

        /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.scanAdvertisements */
    }

    private val scanCallback = object : BluetoothCentralController.ScanAdvertisementsCallback {
        override fun onScanned(advertisement: Advertisement) {
            // 通知された `Advertisement` のフィールドを確認し、
            // 接続対象からのアドバタイズかを判別します。

            if (advertisement.deviceAddress != targetLinbleAddress) {
                return
            }

            debugLogger.logw(this@WirelessUartController.logTag, "onScanned: advertisement=$advertisement")

            bluetoothCentralController.cancelScan()

            /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.cancelScan */

            operationStep = OperationStep.Connecting
            bluetoothCentralController.connect(advertisement, connectCallback)

            /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.connect */
        }
    }

    private val connectCallback = object : Linble.GattOperationCallback {
        override fun onError(reason: Throwable) {
            debugLogger.loge(this@WirelessUartController.logTag, "connect.onError: reason=$reason")

            startScan(toReconnect = true)
        }

        override fun onSuccess(linble: Linble) {
            connected = linble

            linble.discoverServicesAndCharacteristics(discoverCallback)

            /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteLinble.discoverServicesAndCharacteristics */
        }
    }

    private val discoverCallback = object : Linble.GattOperationCallback {
        override fun onError(reason: Throwable) {
            debugLogger.loge(this@WirelessUartController.logTag, "discover.onError: reason=$reason")

            startScan(toReconnect = true)
        }

        override fun onSuccess(linble: Linble) {
            linble.enableNotification(enableNotificationCallback, notificationCallback)

            /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteLinble.enableNotification */
        }
    }

    private val enableNotificationCallback = object : Linble.GattOperationCallback {
        override fun onError(reason: Throwable) {
            debugLogger.loge(this@WirelessUartController.logTag, "enableNotification.onError: reason=$reason")

            startScan(toReconnect = true)
        }

        override fun onSuccess(linble: Linble) {
            // LINBLEとの通信準備完了
            debugLogger.logw(this@WirelessUartController.logTag, "enableNotification.onSuccess: setup completed!")

            uartDataParser.clear()

            operationStep = OperationStep.Connected
        }
    }

    private val notificationCallback = object : Linble.NotificationCallback {
        override fun onNotify(notificationEvent: NotificationEvent) {
            uartDataParser.parse(notificationEvent.data)
        }
    }

    private val innerUartDataParserCallback = object : UartDataParserCallback {
        override fun onParse(rxPacket: UartRxPacket) {
            debugLogger.logd(this@WirelessUartController.logTag, "onParse: $rxPacket")

            uartDataParserCallback?.onParse(rxPacket)
        }
    }

    var uartDataParserCallback: UartDataParserCallback? = null

    private val uartDataParser = UartDataParser(innerUartDataParserCallback, debugLogger)

    private var txPacketDivider: TxPacketDivider? = null

    fun write(command: UartCommand) {
        debugLogger.logd(this@WirelessUartController.logTag, "write: $command")

        txPacketDivider = TxPacketDivider(command.toByteArray(), size = 20)

        writeFragmentedByteArrayList()
    }

    private fun writeFragmentedByteArrayList() {
        val connectedLinble = this.connected ?: return
        val txPacketDivider = this.txPacketDivider ?: return

        debugLogger.logd(this@WirelessUartController.logTag, "writeFragmentedByteArrayList: remainPacketSize=${txPacketDivider.remain.size}")

        val fragmentedByteArray = txPacketDivider.next ?: run {
            debugLogger.logw(this@WirelessUartController.logTag, "txPacketDivider.next == null")

            this.txPacketDivider = null
            return
        }

        connectedLinble.write(fragmentedByteArray, object : Linble.GattOperationCallback {
            override fun onError(reason: Throwable) {
                debugLogger.loge(this@WirelessUartController.logTag, "write.onError: reason=$reason")
            }

            override fun onSuccess(linble: Linble) {
                debugLogger.logw(this@WirelessUartController.logTag, "write.onSuccess:")

                // 次のデータを送信する
                writeFragmentedByteArrayList()
            }
        })

        /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteLinble.write */
    }

    fun stop() {
        debugLogger.logw(this@WirelessUartController.logTag, "stop:")

        txPacketDivider = null

        bluetoothCentralController.stopDeviceBluetoothStateMonitoring()
        bluetoothCentralController.cancelScan()
        bluetoothCentralController.cancelConnection()

        /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.stopDeviceBluetoothStateMonitoring */
        /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.cancelScan */
        /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController.cancelConnection */

        connected?.cancelConnection()
        connected = null

        /** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteLinble.cancelConnection */
    }

    var onChangeOperationStep: ((OperationStep) -> Unit)? = null

    var operationStep: OperationStep = OperationStep.Initializing
        private set(value) {
            field = value

            debugLogger.logd(this@WirelessUartController.logTag, "onChangeOperationStep: $value")

            onChangeOperationStep?.invoke(value)
        }
}

private fun String.insertColons(): String {
    return this.indices
        .filter { it % 2 == 0 }
        .joinToString(":") { index ->
            "${this[index]}${this[index+1]}".toUpperCase(Locale.US)
        }
}
