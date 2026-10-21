package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.transport.MeshLinkSession
import java.util.UUID

/** Tracks actual server connections, including links awaiting physical disconnection after expiry. */
internal class MeshIncomingSessions(private val maxConnections: Int = 8) {
    private class Link(var session: MeshLinkSession? = null, var closing: Boolean = false)
    private val links = linkedMapOf<String, Link>()

    fun connected(address: String): Boolean {
        if (address in links) return true // Duplicate callbacks cannot revive a retired link.
        if (links.size >= maxConnections) return false
        links[address] = Link()
        return true
    }

    fun get(address: String): MeshLinkSession? = links[address]?.takeUnless { it.closing }?.session

    fun getOrOpen(address: String, open: (MeshLinkSession) -> Boolean): MeshLinkSession? {
        val link = links[address]?.takeUnless { it.closing } ?: return null
        link.session?.let { return it }
        val session = MeshLinkSession(UUID.randomUUID())
        if (!open(session)) return null
        link.session = session
        return session
    }

    fun retire(session: MeshLinkSession): String? {
        val entry = links.entries.firstOrNull { it.value.session == session } ?: return null
        entry.value.session = null
        entry.value.closing = true
        return entry.key
    }

    fun disconnected(address: String): MeshLinkSession? = links.remove(address)?.session
    fun clear(): List<MeshLinkSession> = links.values.mapNotNull { it.session }.also { links.clear() }
}
