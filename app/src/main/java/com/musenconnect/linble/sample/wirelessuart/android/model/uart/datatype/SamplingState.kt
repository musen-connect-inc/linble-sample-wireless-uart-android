package com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype

enum class SamplingState(val value: Byte) {
    Stopped(0),
    Sampling(1);

    fun toByte(): Byte {
        return value
    }

    companion object {
        fun from(value: Byte): SamplingState {
            return values().first { it.value == value }
        }
    }
}