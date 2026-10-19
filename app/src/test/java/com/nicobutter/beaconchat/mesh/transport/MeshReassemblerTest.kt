package com.nicobutter.beaconchat.mesh.transport

import com.nicobutter.beaconchat.mesh.core.FakeClock
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class MeshReassemblerTest {
    private val clock = FakeClock()
    private val receiver = MeshReassembler(clock)
    private fun frames(text: String = "123456789", id: Int = 1) = MeshFragmentProtocol.fragment(text.toByteArray(), id)
    private fun rejected(reason: FragmentRejection, result: ReassemblyResult) {
        assertEquals(ReassemblyResult.Rejected(reason), result)
    }
    private fun complete(receiver: MeshReassembler, bytes: ByteArray, id: Int, link: MeshLinkSession = session()): ByteArray {
        val frames = MeshFragmentProtocol.fragment(bytes, id)
        frames.dropLast(1).forEach { assertEquals(ReassemblyResult.Pending, receiver.accept(link, it)) }
        return (receiver.accept(link, frames.last()) as ReassemblyResult.Complete).payload
    }

    @Test fun singleFrameCompletesAndReleasesBuffer() {
        assertArrayEquals(byteArrayOf(42), complete(receiver, byteArrayOf(42), 0))
        assertEquals(ReassemblySnapshot(0, 1, 0), receiver.snapshot())
    }

    @Test fun maximumUnicodeEnvelopeSurvivesSplitsInsideUtf8Codepoints() {
        val original = message("a" + "🆘".repeat(255) + "sí") // 1024 bytes, including a split emoji.
        val encoded = MeshMessageCodec.encode(original)
        assertEquals(1077, encoded.size)
        assertEquals(original, MeshMessageCodec.decode(complete(receiver, encoded, 65535)))
        assertEquals(0, receiver.snapshot().bufferedBytes)
    }

    @Test fun reverseOrderIncludingLastFrameFirstCompletesExactlyOnce() {
        val payload = MeshMessageCodec.encode(message("🆘".repeat(80)))
        val frames = MeshFragmentProtocol.fragment(payload, 42).reversed()
        frames.dropLast(1).forEach { assertEquals(ReassemblyResult.Pending, receiver.accept(session(), it)) }
        assertArrayEquals(payload, (receiver.accept(session(), frames.last()) as ReassemblyResult.Complete).payload)
        frames.forEach { rejected(FragmentRejection.CLOSED_TRANSFER, receiver.accept(session(), it)) }
    }

    @Test fun deterministicShufflesPreserveAllBytes() {
        val payload = MeshMessageCodec.encode(message("sí 🆘 ".repeat(80)))
        repeat(20) { seed ->
            val shuffled = MeshFragmentProtocol.fragment(payload, seed).shuffled(kotlin.random.Random(seed))
            val results = shuffled.map { receiver.accept(session(), it) }
            assertEquals(1, results.count { it is ReassemblyResult.Complete })
            assertArrayEquals(payload, (results.last() as ReassemblyResult.Complete).payload)
        }
    }

    @Test fun identicalDuplicatesAreIdempotentAndDoNotCompleteMissingData() {
        val frames = frames("a".repeat(40))
        frames.dropLast(1).forEach {
            assertEquals(ReassemblyResult.Pending, receiver.accept(session(), it))
            assertEquals(ReassemblyResult.Duplicate, receiver.accept(session(), it.copyOf()))
        }
        assertArrayEquals("a".repeat(40).toByteArray(),
            (receiver.accept(session(), frames.last()) as ReassemblyResult.Complete).payload)
    }

    @Test fun conflictingDuplicateAbortsAndPreventsResurrection() {
        val frames = frames()
        receiver.accept(session(), frames.first())
        val conflict = frames.first().copyOf().also { it[12] = (it[12].toInt() xor 1).toByte() }
        rejected(FragmentRejection.CONFLICTING_DUPLICATE, receiver.accept(session(), conflict))
        assertEquals(ReassemblySnapshot(0, 1, 0), receiver.snapshot())
        frames.forEach { rejected(FragmentRejection.CLOSED_TRANSFER, receiver.accept(session(), it)) }
    }

    @Test fun mixedPayloadsUnderSameTransferIdAbortOnMetadataMismatch() {
        val a = frames("123456789")
        val b = frames("abcdefghi")
        receiver.accept(session(), a.first())
        rejected(FragmentRejection.INCONSISTENT_TRANSFER, receiver.accept(session(), b.last()))
        rejected(FragmentRejection.CLOSED_TRANSFER, receiver.accept(session(), a.last()))
        assertEquals(0, receiver.snapshot().bufferedBytes)
    }

    @Test fun inconsistentTotalLengthAbortsEvenWhenFragmentCountIsUnchanged() {
        val a = frames("123456789")
        val b = frames("1234567890123456")
        receiver.accept(session(), a.first())
        rejected(FragmentRejection.INCONSISTENT_TRANSFER, receiver.accept(session(), b.first()))
    }

    @Test fun sameTransferIdOnDifferentSessionsDoesNotMix() {
        val a = frames("123456789")
        val b = frames("abcdefghi")
        receiver.accept(session(1), a.first())
        receiver.accept(session(2), b.first())
        assertArrayEquals("abcdefghi".toByteArray(),
            (receiver.accept(session(2), b.last()) as ReassemblyResult.Complete).payload)
        assertArrayEquals("123456789".toByteArray(),
            (receiver.accept(session(1), a.last()) as ReassemblyResult.Complete).payload)
    }

    @Test fun simultaneousTransfersOnSameSessionCanInterleave() {
        val a = frames("123456789", 1)
        val b = frames("abcdefghi", 2)
        receiver.accept(session(), a.last())
        receiver.accept(session(), b.first())
        assertEquals(ReassemblySnapshot(2, 2, 18), receiver.snapshot())
        assertArrayEquals("123456789".toByteArray(),
            (receiver.accept(session(), a.first()) as ReassemblyResult.Complete).payload)
        assertArrayEquals("abcdefghi".toByteArray(),
            (receiver.accept(session(), b.last()) as ReassemblyResult.Complete).payload)
    }

    @Test fun globalConcurrencyRejectsNewTransferWithoutEvictingLiveOnes() {
        val limited = MeshReassembler(clock, ReassemblyLimits(maxConcurrentTransfers = 2))
        limited.accept(session(1), frames(id = 1).first())
        limited.accept(session(2), frames(id = 2).first())
        rejected(FragmentRejection.CAPACITY, limited.accept(session(3), frames(id = 3).first()))
        assertEquals(ReassemblySnapshot(2, 2, 18), limited.snapshot())
        assertTrue(limited.accept(session(1), frames(id = 1).last()) is ReassemblyResult.Complete)
        assertEquals(ReassemblyResult.Pending, limited.accept(session(3), frames(id = 3).first()))
    }

    @Test fun perSessionQuotaAllowsAnotherSessionWithoutEviction() {
        receiver.accept(session(1), frames(id = 1).first())
        receiver.accept(session(1), frames(id = 2).first())
        rejected(FragmentRejection.CAPACITY, receiver.accept(session(1), frames(id = 3).first()))
        assertEquals(ReassemblyResult.Pending, receiver.accept(session(2), frames(id = 3).first()))
    }

    @Test fun byteBudgetReservesWholeTransferAndIsReusableAfterCompletion() {
        val limited = MeshReassembler(clock, ReassemblyLimits(maxBufferedBytes = 18))
        limited.accept(session(), frames(id = 1).first())
        limited.accept(session(), frames(id = 2).first())
        assertEquals(18, limited.snapshot().bufferedBytes)
        rejected(FragmentRejection.CAPACITY, limited.accept(session(2), frames(id = 3).first()))
        limited.accept(session(), frames(id = 1).last())
        assertEquals(ReassemblyResult.Pending, limited.accept(session(2), frames(id = 3).first()))
        assertEquals(18, limited.snapshot().bufferedBytes)
    }

    @Test fun rejectsTransferTooLargeForConfiguredBufferBeforeAllocating() {
        val limited = MeshReassembler(clock, ReassemblyLimits(maxBufferedBytes = 8))
        rejected(FragmentRejection.CAPACITY, limited.accept(session(), frames().first()))
        assertEquals(ReassemblySnapshot(0, 0, 0), limited.snapshot())
    }

    @Test fun configuredSizeAndFragmentLimitsRejectBeforeAllocating() {
        val sizeLimited = MeshReassembler(clock, ReassemblyLimits(maxTransferBytes = 8))
        val countLimited = MeshReassembler(clock, ReassemblyLimits(maxFragments = 1))
        listOf(sizeLimited, countLimited).forEach {
            rejected(FragmentRejection.LIMIT_EXCEEDED, it.accept(session(), frames().first()))
            assertEquals(ReassemblySnapshot(0, 0, 0), it.snapshot())
        }
    }

    @Test fun completedTombstonesAreBoundedAndExpireAtOriginalDeadline() {
        val limited = MeshReassembler(clock, ReassemblyLimits(maxConcurrentTransfers = 1,
            maxTransfersPerSession = 1, maxTrackedTransfers = 2))
        complete(limited, byteArrayOf(1), 1)
        complete(limited, byteArrayOf(2), 2)
        rejected(FragmentRejection.CAPACITY, limited.accept(session(2), frames(id = 3).first()))
        assertEquals(ReassemblySnapshot(0, 2, 0), limited.snapshot())
        clock.time = 15_000
        assertEquals(2, limited.cleanup())
        assertEquals(ReassemblySnapshot(0, 0, 0), limited.snapshot())
        assertArrayEquals(byteArrayOf(3), complete(limited, byteArrayOf(3), 1))
    }

    @Test fun missingFragmentNeverCompletesAndExpiresExactlyWithoutSlidingOnDuplicates() {
        val frames = frames()
        assertEquals(ReassemblyResult.Pending, receiver.accept(session(), frames.first()))
        clock.time = 14_999
        assertEquals(ReassemblyResult.Duplicate, receiver.accept(session(), frames.first()))
        assertEquals(1, receiver.snapshot().activeTransfers)
        clock.time = 15_000
        assertEquals(1, receiver.cleanup())
        assertEquals(ReassemblySnapshot(0, 0, 0), receiver.snapshot())
        assertEquals(ReassemblyResult.Pending, receiver.accept(session(), frames.last()))
    }

    @Test fun everyNewOperationCleansExpiredBuffers() {
        receiver.accept(session(), frames().first())
        clock.time = 15_000
        receiver.accept(session(2), frames(id = 2).first())
        assertEquals(ReassemblySnapshot(1, 1, 9), receiver.snapshot())
        clock.time = 30_000
        assertEquals(ReassemblySnapshot(0, 0, 0), receiver.snapshot())
    }

    @Test fun completingLateDoesNotExtendTombstoneRetention() {
        val frames = frames()
        receiver.accept(session(), frames.first())
        clock.time = 14_000
        assertTrue(receiver.accept(session(), frames.last()) is ReassemblyResult.Complete)
        clock.time = 15_000
        assertEquals(1, receiver.cleanup())
    }

    @Test fun corruptPayloadFailsWholeTransferIntegrityAndFreesMemory() {
        val frames = frames("a".repeat(20))
        frames.first()[12] = 98
        frames.dropLast(1).forEach { receiver.accept(session(), it) }
        rejected(FragmentRejection.INTEGRITY_FAILED, receiver.accept(session(), frames.last()))
        assertEquals(ReassemblySnapshot(0, 1, 0), receiver.snapshot())
        rejected(FragmentRejection.CLOSED_TRANSFER, receiver.accept(session(), frames.first()))
    }

    @Test fun consistentlyCorruptChecksumIsRejectedOnCompletion() {
        val frames = frames()
        frames.forEach { it[8] = (it[8].toInt() xor 1).toByte() }
        receiver.accept(session(), frames.first())
        rejected(FragmentRejection.INTEGRITY_FAILED, receiver.accept(session(), frames.last()))
    }

    @Test fun callerCannotMutateStoredFragmentsAfterAdmission() {
        val frames = frames()
        receiver.accept(session(), frames.first())
        frames.first().fill(0)
        assertArrayEquals("123456789".toByteArray(),
            (receiver.accept(session(), frames.last()) as ReassemblyResult.Complete).payload)
    }

    @Test fun invalidFramesAreRejectedWithoutAllocationAndUnknownVersionCannotStartTransfer() {
        val frame = frames().first()
        val invalid = frame.indices.map { frame.copyOf(it) } + listOf(frame + 0,
            frame.copyOf().also { it[1] = 2 }, frame.copyOf().also { it[5] = 0 }, ByteArray(100_000))
        invalid.forEach { rejected(FragmentRejection.MALFORMED_FRAME, receiver.accept(session(), it)) }
        assertEquals(ReassemblySnapshot(0, 0, 0), receiver.snapshot())
    }

    @Test fun rejectedMalformedFragmentDoesNotEraseAValidActiveTransfer() {
        val frames = frames()
        receiver.accept(session(), frames.first())
        rejected(FragmentRejection.MALFORMED_FRAME, receiver.accept(session(), frames.last().dropLast(1).toByteArray()))
        assertTrue(receiver.accept(session(), frames.last()) is ReassemblyResult.Complete)
    }

    @Test fun discardingSessionReleasesOnlyItsBuffersAndTerminalEntries() {
        receiver.accept(session(1), frames(id = 1).first())
        complete(receiver, byteArrayOf(1), 2, session(1))
        receiver.accept(session(2), frames(id = 1).first())
        receiver.discardSession(session(1))
        assertEquals(ReassemblySnapshot(1, 1, 9), receiver.snapshot())
        assertTrue(receiver.accept(session(2), frames(id = 1).last()) is ReassemblyResult.Complete)
    }

    @Test fun reconnectWithNewSessionDoesNotCompleteUsingOldSessionBytes() {
        val frames = frames()
        receiver.accept(session(1), frames.first())
        receiver.discardSession(session(1))
        assertEquals(ReassemblyResult.Pending, receiver.accept(session(2), frames.last()))
        assertTrue(receiver.accept(session(2), frames.first()) is ReassemblyResult.Complete)
    }

    @Test fun monotonicClockAndSaturatedDeadlines() {
        clock.time = Long.MAX_VALUE - 10
        receiver.accept(session(), frames().first())
        clock.time = Long.MAX_VALUE - 1
        assertEquals(1, receiver.snapshot().activeTransfers)
        clock.time = Long.MAX_VALUE
        assertEquals(1, receiver.cleanup())
        clock.time--
        try { receiver.cleanup(); fail("Backward clock accepted") } catch (_: IllegalStateException) { }
    }

    @Test fun invalidLimitConfigurationsFailEarly() {
        listOf<() -> ReassemblyLimits>(
            { ReassemblyLimits(maxConcurrentTransfers = 0) }, { ReassemblyLimits(maxTransfersPerSession = 0) },
            { ReassemblyLimits(maxTransfersPerSession = 9) }, { ReassemblyLimits(maxTrackedTransfers = 7) },
            { ReassemblyLimits(maxBufferedBytes = 0) }, { ReassemblyLimits(maxTransferBytes = 0) },
            { ReassemblyLimits(maxTransferBytes = 1078) }, { ReassemblyLimits(maxFragments = 0) },
            { ReassemblyLimits(maxFragments = 136) }, { ReassemblyLimits(retentionMillis = 0) }
        ).forEach { rejects { it() } }
    }

    @Test fun randomBytesAndMutatedFramesNeverCrashOrEscapeResourceLimits() {
        val random = Random(20261018L)
        val valid = frames("a".repeat(120))
        repeat(10_000) {
            val frame = if (it % 2 == 0) ByteArray(random.nextInt(100)).also(random::nextBytes)
                else valid[random.nextInt(valid.size)].copyOf().also { bytes ->
                    val position = random.nextInt(bytes.size)
                    bytes[position] = random.nextInt(256).toByte()
                }
            receiver.accept(session((it % 5).toLong()), frame)
            val snapshot = receiver.snapshot()
            assertTrue(snapshot.activeTransfers <= 8)
            assertTrue(snapshot.trackedTransfers <= 32)
            assertTrue(snapshot.bufferedBytes in 0..8616)
        }
    }
}
