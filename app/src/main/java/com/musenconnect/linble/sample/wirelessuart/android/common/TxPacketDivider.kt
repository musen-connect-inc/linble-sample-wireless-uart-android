package com.musenconnect.linble.sample.wirelessuart.android.common


fun ByteArray.chunked(expectedSize: Int): List<ByteArray> {
    var target = this.toList()

    val fragmentedDataList = mutableListOf<ByteArray>()

    while (target.isNotEmpty()) {
        val currentChunkSize = if (target.size >= expectedSize) expectedSize else target.size

        val fragmentedData = target.take(currentChunkSize).toByteArray()
        fragmentedDataList.add(fragmentedData)

        target = target.drop(currentChunkSize)
    }

    return fragmentedDataList.toList()
}


class TxPacketDivider(data: ByteArray, size: Int) {
    private val fragmentedByteArrayList = data.chunked(size).toMutableList()

    val remain: List<ByteArray>
        get() {
            return fragmentedByteArrayList.toList()
        }

    val next: ByteArray?
        get() {
            if (fragmentedByteArrayList.isEmpty()) {
                return null
            }

            return fragmentedByteArrayList.removeAt(0)
        }
}
