package com.musenconnect.linble.sample.wirelessuart.android.model

import com.musenconnect.linble.sample.wirelessuart.android.logTag
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.Advertisement
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.BluetoothCentralController
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol.NotificationEvent
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartCommand
import com.musenconnect.linble.sample.wirelessuart.android.model.uart.command.UartRxPacket
import java.util.Locale

class WirelessUartController(
    private val bluetoothCentralController: BluetoothCentralController,
    private val debugLogger: DebugLogger
) {
    companion object {
        val defaultTargetLinbleAddress: String =
            "FFFFFFFFFFFF" // `BTM` コマンドで確認できるBDアドレス文字列をここに貼り付けてください。
                .insertColons()
    }

    val targetLinbleAddress = defaultTargetLinbleAddress

    var onChangeDeviceBluetoothState: ((DeviceBluetoothState) -> Unit)? = null

    fun start() {
        debugLogger.logd(this@WirelessUartController.logTag, "start:")

        bluetoothCentralController.startDeviceBluetoothStateMonitoring(
            deviceBluetoothStateMonitoringCallback
        )
    }

    private val deviceBluetoothStateMonitoringCallback =
        object : BluetoothCentralController.DeviceBluetoothStateMonitoringCallback {
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
            bluetoothCentralController.cancelConnection()
        }

        if (bluetoothCentralController.isConnected) {
            // 接続済みである場合、スキャンは行わせません。
            debugLogger.loge(this@WirelessUartController.logTag, "startScan: ignored")
            return
        }

        operationStep = OperationStep.Scanning
        bluetoothCentralController.scanAdvertisements(scanCallback)
    }

    private val scanCallback = object : BluetoothCentralController.ScanAdvertisementsCallback {
        override fun onScanned(advertisement: Advertisement) {
            // 通知された `Advertisement` のフィールドを確認し、
            // 接続対象からのアドバタイズかを判別します。

            if (advertisement.deviceAddress != targetLinbleAddress) {
                return
            }

            debugLogger.logw(
                this@WirelessUartController.logTag,
                "onScanned: advertisement=$advertisement"
            )

            bluetoothCentralController.cancelScan()

            operationStep = OperationStep.Connecting
            bluetoothCentralController.connect(
                advertisement,
                linbleSetupCallback
            )
        }
    }

    private val linbleSetupCallback =
        object : BluetoothCentralController.LinbleSetupCallback {
            override fun onError(reason: Throwable) {
                debugLogger.loge(
                    this@WirelessUartController.logTag,
                    "connect.onError: reason=$reason"
                )

                startScan(toReconnect = true)
            }

            override fun onComplete() {
                // LINBLEとの通信準備完了
                debugLogger.logw(
                    this@WirelessUartController.logTag,
                    "enableNotification.onSuccess: setup completed!"
                )

                uartDataParser.clear()

                operationStep = OperationStep.Connected
            }

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
        val txPacketDivider = this.txPacketDivider ?: return

        debugLogger.logd(
            this@WirelessUartController.logTag,
            "writeFragmentedByteArrayList: remainPacketSize=${txPacketDivider.remain.size}"
        )

        val fragmentedByteArray = txPacketDivider.next ?: run {
            debugLogger.logw(this@WirelessUartController.logTag, "txPacketDivider.next == null")

            this.txPacketDivider = null
            return
        }

        bluetoothCentralController.write(
            fragmentedByteArray,
            object : BluetoothCentralController.WriteOperationCallback {
                override fun onError(reason: Throwable) {
                    debugLogger.loge(
                        this@WirelessUartController.logTag,
                        "write.onError: reason=$reason"
                    )
                }

                override fun onSuccess() {
                    debugLogger.logw(this@WirelessUartController.logTag, "write.onSuccess:")

                    // 次のデータを送信する
                    writeFragmentedByteArrayList()
                }
            })
    }

    fun stop() {
        debugLogger.logw(this@WirelessUartController.logTag, "stop:")

        txPacketDivider = null

        bluetoothCentralController.stopDeviceBluetoothStateMonitoring()
        bluetoothCentralController.cancelScan()
        bluetoothCentralController.cancelConnection()
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
            "${this[index]}${this[index + 1]}".uppercase(Locale.US)
        }
}
