package com.musenconnect.linble.sample.wirelessuart.android.model.uart.datatype

data class AsciiString(
    val value: String
) {
    constructor(byteArray: ByteArray) : this(String(byteArray, charset))

    companion object {
        private val charset = Charsets.US_ASCII
    }

    init {
        // 「一旦ASCIIエンコードして再ASCIIデコードした文字列」が
        // 「元の文字列」と同一になるかどうかで判定
        require(this.toByteArray().toString(charset) == value)
    }

    fun toByteArray(): ByteArray {
        return value.toByteArray(charset)
    }
}
