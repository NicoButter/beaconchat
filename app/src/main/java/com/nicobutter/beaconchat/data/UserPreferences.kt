package com.nicobutter.beaconchat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property para crear una instancia de DataStore.
 * Utiliza el nombre "user_preferences" para el almacenamiento persistente.
 */
private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "user_preferences")

/**
 * Gestiona las preferencias del usuario utilizando DataStore.
 *
 * Esta clase maneja el almacenamiento y recuperación de configuraciones del usuario,
 * principalmente el callsign (identificador único del usuario) y su estado de habilitación.
 * Utiliza Kotlin Flow para proporcionar actualizaciones reactivas de las preferencias.
 *
 * @property context El contexto de Android utilizado para acceder al DataStore.
 *
 * @sample
 * ```
 * val userPreferences = UserPreferences(context)
 * 
 * // Observar cambios en el callsign
 * userPreferences.callsign.collect { callsign ->
 *     println("Callsign actual: $callsign")
 * }
 * 
 * // Guardar un nuevo callsign
 * userPreferences.saveCallsign("W1XYZ")
 * ```
 */
class UserPreferences(private val context: Context) {

    companion object {
        /** Clave para almacenar el callsign del usuario en DataStore */
        private val CALLSIGN_KEY = stringPreferencesKey("callsign")
        
        /** Clave para almacenar el estado de habilitación del callsign */
        private val CALLSIGN_ENABLED_KEY = booleanPreferencesKey("callsign_enabled")

        /** Longitud mínima permitida para un callsign válido */
        const val MIN_CALLSIGN_LENGTH = 3
        
        /** Longitud máxima permitida para un callsign válido */
        const val MAX_CALLSIGN_LENGTH = 8
    }

    /**
     * Flow que emite el callsign actual del usuario.
     *
     * Este Flow se actualiza automáticamente cuando el callsign cambia en el DataStore.
     * Emite una cadena vacía si no se ha establecido ningún callsign.
     *
     * @return Flow<String> que emite el callsign actual o una cadena vacía.
     */
    val callsign: Flow<String> =
            context.dataStore.data.map { preferences -> preferences[CALLSIGN_KEY] ?: "" }

    /**
     * Flow que emite el estado de habilitación del callsign.
     *
     * Este Flow se actualiza automáticamente cuando el estado cambia en el DataStore.
     * Emite `false` si no se ha establecido ningún valor.
     *
     * @return Flow<Boolean> que emite `true` si el callsign está habilitado, `false` en caso contrario.
     */
    val callsignEnabled: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[CALLSIGN_ENABLED_KEY] ?: false }

    /**
     * Guarda un nuevo callsign para el usuario.
     *
     * El callsign se convierte automáticamente a mayúsculas y se elimina el espacio en blanco.
     * Esta operación es asíncrona y debe ser llamada desde una corrutina.
     *
     * @param callsign El nuevo callsign a guardar. Será normalizado a mayúsculas y sin espacios.
     *
     * @sample
     * ```
     * scope.launch {
     *     userPreferences.saveCallsign("w1xyz")
     *     // Se guardará como "W1XYZ"
     * }
     * ```
     */
    suspend fun saveCallsign(callsign: String) {
        context.dataStore.edit { preferences ->
            preferences[CALLSIGN_KEY] = callsign.uppercase().trim()
        }
    }

    /**
     * Habilita o deshabilita el uso del callsign en los mensajes.
     *
     * Cuando está habilitado, el callsign se agregará como prefijo a los mensajes transmitidos.
     * Esta operación es asíncrona y debe ser llamada desde una corrutina.
     *
     * @param enabled `true` para habilitar el callsign, `false` para deshabilitarlo.
     */
    suspend fun setCallsignEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[CALLSIGN_ENABLED_KEY] = enabled }
    }

    /**
     * Valida si un callsign cumple con los requisitos establecidos.
     *
     * Un callsign válido debe:
     * - Tener entre [MIN_CALLSIGN_LENGTH] y [MAX_CALLSIGN_LENGTH] caracteres
     * - Contener solo letras y números (sin espacios ni caracteres especiales)
     *
     * @param callsign El callsign a validar.
     * @return `true` si el callsign es válido, `false` en caso contrario.
     *
     * @sample
     * ```
     * val isValid = userPreferences.isValidCallsign("W1XYZ")  // true
     * val isInvalid = userPreferences.isValidCallsign("A")    // false (muy corto)
     * val hasSpace = userPreferences.isValidCallsign("W1 XY") // false (contiene espacio)
     * ```
     */
    fun isValidCallsign(callsign: String): Boolean {
        val trimmed = callsign.trim()
        return trimmed.length in MIN_CALLSIGN_LENGTH..MAX_CALLSIGN_LENGTH &&
                trimmed.all { it.isLetterOrDigit() }
    }

    /**
     * Formatea un mensaje agregando el callsign como prefijo si está habilitado.
     *
     * Si el callsign está habilitado y no está vacío, se agrega al inicio del mensaje
     * en el formato: "CALLSIGN-mensaje". De lo contrario, devuelve el mensaje sin modificar.
     *
     * @param message El mensaje original a formatear.
     * @param currentCallsign El callsign actual del usuario.
     * @param isEnabled Indica si el callsign está habilitado.
     * @return El mensaje formateado con el callsign (si está habilitado) o el mensaje original.
     *
     * @sample
     * ```
     * val formatted = userPreferences.formatMessageWithCallsign(
     *     message = "Hello",
     *     currentCallsign = "W1XYZ",
     *     isEnabled = true
     * )
     * // Resultado: "W1XYZ-Hello"
     * ```
     */
    suspend fun formatMessageWithCallsign(
            message: String,
            currentCallsign: String,
            isEnabled: Boolean
    ): String {
        return if (isEnabled && currentCallsign.isNotBlank()) {
            "${currentCallsign.uppercase()}-$message"
        } else {
            message
        }
    }
}
