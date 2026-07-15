package com.jamlink.nativelib.network.timesync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeSyncTest {

    @Test
    fun testPacketSerialization() {
        val original = TimeSyncPacket(
            type = TimeSyncPacket.TYPE_RESPONSE,
            t1 = 1000L,
            t2 = 1010L,
            t3 = 1020L,
            sequence = 42L
        )

        val bytes = original.toByteArray()
        val deserialized = TimeSyncPacket.fromByteArray(bytes)

        assertEquals(original.type, deserialized.type)
        assertEquals(original.t1, deserialized.t1)
        assertEquals(original.t2, deserialized.t2)
        assertEquals(original.t3, deserialized.t3)
        assertEquals(original.sequence, deserialized.sequence)
    }

    @Test
    fun testRttCalculation() {
        val t1 = 1000L // Client sends
        val t2 = 1010L // Server receives
        val t3 = 1020L // Server sends
        val t4 = 1040L // Client receives

        val rtt = (t4 - t1) - (t3 - t2)
        assertEquals(30L, rtt)
    }

    @Test
    fun testOffsetCalculation() {
        val t1 = 1000L // Client sends
        val t2 = 1010L // Server receives
        val t3 = 1020L // Server sends
        val t4 = 1040L // Client receives

        val offset = ((t2 - t1) + (t3 - t4)) / 2
        assertEquals(-5L, offset)
    }

    @Test
    fun testOutlierRejection() {
        data class SyncResult(val rtt: Long, val offset: Long)
        
        val results = mutableListOf(
            SyncResult(10L, 5L),
            SyncResult(12L, 6L),
            SyncResult(11L, 4L),
            SyncResult(10L, 5L),
            SyncResult(100L, 50L) // Outlier
        )
        
        results.sortBy { it.rtt }
        val medianRtt = results[results.size / 2].rtt
        val threshold = medianRtt * 3
        
        val validResults = results.filter { it.rtt <= threshold }
        
        assertEquals(4, validResults.size)
        assertTrue(validResults.none { it.rtt == 100L })
    }

    @Test
    fun testMinimumRttSelection() {
        data class SyncResult(val rtt: Long, val offset: Long)
        
        val validResults = listOf(
            SyncResult(12L, 6L),
            SyncResult(9L, 4L), // Minimum RTT
            SyncResult(11L, 5L)
        )
        
        val bestSample = validResults.minByOrNull { it.rtt }
        
        assertEquals(9L, bestSample?.rtt)
        assertEquals(4L, bestSample?.offset)
    }

    @Test
    fun testEmaSmoothing() {
        val ALPHA = 0.3
        var smoothedOffsetNs = 0L
        var isFirstSample = true
        
        // 1st sample (bootstrap)
        val sample1Offset = 10L
        if (isFirstSample) {
            smoothedOffsetNs = sample1Offset
            isFirstSample = false
        }
        assertEquals(10L, smoothedOffsetNs)
        
        // 2nd sample
        val sample2Offset = 20L
        smoothedOffsetNs = (ALPHA * sample2Offset + (1 - ALPHA) * smoothedOffsetNs).toLong()
        
        // Expected: 0.3 * 20 + 0.7 * 10 = 6 + 7 = 13
        assertEquals(13L, smoothedOffsetNs)
    }
}
