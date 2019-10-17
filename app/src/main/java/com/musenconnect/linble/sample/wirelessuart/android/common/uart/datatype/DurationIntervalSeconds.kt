package com.musenconnect.linble.sample.wirelessuart.android.common.uart.datatype

data class DurationIntervalSeconds(
    val value: Byte
) {
    init {
        require(this.value in 1..60)
    }
}