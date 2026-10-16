package com.nicobutter.beaconchat.mesh.core

import org.junit.Assert.*
import org.junit.Test

class MeshEngineTest {
    private val clock = FakeClock()
    private val limits = MeshLimits(maxSeenMessages = 4, maxPendingMessages = 2,
        seenRetentionMillis = 100, pendingRetentionMillis = 20, maxHops = 4, maxContentBytes = 32)
    private val engine = MeshEngine(node("B"), clock, limits)
    private fun message(name: String = "one", destination: NodeId? = node("C"), hops: Int = 2) =
        MeshMessage(id(name), node("A"), destination, name, hops)
    private fun receive(packet: MeshMessage) = engine.receive(packet, node("A"))
    private val acceptedTransport = MeshTransport { _, _ -> Handoff.ACCEPTED }

    @Test fun localDestinationDeliversAtZeroTtlWithoutForwarding() {
        val result = receive(message(destination = node("B"), hops = 0))
        assertEquals(Acceptance.ACCEPTED, result.acceptance)
        assertTrue(result.deliverLocally)
        assertEquals(Forwarding.LOCAL_DESTINATION, result.forwarding)
        assertEquals(0, engine.snapshot().pendingMessages)
    }

    @Test fun originatingForSelfDeliversOnceWithoutAnyLink() {
        val result = engine.originate(node("B"), "Solo yo", 4, id("self"))
        assertTrue(result.deliverLocally)
        assertEquals(Forwarding.LOCAL_DESTINATION, result.forwarding)
        assertEquals(Acceptance.DUPLICATE, engine.originate(node("B"), "Solo yo", 4, id("self")).acceptance)
        assertTrue(engine.dispatch(acceptedTransport).isEmpty())
    }

    @Test fun ownOriginEchoIsIgnoredWithoutPoisoningLocalIds() {
        val echo = message().copy(originId = node("B"))
        assertEquals(Acceptance.OWN_ORIGIN, receive(echo).acceptance)
        assertEquals(MeshSnapshot(0, 0), engine.snapshot())
        assertEquals(Acceptance.ACCEPTED,
            engine.originate(node("C"), echo.content, 2, echo.messageId).acceptance)
        assertEquals(Acceptance.OWN_ORIGIN, receive(echo).acceptance)
    }

    @Test fun duplicatesDoNotDeliverOrQueueAgainAndDoNotRefreshRetention() {
        val packet = message(destination = null)
        assertTrue(receive(packet).deliverLocally)
        clock.time = 90
        val duplicate = receive(packet)
        assertEquals(Acceptance.DUPLICATE, duplicate.acceptance)
        assertFalse(duplicate.deliverLocally)
        assertEquals(Forwarding.NOT_APPLICABLE, duplicate.forwarding)
        assertEquals(MeshSnapshot(1, 0), engine.snapshot()) // Pending expired at t=20.
        clock.time = 100
        assertEquals(Acceptance.ACCEPTED, receive(packet).acceptance)
    }

    @Test fun keyIncludesOriginSoIdenticalMessageIdsFromDifferentNodesAreDistinct() {
        val packet = message(destination = node("B"))
        assertTrue(receive(packet).deliverLocally)
        assertTrue(receive(packet.copy(originId = node("D"))).deliverLocally)
        assertEquals(2, engine.snapshot().seenMessages)
    }

    @Test fun unsupportedVersionAndInvalidMessagesConsumeNoCapacity() {
        assertEquals(Acceptance.UNSUPPORTED_VERSION, receive(message().copy(version = 99)).acceptance)
        val bad = listOf(message(hops = -1), message(hops = 5), message().copy(content = " "),
            message().copy(content = "x".repeat(33)), message().copy(content = "🆘".repeat(9)),
            message().copy(content = "\uD800"))
        bad.forEach { assertEquals(Acceptance.INVALID_MESSAGE, receive(it).acceptance) }
        assertEquals(MeshSnapshot(0, 0), engine.snapshot())
        assertEquals(Acceptance.ACCEPTED, receive(message()).acceptance)
    }

    @Test fun utf8PayloadLimitIsIndependentFromTheExistingTwentyByteBleFrame() {
        val result = receive(message(destination = node("B")).copy(content = "🆘".repeat(8)))
        assertTrue(result.deliverLocally)
        assertEquals(32, result.message.content.toByteArray(Charsets.UTF_8).size)
    }

    @Test fun fullSeenCacheRejectsNewestWithoutEvictingLiveKeys() {
        repeat(4) { assertTrue(receive(message("m$it", node("B"))).deliverLocally) }
        val refused = receive(message("fifth", node("B")))
        assertEquals(Acceptance.SEEN_CAPACITY, refused.acceptance)
        assertFalse(refused.deliverLocally)
        assertEquals(Acceptance.DUPLICATE, receive(message("m0", node("B"))).acceptance)
        assertEquals(MeshSnapshot(4, 0), engine.snapshot())
        clock.time = 100
        assertEquals(CleanupResult(4, 0), engine.cleanup())
        assertTrue(receive(message("fifth", node("B"))).deliverLocally)
    }

    @Test fun queueIsFifoAndDropsNewestWhileStillDeduplicatingIt() {
        assertEquals(Forwarding.QUEUED, receive(message("first")).forwarding)
        assertEquals(Forwarding.QUEUED, receive(message("second")).forwarding)
        assertEquals(Forwarding.QUEUE_FULL, receive(message("third")).forwarding)
        val batch = engine.dispatch(acceptedTransport)
        assertEquals(listOf("first", "second"), batch.map { it.message.content })
        assertEquals(listOf(1, 1), batch.map { it.message.hopsRemaining })
        assertEquals(Acceptance.DUPLICATE, receive(message("third")).acceptance)
        assertEquals(0, engine.snapshot().pendingMessages)
    }

    @Test fun broadcastLocalDeliverySurvivesFullForwardingQueue() {
        receive(message("one")); receive(message("two"))
        val third = receive(message("three", null))
        assertTrue(third.deliverLocally)
        assertEquals(Forwarding.QUEUE_FULL, third.forwarding)
    }

    @Test fun dispatchBatchLimitPreservesPendingOrder() {
        receive(message("one")); receive(message("two"))
        assertEquals("one", engine.dispatch(acceptedTransport, 1).single().message.content)
        assertEquals(1, engine.snapshot().pendingMessages)
        assertEquals("two", engine.dispatch(acceptedTransport, 1).single().message.content)
    }

    @Test fun expirationAtExactBoundaryNeverHandsOffStaleItems() {
        receive(message("one"))
        clock.time = 19
        assertEquals(MeshSnapshot(1, 1), engine.snapshot())
        clock.time = 20
        assertEquals(CleanupResult(0, 1), engine.cleanup())
        assertTrue(engine.dispatch(MeshTransport { _, _ -> fail("Expired item sent"); Handoff.ACCEPTED }).isEmpty())
        assertEquals(Acceptance.DUPLICATE, receive(message("one")).acceptance)
    }

    @Test fun timeAdvancingDuringDispatchExpiresRemainingItems() {
        receive(message("one")); receive(message("two"))
        val results = engine.dispatch(MeshTransport { _, _ -> clock.time = 20; Handoff.ACCEPTED })
        assertEquals(listOf("one"), results.map { it.message.content })
        assertEquals(0, engine.snapshot().pendingMessages)
    }

    @Test fun transportExceptionAndUnavailableDoNotRetryOrInventDelivery() {
        receive(message("one")); receive(message("two"))
        var calls = 0
        val results = engine.dispatch(MeshTransport { _, _ ->
            calls++
            if (calls == 1) throw IllegalStateException("Fake transport failed")
            Handoff.UNAVAILABLE
        })
        assertEquals(listOf(DispatchStatus.TRANSPORT_ERROR, DispatchStatus.UNAVAILABLE), results.map { it.status })
        assertEquals(2, calls)
        assertTrue(engine.dispatch(acceptedTransport).isEmpty())
        assertEquals(Acceptance.DUPLICATE, receive(message("one")).acceptance)
        assertEquals(Acceptance.ACCEPTED, receive(message("new")).acceptance)
    }

    @Test fun forwardingExcludesSelfAndPreviousHopButPreservesEnvelope() {
        val original = message()
        receive(original)
        engine.dispatch(MeshTransport { packet, excluded ->
            assertEquals(original.copy(hopsRemaining = 1), packet)
            assertEquals(setOf(node("A"), node("B")), excluded)
            Handoff.ACCEPTED
        })
        engine.originate(node("C"), "from-B", 1, id("local"))
        engine.dispatch(MeshTransport { _, excluded ->
            assertEquals(setOf(node("B")), excluded)
            Handoff.ACCEPTED
        })
    }

    @Test fun sustainedInputKeepsBothStructuresBoundedAndCleanupReleasesThem() {
        repeat(1000) { receive(message("m$it", null)) }
        assertEquals(MeshSnapshot(4, 2), engine.snapshot())
        clock.time = 100
        assertEquals(CleanupResult(4, 2), engine.cleanup())
        assertEquals(MeshSnapshot(0, 0), engine.snapshot())
    }

    @Test fun injectedMessageIdFactoryIsUsedOnlyForNewUnspecifiedIds() {
        var sequence = 0
        val source = MeshEngine(node("S"), clock, limits) { id("generated-${sequence++}") }
        assertEquals(id("generated-0"), source.originate(node("B"), "one").message.messageId)
        assertEquals(id("explicit"), source.originate(node("B"), "two", messageId = id("explicit")).message.messageId)
        assertEquals(id("generated-1"), source.originate(node("B"), "three").message.messageId)
    }

    @Test fun lowTtlFirstArrivalWinsOverLaterDuplicateWithMoreHops() {
        val original = message(hops = 0)
        assertEquals(Forwarding.TTL_EXHAUSTED, receive(original).forwarding)
        assertEquals(Acceptance.DUPLICATE, receive(original.copy(hopsRemaining = 4)).acceptance)
        assertEquals(0, engine.snapshot().pendingMessages)
    }

    @Test fun ttlStillTerminatesWhenEveryRevisitOutlivesTheSeenCache() {
        val relays = listOf("B", "C", "D").map { MeshEngine(node(it), clock, limits) }
        var packet = message(destination = null, hops = 4)
        var previous = node("A")
        val forwardedHops = mutableListOf<Int>()
        var finished = false
        for (step in 0..4) {
            val relay = relays[step % relays.size]
            val result = relay.receive(packet, previous)
            assertEquals(Acceptance.ACCEPTED, result.acceptance)
            if (packet.hopsRemaining == 0) {
                assertEquals(Forwarding.TTL_EXHAUSTED, result.forwarding)
                assertTrue(relay.dispatch(acceptedTransport).isEmpty())
                finished = true
                break
            }
            val outgoing = relay.dispatch(acceptedTransport).single().message
            forwardedHops.add(outgoing.hopsRemaining)
            previous = relay.nodeId
            packet = outgoing
            clock.time += 101
        }
        assertTrue(finished)
        assertEquals(listOf(3, 2, 1, 0), forwardedHops)
    }

    @Test(expected = IllegalStateException::class)
    fun clockRegressionIsExplicitRatherThanExtendingRetentionSilently() {
        clock.time = 10; engine.snapshot()
        clock.time = 9; engine.snapshot()
    }

    @Test(expected = IllegalArgumentException::class)
    fun pendingLifetimeCannotOutliveDeduplication() {
        MeshLimits(seenRetentionMillis = 10, pendingRetentionMillis = 11)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDispatchBudgetIsRejected() { engine.dispatch(acceptedTransport, 0) }
}
