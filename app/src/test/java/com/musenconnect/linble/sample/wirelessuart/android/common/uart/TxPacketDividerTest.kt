package com.musenconnect.linble.sample.wirelessuart.android.common.uart

import com.musenconnect.linble.sample.wirelessuart.android.asHexStringToByteArray
import com.musenconnect.linble.sample.wirelessuart.android.common.TxPacketDivider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TxPacketDividerTest {
    @Test
    fun chunked() {
        TxPacketDivider("010203".asHexStringToByteArray(), size = 3).let {
            assertArrayEquals("010203".asHexStringToByteArray(), it.next)
            assertEquals(0, it.remain.size)
        }

        TxPacketDivider("01020304".asHexStringToByteArray(), size = 3).let {
            assertArrayEquals("010203".asHexStringToByteArray(), it.next)
            assertEquals(1, it.remain.size)
            assertArrayEquals("04".asHexStringToByteArray(), it.next)
            assertEquals(0, it.remain.size)
        }

        TxPacketDivider("01020304050607080910".asHexStringToByteArray(), size = 3).let {
            assertArrayEquals("010203".asHexStringToByteArray(), it.next)
            assertEquals(3, it.remain.size)
            assertArrayEquals("040506".asHexStringToByteArray(), it.next)
            assertEquals(2, it.remain.size)
            assertArrayEquals("070809".asHexStringToByteArray(), it.next)
            assertEquals(1, it.remain.size)
            assertArrayEquals("10".asHexStringToByteArray(), it.next)
            assertEquals(0, it.remain.size)
        }
    }
}