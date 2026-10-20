package com.nicobutter.beaconchat.sos.gateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import com.nicobutter.beaconchat.mesh.core.MeshClock
import com.nicobutter.beaconchat.mesh.core.MeshMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Main-looper host; mesh's existing foreground ticker drives it. No background service or storage. */
class AndroidSosInternetGateway(context: Context) {
    private val connectivity = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val gateway = SosInternetGateway(MeshClock { SystemClock.elapsedRealtime() })
    private val transport = HttpsLabHttpTransport()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var inFlight: Job? = null
    private var cancellation: Job? = null
    private var generation = 0L
    private val _state = MutableStateFlow(GatewaySnapshot())
    val state = _state.asStateFlow()

    fun enable(endpoint: String) {
        val parsed = LabEndpoint.parse(endpoint)
        gateway.enable(parsed)
        transport.setEnabled(true)
        publish()
    }

    fun disable() {
        generation++
        gateway.disable()
        revokeTransport()
        // Keep the physical worker counted until it exits: no unbounded IO jobs after timeout/toggle.
        publish()
    }

    fun onAcceptedMesh(message: MeshMessage) {
        gateway.onAcceptedMesh(message)
        publish()
    }

    fun tick() {
        val snapshot = gateway.snapshot() // Also applies queue expiry and the logical attempt deadline.
        if (snapshot.enabled && inFlight?.isActive == true && snapshot.incidents.none { it.state == GatewayState.HTTP_STARTED }) {
            revokeTransport() // Abort a logically timed-out write/read if the platform permits it.
        }
        if (inFlight?.isActive == true) { publish(); return }
        val attempt = gateway.beginDelivery(validatedInternet())
        if (attempt != null) {
            transport.setEnabled(true)
            val token = generation
            val client = LabApiClient(transport.authorized())
            inFlight = scope.launch {
                val result = withContext(Dispatchers.IO) { client.deliver(attempt.request) }
                if (token == generation) gateway.complete(attempt, result)
                publish()
            }
        }
        publish()
    }

    private fun revokeTransport() {
        transport.setEnabled(false)
        if (cancellation?.isActive == true) return
        val disconnect = transport.cancellation()
        cancellation = scope.launch(Dispatchers.IO) {
            try { disconnect() } catch (_: Exception) { /* No sensitive exception text in state/logs. */ }
        }
    }

    private fun validatedInternet(): Boolean {
        if (!gateway.enabled) return false
        return try {
            val active = connectivity.activeNetwork ?: return false
            val caps = connectivity.getNetworkCapabilities(active) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        } catch (_: SecurityException) { false }
    }
    private fun publish() { _state.value = gateway.snapshot() }
}
