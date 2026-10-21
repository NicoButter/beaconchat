package com.nicobutter.beaconchat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.nicobutter.beaconchat.domain.EmergencyMode
import com.nicobutter.beaconchat.domain.EmergencyType
import com.nicobutter.beaconchat.ui.screens.HomeScreen
import com.nicobutter.beaconchat.ui.screens.WelcomeScreen
import kotlinx.coroutines.delay

enum class AppScreen(val title: String) {
    WELCOME("Bienvenida"), HOME("Inicio"), EMERGENCY("Señal local"),
    TRANSMIT("Transmitir"), RECEIVE("Recibir"), SCAN("Escáner"), MESH("Mesh Radar"),
    LIGHTMAP("Radar óptico"), OSCILLOSCOPE("Osciloscopio"), SETTINGS("Configuración"),
    VIBRATION_DETECTOR("Detector de vibración")
}

typealias EmergencyRequest = (EmergencyType, EmergencyMode) -> Unit
internal const val WELCOME_MILLIS = 450L

// Restoring an Activity must not replay institutional branding or restart an emission.
private val screenSaver = Saver<AppScreen, String>(
    save = { if (it == AppScreen.WELCOME || it == AppScreen.EMERGENCY) AppScreen.HOME.name else it.name },
    restore = { value -> AppScreen.values().firstOrNull { it.name == value } ?: AppScreen.HOME }
)

/** Stable UX shell; technical modules supply content without owning the app's starting route. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconNavigation(
    onLeaveScreen: () -> Unit,
    emergencyContent: @Composable (EmergencyType, EmergencyMode, () -> Unit) -> Unit,
    featureContent: @Composable (AppScreen, Modifier, EmergencyRequest, (AppScreen) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var screen by rememberSaveable(stateSaver = screenSaver) { mutableStateOf(AppScreen.WELCOME) }
    var emergencyType by remember { mutableStateOf(EmergencyType.SOS) }
    var emergencyMode by remember { mutableStateOf(EmergencyMode.ALL) }

    fun navigate(next: AppScreen) {
        if (next == screen || next == AppScreen.WELCOME || next == AppScreen.EMERGENCY) return
        if (screen != AppScreen.WELCOME) onLeaveScreen()
        screen = next
    }
    val goHome = { navigate(AppScreen.HOME) }
    val requestEmergency: EmergencyRequest = { type, mode ->
        onLeaveScreen()
        emergencyType = type
        emergencyMode = mode
        screen = AppScreen.EMERGENCY
    }
    BackHandler(enabled = screen != AppScreen.HOME) {
        navigate(if (screen == AppScreen.VIBRATION_DETECTOR) AppScreen.RECEIVE else AppScreen.HOME)
    }

    // Emissions end on pause; returning to the app must not restart them or display stale success.
    val parent = LocalLifecycleOwner.current
    val onPause by rememberUpdatedState { if (screen == AppScreen.EMERGENCY) goHome() }
    DisposableEffect(parent) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) onPause() }
        parent.lifecycle.addObserver(observer)
        onDispose { parent.lifecycle.removeObserver(observer) }
    }

    Box(modifier.fillMaxSize().testTag("app_shell")) {
        when (screen) {
            AppScreen.WELCOME -> {
                LaunchedEffect(Unit) { delay(WELCOME_MILLIS); navigate(AppScreen.HOME) }
                WelcomeScreen()
            }
            AppScreen.EMERGENCY -> ScreenLifecycle(screen) {
                emergencyContent(emergencyType, emergencyMode, goHome)
            }
            else -> Scaffold(
                topBar = {
                    TopAppBar(title = { Text(screen.title) }, navigationIcon = {
                        if (screen != AppScreen.HOME) IconButton(onClick = goHome) {
                            Icon(Icons.Default.Home, contentDescription = "Volver a Inicio")
                        }
                    })
                },
                bottomBar = { AdvancedNavigation(screen, ::navigate) }
            ) { padding ->
                ScreenLifecycle(screen) {
                    if (screen == AppScreen.HOME) HomeScreen(
                        { navigate(AppScreen.TRANSMIT) }, { navigate(AppScreen.RECEIVE) },
                        { navigate(AppScreen.MESH) }, requestEmergency, Modifier.padding(padding)
                    ) else featureContent(screen, Modifier.padding(padding), requestEmergency, ::navigate)
                }
            }
        }
    }
}

/** Same seven modules and order as before; each tab retains a readable label and touch target. */
@Composable
private fun AdvancedNavigation(screen: AppScreen, navigate: (AppScreen) -> Unit) {
    val items = listOf(
        Triple(AppScreen.TRANSMIT, "🔦", "Emitir"), Triple(AppScreen.RECEIVE, "📷", "Recibir"),
        Triple(AppScreen.SCAN, "🔍", "Escáner"), Triple(AppScreen.MESH, "📡", "Mesh"),
        Triple(AppScreen.LIGHTMAP, "🎯", "Radar"), Triple(AppScreen.OSCILLOSCOPE, "📊", "Oscilo."),
        Triple(AppScreen.SETTINGS, "⚙️", "Ajustes")
    )
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        Column {
            Text("Herramientas · deslizá para ver más", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            BoxWithConstraints(Modifier.testTag("advanced_navigation")) {
                val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
                val barWidth = maxOf(maxWidth, 80.dp * fontScale * items.size)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    NavigationBar(Modifier.width(barWidth).heightIn(min = 80.dp * minOf(fontScale, 1.5f))) {
                        items.forEach { (destination, emoji, label) ->
                            NavigationBarItem(
                                selected = screen == destination, onClick = { navigate(destination) },
                                icon = { Text(emoji, fontSize = 22.sp, modifier = Modifier.clearAndSetSemantics { }) },
                                label = { Text(label) },
                                modifier = Modifier.semantics { contentDescription = destination.title }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A camera bound to this owner is destroyed on route exit, even while the Activity stays resumed. */
@Composable
private fun ScreenLifecycle(screen: AppScreen, content: @Composable () -> Unit) {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent, screen) { RouteLifecycleOwner() }
    DisposableEffect(owner, parent) {
        val observer = LifecycleEventObserver { _, _ -> owner.registry.currentState = parent.lifecycle.currentState }
        parent.lifecycle.addObserver(observer)
        owner.registry.currentState = parent.lifecycle.currentState
        onDispose {
            parent.lifecycle.removeObserver(observer)
            owner.registry.currentState = Lifecycle.State.DESTROYED
        }
    }
    CompositionLocalProvider(LocalLifecycleOwner provides owner, content = content)
}

private class RouteLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.CREATED }
    override val lifecycle: Lifecycle get() = registry
}
