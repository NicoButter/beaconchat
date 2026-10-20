package com.nicobutter.beaconchat.sos.gateway

import com.nicobutter.beaconchat.mesh.core.MeshClock
import com.nicobutter.beaconchat.mesh.core.MeshMessage
import com.nicobutter.beaconchat.sos.*
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.UUID

data class GatewayLimits(val maxQueued: Int = 16, val maxTracked: Int = 64, val maxEvents: Int = 32,
    val queueLifetimeMillis: Long = 300_000, val dedupLifetimeMillis: Long = 600_000,
    val minSendIntervalMillis: Long = 5000, val attemptTimeoutMillis: Long = 20_000,
    val maxAttempts: Int = 3) {
    init {
        require(maxTracked in 1..256 && maxQueued in 1..maxTracked && maxEvents in 1..128)
        require(queueLifetimeMillis in 1..dedupLifetimeMillis && dedupLifetimeMillis <= 3_600_000)
        require(minSendIntervalMillis in 1..60_000 && attemptTimeoutMillis in 1..60_000 && maxAttempts in 1..5)
    }
}

enum class GatewayState { RECEIVED, QUEUED, HTTP_STARTED, LAB_CONFIRMED, ERROR, REJECTED, TIMEOUT, UNKNOWN, EXPIRED, CANCELLED }
enum class GatewayAdmission { QUEUED, DISABLED, NOT_SOS, PRIVACY_BLOCKED, INVALID, DUPLICATE, CONFLICT, QUEUE_FULL, MEMORY_FULL }
data class GatewayEvent(val incidentId: UUID, val state: GatewayState)
data class GatewayIncidentStatus(val incidentId: UUID, val state: GatewayState, val attempts: Int,
    val pending: Boolean, val receipt: String?)
data class GatewaySnapshot(val enabled: Boolean = false, val connected: Boolean = false,
    val queued: Int = 0, val tracked: Int = 0, val incidents: List<GatewayIncidentStatus> = emptyList(),
    val events: List<GatewayEvent> = emptyList(), val lastAdmission: GatewayAdmission? = null, val endpointUrl: String? = null)
data class GatewayAttempt(val token: Long, val request: LabHttpRequest)

/**
 * Volatile, bounded incident queue. Caller serializes every operation; no Android or network I/O.
 * Observe ONLY accepted inbound MeshEngine decisions, including transit unicast. Never read UI history.
 * Dedupe retains a SHA-256 of the whole canonical SOS, including its note; only the allowlist DTO is kept.
 * Limits use local monotonic time, never an untrusted sender timestamp. Live entries are never evicted.
 */
class SosInternetGateway(private val clock: MeshClock, val limits: GatewayLimits = GatewayLimits()) {
    private class Entry(val payload: LabIncidentPayload, val fingerprint: List<Byte>, val receivedAt: Long,
        var state: GatewayState = GatewayState.RECEIVED, var attempts: Int = 0,
        var pending: Boolean = true, var nextAttemptAt: Long = receivedAt, var receipt: String? = null)
    private val entries = linkedMapOf<UUID, Entry>()
    private val events = ArrayDeque<GatewayEvent>()
    private var endpoint: LabEndpoint? = null
    private var connected = false
    private var active: GatewayAttempt? = null
    private var activeDeadline = 0L
    private var nextToken = 0L
    private var nextSendAt = 0L
    private var lastTime = Long.MIN_VALUE
    private var lastAdmission: GatewayAdmission? = null
    val enabled: Boolean get() = endpoint != null

    fun enable(endpoint: LabEndpoint) {
        check(!enabled || this.endpoint?.url == endpoint.url) { "Desactivá antes de cambiar el endpoint" }
        cleanup(now()); this.endpoint = endpoint
    }

    /** An in-flight cancellation cannot undo a possible API save, so it becomes UNKNOWN. */
    fun disable() {
        cleanup(now())
        entries.forEach { (id, entry) ->
            if (entry.pending) {
                transition(id, entry, if (entry.state == GatewayState.HTTP_STARTED) GatewayState.UNKNOWN else GatewayState.CANCELLED)
                entry.pending = false
            }
        }
        active = null; endpoint = null; connected = false
    }

    fun onAcceptedMesh(message: MeshMessage): GatewayAdmission {
        if (!enabled) return admission(GatewayAdmission.DISABLED)
        if (!message.content.startsWith(SosCodec.PREFIX)) return GatewayAdmission.NOT_SOS
        val incident = try { SosCodec.decode(message.content) } catch (_: IllegalArgumentException) {
            return admission(GatewayAdmission.INVALID)
        }
        return receive(incident)
    }

    fun receive(incident: SosIncident): GatewayAdmission {
        val now = now(); cleanup(now)
        if (!enabled) return admission(GatewayAdmission.DISABLED)
        if (!SosMeshPolicy.permitted(incident)) return admission(GatewayAdmission.PRIVACY_BLOCKED)
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(strictUtf8(SosCodec.encode(incident))).toList()
        entries[incident.incidentId]?.let {
            return admission(if (it.fingerprint == fingerprint) GatewayAdmission.DUPLICATE else GatewayAdmission.CONFLICT)
        }
        if (entries.size >= limits.maxTracked) return admission(GatewayAdmission.MEMORY_FULL)
        // Full-queue rejections keep a tombstone so a retry cannot create repeated admissions.
        val entry = Entry(LabIncidentPayload.from(incident), fingerprint, now)
        entries[incident.incidentId] = entry
        event(incident.incidentId, GatewayState.RECEIVED)
        if (entries.values.count { it.pending } > limits.maxQueued) {
            entry.pending = false; transition(incident.incidentId, entry, GatewayState.REJECTED)
            return admission(GatewayAdmission.QUEUE_FULL)
        }
        transition(incident.incidentId, entry, GatewayState.QUEUED)
        return admission(GatewayAdmission.QUEUED)
    }

    /** One active request; rate and retry budgets apply across incidents, failures and toggles. */
    fun beginDelivery(hasValidatedInternet: Boolean): GatewayAttempt? {
        val now = now(); cleanup(now)
        connected = enabled && hasValidatedInternet
        val target = endpoint ?: return null
        if (!connected || active != null || now < nextSendAt) return null
        val (id, entry) = entries.entries.firstOrNull { it.value.pending && now >= it.value.nextAttemptAt } ?: return null
        entry.attempts++
        transition(id, entry, GatewayState.HTTP_STARTED)
        val attempt = GatewayAttempt(++nextToken, LabHttpRequest(target, entry.payload))
        active = attempt; activeDeadline = deadline(now, limits.attemptTimeoutMillis)
        nextSendAt = deadline(now, limits.minSendIntervalMillis)
        return attempt
    }

    /** Late/stale results cannot resurrect expired, cancelled, disabled or newer attempts. */
    fun complete(attempt: GatewayAttempt, result: LabDeliveryResult): Boolean {
        val now = now(); cleanup(now)
        if (!enabled || active != attempt) return false
        val id = attempt.request.payload.incidentId
        val entry = entries[id] ?: return false
        active = null
        when (result) {
            is LabDeliveryResult.Confirmed -> {
                // Defense in depth even for an injected client.
                if (result.receipt.incidentId != id || !result.receipt.receipt.matches(Regex("lab_[A-Za-z0-9_-]{1,96}"))) {
                    failed(id, entry, GatewayState.UNKNOWN, now)
                } else {
                    entry.receipt = result.receipt.receipt; entry.pending = false
                    transition(id, entry, GatewayState.LAB_CONFIRMED)
                }
            }
            is LabDeliveryResult.Rejected -> { entry.pending = false; transition(id, entry, GatewayState.REJECTED) }
            LabDeliveryResult.Error -> failed(id, entry, GatewayState.ERROR, now)
            LabDeliveryResult.Timeout -> failed(id, entry, GatewayState.TIMEOUT, now)
            LabDeliveryResult.Unknown -> failed(id, entry, GatewayState.UNKNOWN, now)
        }
        return true
    }

    fun snapshot(): GatewaySnapshot {
        cleanup(now())
        return GatewaySnapshot(enabled, connected, entries.values.count { it.pending }, entries.size,
            entries.map { (id, entry) -> GatewayIncidentStatus(id, entry.state, entry.attempts, entry.pending, entry.receipt) },
            events.toList(), lastAdmission, endpoint?.url)
    }

    private fun failed(id: UUID, entry: Entry, state: GatewayState, now: Long) {
        transition(id, entry, state)
        entry.pending = entry.attempts < limits.maxAttempts
        entry.nextAttemptAt = deadline(now, limits.minSendIntervalMillis * (1L shl (entry.attempts - 1)))
    }
    private fun cleanup(now: Long) {
        active?.let { attempt ->
            if (now >= activeDeadline) {
                active = null
                entries[attempt.request.payload.incidentId]?.let { failed(it.payload.incidentId, it, GatewayState.TIMEOUT, now) }
            }
        }
        entries.forEach { (id, entry) ->
            if (entry.pending && now - entry.receivedAt >= limits.queueLifetimeMillis) {
                if (active?.request?.payload?.incidentId == id) active = null
                entry.pending = false
                transition(id, entry, if (entry.attempts == 0) GatewayState.EXPIRED else GatewayState.UNKNOWN)
            }
        }
        entries.entries.removeAll { now - it.value.receivedAt >= limits.dedupLifetimeMillis }
    }
    private fun transition(id: UUID, entry: Entry, state: GatewayState) { entry.state = state; event(id, state) }
    private fun event(id: UUID, state: GatewayState) {
        if (events.size == limits.maxEvents) events.removeFirst()
        events.addLast(GatewayEvent(id, state))
    }
    private fun admission(value: GatewayAdmission): GatewayAdmission { lastAdmission = value; return value }
    private fun now(): Long {
        val now = clock.nowMillis(); check(now >= lastTime) { "Gateway clock must be monotonic" }; lastTime = now; return now
    }
    private fun deadline(now: Long, duration: Long) = if (now > Long.MAX_VALUE - duration) Long.MAX_VALUE else now + duration
}
