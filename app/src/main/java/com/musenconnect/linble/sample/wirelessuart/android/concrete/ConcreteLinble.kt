package com.musenconnect.linble.sample.wirelessuart.android.concrete

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.musenconnect.linble.sample.wirelessuart.android.common.DebugLogger
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.BluetoothLowEnergySpec
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.Linble
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.NotificationEvent
import com.musenconnect.linble.sample.wirelessuart.android.description
import com.musenconnect.linble.sample.wirelessuart.android.logTag
import java.util.*
import kotlin.concurrent.schedule

class ConcreteLinble(
    private var gatt: BluetoothGatt?,
    private val bluetoothGattCallbackBridger: BluetoothGattCallbackBridger,
    private val debugLogger: DebugLogger
): Linble {
    companion object {
        const val gattOperationTimeoutMillis: Long = 5000
    }

    private var gattOperationTimeoutDetector: TimerTask? = null

    override fun discoverServicesAndCharacteristics(gattOperationCallback: Linble.GattOperationCallback) {
        debugLogger.logd(logTag, "discoverServicesAndCharacteristics:")

        val gatt = this.gatt ?: run {
            return gattOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        bluetoothGattCallbackBridger.register(object: BluetoothGattCallback() {
            override fun onServicesDiscovered(serviceDiscoveredGatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(serviceDiscoveredGatt, status)

                gattOperationTimeoutDetector?.cancel()
                bluetoothGattCallbackBridger.unregister(this)

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    gattOperationCallback.onError(ResponseFailureGattOperationException("onServicesDiscovered"))
                    return
                }

                val succeeded = run {
                    val linbleUartService = serviceDiscoveredGatt?.services?.firstOrNull { it.uuid == Linble.GattUuid.linbleUartService } ?: return@run false
                    val characteristics = linbleUartService.characteristics ?: return@run false

                    dataFromPeripheral = characteristics.firstOrNull { it.uuid == Linble.GattUuid.dataFromPeripheral } ?: return@run false
                    dataFromPeripheralCccd = dataFromPeripheral?.getDescriptor(BluetoothLowEnergySpec.GattUuid.cccd) ?: return@run false
                    dataToPeripheral = characteristics.firstOrNull { it.uuid == Linble.GattUuid.dataToPeripheral } ?: return@run false

                    return@run true
                }

                if (!succeeded) {
                    gattOperationCallback.onError(UnsupportedDeviceConnectedException())
                    return
                }

                gattOperationCallback.onSuccess(this@ConcreteLinble)
            }
        })

        gattOperationTimeoutDetector = Timer().schedule(gattOperationTimeoutMillis) {
            gattOperationCallback.onError(TimeoutGattOperationException("discoverServices"))
        }

        val requested = gatt.discoverServices()
        if (!requested) {
            gattOperationTimeoutDetector?.cancel()

            gattOperationCallback.onError(RequestFailureGattOperationException("discoverServices"))
        }
    }

    override var dataFromPeripheral: BluetoothGattCharacteristic? = null
    override var dataFromPeripheralCccd: BluetoothGattDescriptor? = null

    override var dataToPeripheral: BluetoothGattCharacteristic? = null

    override fun enableNotification(gattOperationCallback: Linble.GattOperationCallback, notificationCallback: Linble.NotificationCallback) {
        debugLogger.logd(logTag, "enableNotification:")

        val gatt = this.gatt ?: run {
            return gattOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        val dataFromPeripheral = this.dataFromPeripheral ?: run {
            return gattOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        val dataFromPeripheralCccd = this.dataFromPeripheralCccd ?: run {
            return gattOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        // AndroidでNotificationEnable操作をするためには、以下の2手順を行う必要があります。

        /*
        1. `BluetoothGatt.setCharacteristicNotification()` による、AndroidOSへのNotification受信イベント通知の許可

        対象キャラクタリスティックからNotificationが届いた場合、
        このアプリの `BluetoothGattCallback` にもNotification受信イベントを届けるよう、AndroidOSへ指示するための操作です。

        この操作はBLEフレームワーク内のフラグ制御のためにあるもので、これによるLINBLEとの通信はまだ発生しません。
         */
        var succeeded = gatt.setCharacteristicNotification(dataFromPeripheral, true)
        if (!succeeded) {
            gattOperationCallback.onError(RequestFailureGattOperationException("setCharacteristicNotification"))
            return
        }

        /*
        2. `BluetoothGatt.writeDescriptor()` による、LINBLEへのNotification発行の許可

        LINBLEと通信し、対象キャラクタリスティックからのNotification発行を実際に許可するための操作です。
         */
        bluetoothGattCallbackBridger.register(object : BluetoothGattCallback() {
            override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                super.onDescriptorWrite(gatt, descriptor, status)

                gattOperationTimeoutDetector?.cancel()
                bluetoothGattCallbackBridger.unregister(this)

                bluetoothGattCallbackBridger.register(object : BluetoothGattCallback() {
                    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                        super.onCharacteristicChanged(gatt, characteristic)

                        val value = characteristic?.value ?: return

                        notificationCallback.onNotify(NotificationEvent(value))
                    }
                })

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    gattOperationCallback.onError(ResponseFailureGattOperationException("writeDescriptor"))
                    return
                }

                gattOperationCallback.onSuccess(this@ConcreteLinble)
            }
        })

        dataFromPeripheralCccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

        gattOperationTimeoutDetector = Timer().schedule(gattOperationTimeoutMillis) {
            gattOperationCallback.onError(TimeoutGattOperationException("writeDescriptor"))
        }

        succeeded = gatt.writeDescriptor(dataFromPeripheralCccd)
        if (!succeeded) {
            gattOperationTimeoutDetector?.cancel()

            gattOperationCallback.onError(RequestFailureGattOperationException("writeDescriptor"))
        }
    }

    override fun write(data: ByteArray, gattOperationCallback: Linble.GattOperationCallback) {
        debugLogger.logw(logTag, "write: data=${data.description()}")

        val gatt = this.gatt ?: run {
            return gattOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        val dataToPeripheral = this.dataToPeripheral ?: run {
            return gattOperationCallback.onError(DisconnectedAfterOnlineException())
        }

        bluetoothGattCallbackBridger.register(object : BluetoothGattCallback() {
            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)

                gattOperationTimeoutDetector?.cancel()
                bluetoothGattCallbackBridger.unregister(this)

                gattOperationCallback.onSuccess(this@ConcreteLinble)
            }
        })

        dataToPeripheral.value = data


        gattOperationTimeoutDetector = Timer().schedule(gattOperationTimeoutMillis) {
            gattOperationCallback.onError(TimeoutGattOperationException("writeCharacteristic"))
        }

        val succeeded = gatt.writeCharacteristic(dataToPeripheral)
        if (!succeeded) {
            gattOperationTimeoutDetector?.cancel()

            gattOperationCallback.onError(RequestFailureGattOperationException("writeCharacteristic"))
        }
    }

    override fun cancelConnection() {
        debugLogger.logw(logTag, "cancelConnection:")

        // 接続対象に関するリソースを全て解放します。
        // 切断操作自体は `ConcreteBluetoothCentralController` が行います。

        bluetoothGattCallbackBridger.unregisterAll()

        gattOperationTimeoutDetector?.cancel()

        gatt = null

        dataFromPeripheral = null
        dataFromPeripheralCccd = null
        dataToPeripheral = null
    }

    override fun toString(): String {
        return "<${this.javaClass.simpleName}: gatt=${gatt?.device}>"
    }
}
