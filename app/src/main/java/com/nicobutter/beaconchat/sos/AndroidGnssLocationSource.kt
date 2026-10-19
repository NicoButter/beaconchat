package com.nicobutter.beaconchat.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** GPS_PROVIDER only: no fused/network provider, geocoder, Internet, background permission or service. */
@Suppress("DEPRECATION", "MissingPermission")
class AndroidGnssLocationSource(context: Context, private val lifecycle: Lifecycle) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val acquisition = GnssAcquisition()
    private val _state = MutableStateFlow(acquisition.snapshot(now()))
    val state = _state.asStateFlow()
    private var listener: LocationListener? = null
    private var timeout: Runnable? = null
    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) cancel()
        if (event == Lifecycle.Event.ON_DESTROY) close()
    }
    init { lifecycle.addObserver(observer) }

    fun request(timeoutMillis: Long = 30_000) {
        require(timeoutMillis in 1..120_000)
        stopUpdates()
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            acquisition.error("GPS solo disponible mientras el laboratorio está en primer plano"); refresh(); return
        }
        val permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        try {
            val cached = if (permitted) manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(::sample) else null
            val enabled = permitted && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val token = acquisition.start(now(), permitted, enabled, cached, timeoutMillis)
            refresh()
            if (token == null) return
            val callback = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val value = sample(location) ?: return
                    if (acquisition.location(token, value, now()) || !acquisition.searching) stopUpdates()
                    refresh()
                }
                override fun onProviderDisabled(provider: String) {
                    if (listener !== this || provider != LocationManager.GPS_PROVIDER) return
                    acquisition.unavailable(now(), "GPS apagado durante la captura; sin fix actual")
                    stopUpdates(); refresh()
                }
                override fun onProviderEnabled(provider: String) { }
                @Deprecated("Android API 24–28 callback")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
            }
            listener = callback
            timeout = Runnable { if (acquisition.timeout(token, now())) { stopUpdates(); refresh() } }
                .also { handler.postDelayed(it, timeoutMillis) }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            stopUpdates(); acquisition.error("Permiso de ubicación revocado o insuficiente"); refresh()
        } catch (_: Exception) {
            stopUpdates(); acquisition.error("No se pudo iniciar GPS_PROVIDER"); refresh()
        }
    }

    /** Refreshes monotonic age; CURRENT automatically becomes LAST_KNOWN/STALE as time passes. */
    fun refresh() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            _state.value.position != null) {
            stopUpdates(); acquisition.error("Permiso revocado; posición descartada")
        }
        _state.value = acquisition.snapshot(now())
    }
    fun positionForSos(): SosPosition? { refresh(); return _state.value.position }
    fun cancel() { stopUpdates(); acquisition.cancel(); refresh() }
    fun close() { cancel(); lifecycle.removeObserver(observer) }
    private fun stopUpdates() {
        val previous = listener
        listener = null
        timeout?.let(handler::removeCallbacks)
        timeout = null
        try { previous?.let(manager::removeUpdates) } catch (_: Exception) { }
    }
    private fun now() = SystemClock.elapsedRealtime()
    private fun sample(location: Location): GpsSample? = try {
        if (location.provider != LocationManager.GPS_PROVIDER || location.elapsedRealtimeNanos <= 0) null
        else GpsSample(location.latitude, location.longitude, if (location.hasAccuracy()) location.accuracy.toDouble() else null,
            location.elapsedRealtimeNanos / 1_000_000, location.isFromMockProvider)
    } catch (_: IllegalArgumentException) { null }
}
