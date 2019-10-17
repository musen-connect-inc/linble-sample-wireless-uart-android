package com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol

enum class DeviceBluetoothState {
    Unknown,
    PoweredOff,
    PoweredOnButDisabledLocationService,    // AndroidでBLE APIを使用するためには、位置情報機能もオンになっている必要があります。
    PoweredOn,
}
