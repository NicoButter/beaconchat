package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.data.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Settings screen for configuring BeaconChat preferences.
 *
 * Provides user interface for managing application settings including callsign
 * configuration, validation, and persistence. Uses DataStore for reactive
 * state management and provides feedback for user actions.
 *
 * @param userPreferences User preferences data store for configuration management
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun SettingsScreen(userPreferences: UserPreferences, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    // State from DataStore
    val currentCallsign by userPreferences.callsign.collectAsState(initial = "")
    val callsignEnabled by userPreferences.callsignEnabled.collectAsState(initial = false)

    // Local UI state
    var editingCallsign by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    // Initialize editing state with current callsign
    LaunchedEffect(currentCallsign) {
        if (editingCallsign.isEmpty()) {
            editingCallsign = currentCallsign
        }
    }

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
    ) {
        Text(
                text = "⚙️ Configuración",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Callsign Section
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                        text = "📡 Identificación (Callsign)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                        text =
                                "Tu callsign se antepone a cada mensaje que envías, permitiendo que otros identifiquen al emisor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Callsign input
                OutlinedTextField(
                        value = editingCallsign,
                        onValueChange = {
                            editingCallsign =
                                    it.uppercase().filter { char -> char.isLetterOrDigit() }
                            showError = false
                            showSuccess = false
                        },
                        label = { Text("Callsign") },
                        placeholder = { Text("Ej: NICO, ABC, RESCUE1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showError,
                        supportingText = {
                            if (showError) {
                                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(
                                        "${editingCallsign.length}/${UserPreferences.MAX_CALLSIGN_LENGTH} caracteres (mín: ${UserPreferences.MIN_CALLSIGN_LENGTH})"
                                )
                            }
                        }
                )

                // Save button
                Button(
                        onClick = {
                            if (userPreferences.isValidCallsign(editingCallsign)) {
                                scope.launch {
                                    userPreferences.saveCallsign(editingCallsign)
                                    showSuccess = true
                                    showError = false
                                }
                            } else {
                                showError = true
                                errorMessage =
                                        when {
                                            editingCallsign.length <
                                                    UserPreferences.MIN_CALLSIGN_LENGTH ->
                                                    "Mínimo ${UserPreferences.MIN_CALLSIGN_LENGTH} caracteres"
                                            editingCallsign.length >
                                                    UserPreferences.MAX_CALLSIGN_LENGTH ->
                                                    "Máximo ${UserPreferences.MAX_CALLSIGN_LENGTH} caracteres"
                                            else -> "Solo letras y números"
                                        }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editingCallsign.isNotBlank()
                ) {
                    Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Callsign")
                }

                // Success message
                if (showSuccess) {
                    Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.primaryContainer
                                    )
                    ) {
                        Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = "✅ Callsign guardado correctamente",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Enable/Disable switch
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Incluir callsign en mensajes",
                                style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                                text = if (callsignEnabled) "Activado" else "Desactivado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = callsignEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { userPreferences.setCallsignEnabled(enabled) }
                            },
                            enabled = currentCallsign.isNotBlank()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preview Section
        if (currentCallsign.isNotBlank() && callsignEnabled) {
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
            ) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                                text = "Vista Previa",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Text(
                            text = "Tus mensajes se enviarán así:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                    text = "Mensaje original: SOS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                    text = "Mensaje enviado: $currentCallsign-SOS",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info card
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                        text = "ℹ️ Información",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                        text =
                                "• El callsign debe tener entre ${UserPreferences.MIN_CALLSIGN_LENGTH} y ${UserPreferences.MAX_CALLSIGN_LENGTH} caracteres\n" +
                                        "• Solo se permiten letras y números\n" +
                                        "• Se convierte automáticamente a mayúsculas\n" +
                                        "• Formato de mensaje: [CALLSIGN]-[MENSAJE]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
