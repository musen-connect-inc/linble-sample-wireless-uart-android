package com.musenconnect.linble.sample.wirelessuart.android.common

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.Advertisement
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.BluetoothCentralController
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.DeviceBluetoothState
import com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol.Linble
import com.musenconnect.linble.sample.wirelessuart.android.description
import com.musenconnect.linble.sample.wirelessuart.android.logTag

class StubBluetoothCentralController : BluetoothCentralController {
    override var currentDeviceBluetoothState = DeviceBluetoothState.Unknown

    override fun startDeviceBluetoothStateMonitoring(callback: BluetoothCentralController.DeviceBluetoothStateMonitoringCallback) {
        currentDeviceBluetoothState = DeviceBluetoothState.PoweredOn

        callback.onChange(currentDeviceBluetoothState)
    }

    override fun stopDeviceBluetoothStateMonitoring() {
    }


    var scanAdvertisementsCalled = false
    var scanAdvertisementsCalledCount = 0

    override fun scanAdvertisements(callback: BluetoothCentralController.ScanAdvertisementsCallback) {
        PrintlnDebugLogger.logd(logTag, "scanAdvertisements:")

        scanAdvertisementsCalled = true
        scanAdvertisementsCalledCount += 1

        callback.onScanned(Advertisement(null, WirelessUartController.defaultTargetLinbleAddress))
    }

    var cancelScanCalled = false

    override fun cancelScan() {
        PrintlnDebugLogger.logd(logTag, "cancelScan:")
        cancelScanCalled = true
    }

    var connectCalled = false
    var discoverServicesAndCharacteristicsCalled = false
    var enableNotificationCalled = false

    var writeCalledCount = 0
    var lastWroteData: ByteArray? = null

    override fun connect(target: Advertisement, callback: Linble.GattOperationCallback) {
        PrintlnDebugLogger.logd(logTag, "connect:")
        connectCalled = true

        _isConnected = true

        callback.onSuccess(StubLinble())
    }

    inner class StubLinble: Linble {
        override var dataFromPeripheral: BluetoothGattCharacteristic? = null
        override var dataFromPeripheralCccd: BluetoothGattDescriptor? = null
        override var dataToPeripheral: BluetoothGattCharacteristic? = null

        override fun discoverServicesAndCharacteristics(gattOperationCallback: Linble.GattOperationCallback) {
            PrintlnDebugLogger.logd(logTag, "discoverServicesAndCharacteristics:")

            discoverServicesAndCharacteristicsCalled = true

            if (scanAdvertisementsCalledCount == 1) {
                gattOperationCallback.onError(Exception("scanAdvertisementsCalledCount == 1"))
                return
            }

            gattOperationCallback.onSuccess(this)
        }

        override fun enableNotification(gattOperationCallback: Linble.GattOperationCallback, notificationCallback: Linble.NotificationCallback) {
            PrintlnDebugLogger.logd(logTag, "enableNotification:")

            enableNotificationCalled = true

            gattOperationCallback.onSuccess(this)
        }

        override fun write(data: ByteArray, gattOperationCallback: Linble.GattOperationCallback) {
            PrintlnDebugLogger.logd(logTag, "write: data=${data.description()}")

            writeCalledCount += 1
            lastWroteData = data

            gattOperationCallback.onSuccess(this)
        }

        override fun cancelConnection() {
        }
    }

    private var _isConnected = false
    override val isConnected: Boolean
        get() = _isConnected

    var cancelConnectionCalled = false

    override fun cancelConnection() {
        PrintlnDebugLogger.logd(logTag, "cancelConnection:")
        cancelConnectionCalled = true

        _isConnected = false
    }
}