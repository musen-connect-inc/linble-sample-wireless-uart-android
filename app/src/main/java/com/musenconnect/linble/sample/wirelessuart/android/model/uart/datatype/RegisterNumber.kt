package com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype

data class RegisterNumber(
    val value: Byte
) {
    init {
        require(this.value in 0..7)
    }
}