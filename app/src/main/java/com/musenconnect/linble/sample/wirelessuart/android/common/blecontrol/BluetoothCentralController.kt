package com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol


/** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteBluetoothCentralController */
interface BluetoothCentralController {
    // ..Bluetooth状態の監視..
    val currentDeviceBluetoothState: DeviceBluetoothState

    interface DeviceBluetoothStateMonitoringCallback {
        fun onChange(deviceBluetoothState: DeviceBluetoothState)
    }

    fun startDeviceBluetoothStateMonitoring(callback: DeviceBluetoothStateMonitoringCallback)

    fun stopDeviceBluetoothStateMonitoring()

    // ..スキャン..
    interface ScanAdvertisementsCallback {
        fun onScanned(advertisement: Advertisement)
    }

    fun scanAdvertisements(callback: ScanAdvertisementsCallback)

    fun cancelScan()

    // ..接続..
    val isConnected: Boolean

    fun connect(target: Advertisement, callback: Linble.GattOperationCallback)

    fun cancelConnection()
}