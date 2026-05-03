package com.nicobutter.beaconchat.domain

/**
 * Global observable state of the emergency system.
 *
 * Exposed as a [kotlinx.coroutines.flow.StateFlow] by [com.nicobutter.beaconchat.controller.EmergencyManager].
 * Both UI and other components can observe this without knowing about individual emitters.
 */
data class EmergencyState(
    val isActive: Boolean = false,
    val type: EmergencyType = EmergencyType.SOS,
    val mode: EmergencyMode = EmergencyMode.ALL
)
