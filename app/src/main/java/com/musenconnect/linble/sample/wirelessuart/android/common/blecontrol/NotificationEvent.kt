package com.musenconnect.linble.sample.wirelessuart.android.common.blecontrol

data class NotificationEvent(
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        // Android Studioによる自動生成コード
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationEvent

        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        // Android Studioによる自動生成コード
        var result = data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}