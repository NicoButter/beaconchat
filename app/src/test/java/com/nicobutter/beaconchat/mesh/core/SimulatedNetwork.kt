package com.nicobutter.beaconchat.mesh.core

import java.util.ArrayDeque
import java.util.UUID

internal fun node(name: String) = NodeId(UUID.nameUUIDFromBytes("node:$name".toByteArray(Charsets.UTF_8)))
internal fun id(name: String) = MessageId(UUID.nameUUIDFromBytes("message:$name".toByteArray(Charsets.UTF_8)))
internal class FakeClock(var time: Long = 0) : MeshClock { override fun nowMillis() = time }

/** Deterministic in-memory links. Radio metadata deliberately never enters MeshEngine. */
internal class SimulatedNetwork {
    data class Device(val engine: MeshEngine, val callsign: String, var mac: String)
    data class Frame(val from: NodeId, val to: NodeId, val message: MeshMessage, val sourceMac: String)
    data class Attempt(val from: NodeId, val message: MeshMessage, val outcome: DispatchStatus)
    val clock = FakeClock()
    val devices = linkedMapOf<NodeId, Device>()
    val links = linkedMapOf<NodeId, MutableSet<NodeId>>()
    val deliveries = linkedMapOf<NodeId, MutableList<MeshMessage>>()
    val frames = mutableListOf<Frame>()
    val attempts = mutableListOf<Attempt>()
    private val inFlight = ArrayDeque<Frame>()

    fun add(name: String, callsign: String = name, mac: String = "radio-$name"): MeshEngine {
        val engine = MeshEngine(node(name), clock)
        devices[engine.nodeId] = Device(engine, callsign, mac)
        links[engine.nodeId] = linkedSetOf()
        deliveries[engine.nodeId] = mutableListOf()
        return engine
    }

    fun connect(a: String, b: String) {
        links.getValue(node(a)).add(node(b))
        links.getValue(node(b)).add(node(a))
    }

    fun publish(from: String, to: String?, text: String, hops: Int, messageId: MessageId = id(text)): MeshDecision {
        val result = devices.getValue(node(from)).engine.originate(to?.let(::node), text, hops, messageId)
        record(node(from), result)
        return result
    }

    private fun record(at: NodeId, result: MeshDecision) {
        if (result.deliverLocally) deliveries.getValue(at).add(result.message)
    }

    fun runUntilIdle(maxCycles: Int = 100, maxFrames: Int = 10_000) {
        repeat(maxCycles) {
            for ((sender, device) in devices) {
                val results = device.engine.dispatch(MeshTransport { message, excluded ->
                    val targets = links.getValue(sender).filter { it !in excluded }
                    targets.forEach { recipient ->
                        val frame = Frame(sender, recipient, message, device.mac)
                        frames.add(frame)
                        if (frames.size > maxFrames) throw AssertionError("Unbounded forwarding")
                        inFlight.addLast(frame)
                    }
                    if (targets.isEmpty()) Handoff.UNAVAILABLE else Handoff.ACCEPTED
                })
                results.forEach { attempts.add(Attempt(sender, it.message, it.status)) }
            }
            while (inFlight.isNotEmpty()) {
                val frame = inFlight.removeFirst()
                record(frame.to, devices.getValue(frame.to).engine.receive(frame.message, frame.from))
            }
            if (devices.values.all { it.engine.snapshot().pendingMessages == 0 }) return
        }
        error("Network did not become idle")
    }
}
