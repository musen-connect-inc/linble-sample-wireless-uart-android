package com.musenconnect.linble.sample.wirelessuart.android.model.blecontrol

import java.util.UUID

interface Linble {
    object GattUuid {
        val linbleUartService = UUID.fromString("27ADC9CA-35EB-465A-9154-B8FF9076F3E8")!!
        val dataFromPeripheral = UUID.fromString("27ADC9CB-35EB-465A-9154-B8FF9076F3E8")!!
        val dataToPeripheral = UUID.fromString("27ADC9CC-35EB-465A-9154-B8FF9076F3E8")!!
    }
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