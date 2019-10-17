package com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import java.util.*

/** @see com.musenconnect.linble.sample.wirelessuart.android.concrete.ConcreteLinble */
interface Linble {
    object GattUuid {
        val linbleUartService = UUID.fromString("27ADC9CA-35EB-465A-9154-B8FF9076F3E8")!!
        val dataFromPeripheral = UUID.fromString("27ADC9CB-35EB-465A-9154-B8FF9076F3E8")!!
        val dataToPeripheral = UUID.fromString("27ADC9CC-35EB-465A-9154-B8FF9076F3E8")!!
    }

    // ..GATT操作..
    interface GattOperationCallback {
        fun onError(reason: Throwable)

        fun onSuccess(linble: Linble)
    }
    fun discoverServicesAndCharacteristics(gattOperationCallback: GattOperationCallback)   // AndroidはService検索とCharacteristics検索が一体になっています

    var dataFromPeripheral: BluetoothGattCharacteristic?
    var dataFromPeripheralCccd: BluetoothGattDescriptor?

    var dataToPeripheral: BluetoothGattCharacteristic?

    interface NotificationCallback {
        fun onNotify(notificationEvent: NotificationEvent)
    }
    fun enableNotification(gattOperationCallback: GattOperationCallback, notificationCallback: NotificationCallback)

    fun write(data: ByteArray, gattOperationCallback: GattOperationCallback)

    fun cancelConnection()
}

object BluetoothLowEnergySpec {
    object GattUuid {
        /*
        `cccd` とは、 BLEの仕様で定められた `Client Characteristic Configuration Descriptor` の略称です。
        Notification可能なキャラクタリスティックから実際にNotification発行を許可するかどうかを制御します。

        このUUIDは全てのBLE製品で共通ですが、Android BLEフレームワークにはこの定義が存在しないため、アプリ側コードで用意する必要があります。
         */
        val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")!!
    }
}
