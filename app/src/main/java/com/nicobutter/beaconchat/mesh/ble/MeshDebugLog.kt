package com.nicobutter.beaconchat.mesh.ble

import android.util.Log
import com.nicobutter.beaconchat.BuildConfig

internal object MeshDebugLog : MeshDiagnostics {
    override fun record(event: MeshDiagnostic) {
        if (BuildConfig.DEBUG) Log.d("BeaconMesh", event.toString())
    }
}
