package com.jamlink.nativelib.network.timesync

import java.nio.ByteBuffer

data class TimeSyncPacket(
    val type: Byte,
    val t1: Long,
    val t2: Long,
    val t3: Long,
    val sequence: Long
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(PACKET_SIZE)
        buffer.put(type)
        buffer.putLong(t1)
        buffer.putLong(t2)
        buffer.putLong(t3)
        buffer.putLong(sequence)
        return buffer.array()
    }

    companion object {
        const val PACKET_SIZE = 33
        const val TYPE_REQUEST: Byte = 0x01
        const val TYPE_RESPONSE: Byte = 0x02

        fun fromByteArray(data: ByteArray): TimeSyncPacket {
            val buffer = ByteBuffer.wrap(data)
            return TimeSyncPacket(
                type = buffer.get(),
                t1 = buffer.long,
                t2 = buffer.long,
                t3 = buffer.long,
                sequence = buffer.long
            )
        }
    }
}
