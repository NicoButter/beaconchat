package com.nicobutter.beaconchat

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nicobutter.beaconchat.domain.EmergencyMode
import com.nicobutter.beaconchat.domain.EmergencyType
import com.nicobutter.beaconchat.ui.AppScreen
import com.nicobutter.beaconchat.ui.BeaconNavigation
import com.nicobutter.beaconchat.ui.components.QuickHelpActions
import com.nicobutter.beaconchat.ui.screens.EmergencyTransmissionContent
import com.nicobutter.beaconchat.ui.theme.BeaconChatTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Production shell, branding, Home, quick actions and emergency UI; no emitters or radio requests. */
@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val requests = mutableListOf<Pair<EmergencyType, EmergencyMode>>()
    private val featureOwners = mutableListOf<LifecycleOwner>()
    private var cleanupCalls = 0

    private fun render(width: Int = 360, fontScale: Float = 1f, restoration: StateRestorationTester? = null) {
        compose.mainClock.autoAdvance = false
        val content: @Composable () -> Unit = {
            BeaconChatTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.requiredSize(width.dp, 780.dp)) {
                        BeaconNavigation(
                            onLeaveScreen = { cleanupCalls++ },
                            emergencyContent = { type, mode, dismiss ->
                                DisposableEffect(type, mode) { requests.add(type to mode); onDispose { } }
                                EmergencyTransmissionContent(type, mode, 0, dismiss)
                            },
                            featureContent = { route, modifier, emergency, navigate ->
                                val owner = LocalLifecycleOwner.current
                                DisposableEffect(owner) { featureOwners.add(owner); onDispose { } }
                                Column(modifier) {
                                    Text("Módulo ${route.title}")
                                    if (route == AppScreen.TRANSMIT) QuickHelpActions(emergency)
                                    if (route == AppScreen.RECEIVE) Button(onClick = { navigate(AppScreen.VIBRATION_DETECTOR) }) {
                                        Text("Abrir detector de vibración")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun enterHome() {
        compose.mainClock.advanceTimeBy(600)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("home").assertIsDisplayed()
        compose.onNodeWithTag("welcome").assertDoesNotExist()
    }

    private fun capture(name: String, tag: String = "app_shell") {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "ux-checks").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Missing text layout: $text", layouts.isNotEmpty())
        assertFalse("Clipped text: $text", layouts.any { it.hasVisualOverflow })
    }

    @Test fun welcomeContainsOnlyOriginalBrandingAndAutomaticallyOpensHome() {
        render()
        compose.onNodeWithContentDescription("Logo original de BeaconChat").assertIsDisplayed()
        compose.onNodeWithContentDescription("Logo original de Vetrabyte").assertIsDisplayed()
        compose.onNodeWithText("BeaconChat").assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
        capture("welcome-360")
        enterHome()
        compose.runOnIdle { assertTrue(requests.isEmpty()); assertEquals(0, cleanupCalls) }
    }

    @Test fun allFourQuickActionsAndEveryEmergencyExitReturnHomeWithoutBranding() {
        render(); enterHome()
        val cases = listOf("SOS" to (EmergencyType.SOS to EmergencyMode.ALL),
            "HELP" to (EmergencyType.HELP to EmergencyMode.ALL),
            "TRAPPED" to (EmergencyType.TRAPPED to EmergencyMode.ALL),
            "DISCRETO" to (EmergencyType.SOS to EmergencyMode.DISCREET))
        cases.forEachIndexed { index, (label, expected) ->
            compose.onNodeWithText(label).performScrollTo().performClick()
            compose.onNodeWithText("DETENER TRANSMISIÓN").assertIsDisplayed()
            compose.runOnIdle { assertEquals(expected, requests.last()) }
            when (index) {
                1 -> compose.onNodeWithContentDescription("Detener y volver a Inicio").performClick()
                2 -> Espresso.pressBack()
                else -> compose.onNodeWithText("DETENER TRANSMISIÓN").performClick()
            }
            compose.onNodeWithTag("home").assertIsDisplayed()
            compose.onNodeWithTag("welcome").assertDoesNotExist()
        }
        compose.runOnIdle { assertEquals(4, requests.size) }
    }

    @Test fun advancedRoutesRemainReachableAndDestroyTheirLifecycleOnReturnHome() {
        render(); enterHome()
        for (route in listOf(AppScreen.TRANSMIT, AppScreen.RECEIVE, AppScreen.SCAN, AppScreen.MESH,
            AppScreen.LIGHTMAP, AppScreen.OSCILLOSCOPE, AppScreen.SETTINGS)) {
            compose.onNodeWithContentDescription(route.title).performScrollTo().performClick()
            compose.onNodeWithText("Módulo ${route.title}").assertIsDisplayed()
            compose.runOnIdle { assertEquals(Lifecycle.State.RESUMED, featureOwners.last().lifecycle.currentState) }
            val previous = featureOwners.last()
            compose.onNodeWithContentDescription("Volver a Inicio").performClick()
            compose.onNodeWithTag("home").assertIsDisplayed()
            compose.runOnIdle { assertEquals(Lifecycle.State.DESTROYED, previous.lifecycle.currentState) }
        }
        compose.onNodeWithContentDescription("Recibir").performScrollTo().performClick()
        compose.onNodeWithText("Abrir detector de vibración").performClick()
        compose.onNodeWithText("Módulo Detector de vibración").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("Módulo Recibir").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithTag("home").assertIsDisplayed()
        compose.runOnIdle { assertTrue(requests.isEmpty()) }
    }

    @Test fun stateRestorationPreservesRoutesAndNeverReplaysWelcomeOrAnEmergency() {
        val restoration = StateRestorationTester(compose)
        render(restoration = restoration); enterHome()
        compose.onNodeWithContentDescription("Configuración").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Módulo Configuración").assertIsDisplayed()
        compose.onNodeWithTag("welcome").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver a Inicio").performClick()
        compose.onNodeWithText("SOS").performClick()
        compose.onNodeWithText("DETENER TRANSMISIÓN").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("home").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, requests.size) }
    }

    @Test fun pausingAnEmergencyReturnsHomeWithoutRestartingItOnResume() {
        render(); enterHome()
        compose.onNodeWithText("SOS").performClick()
        compose.onNodeWithText("DETENER TRANSMISIÓN").assertIsDisplayed()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithTag("home").assertIsDisplayed()
        compose.onNodeWithTag("welcome").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, requests.size) }
    }

    @Test fun homeFits360DpAndPrimaryRoutesHaveAccessibleActions() {
        render(); enterHome()
        compose.onNodeWithTag("home").assertWidthIsEqualTo(360.dp)
        listOf("SOS", "HELP", "TRAPPED", "DISCRETO").forEach {
            compose.onNodeWithText(it).assertHasClickAction().assertHeightIsAtLeast(48.dp)
            assertTextFits(it)
        }
        capture("home-360")
        for (title in listOf("Emitir señales o mensajes", "Recibir o detectar señales", "Mesh Radar · experimental")) {
            compose.onNodeWithTag("home").performScrollToNode(hasText(title))
            compose.onNodeWithText(title).assertIsDisplayed().assertHasClickAction()
            assertTextFits(title)
        }
        compose.runOnIdle { assertTrue(requests.isEmpty()) }
    }

    @Test fun homeFits390Dp() {
        render(width = 390); enterHome()
        compose.onNodeWithTag("home").assertWidthIsEqualTo(390.dp)
        listOf("SOS", "HELP", "TRAPPED", "DISCRETO").forEach { assertTextFits(it) }
        capture("home-390")
    }

    @Test fun largeTextAt360DpKeepsActionsReadableAndScrollable() {
        render(fontScale = 2f); enterHome()
        capture("home-360-font200")
        for (label in listOf("SOS", "HELP", "TRAPPED", "DISCRETO")) {
            compose.onNodeWithTag("home").performScrollToNode(hasText(label))
            compose.onNodeWithText(label).assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
            assertTextFits(label)
        }
        compose.onNodeWithContentDescription("Configuración").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertTrue(requests.isEmpty()) }
    }

    @Test fun compactEmergencyKeepsStopVisibleWithLargeText() {
        compose.setContent {
            BeaconChatTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    EmergencyTransmissionContent(EmergencyType.TRAPPED, EmergencyMode.ALL, 0, {},
                        Modifier.requiredSize(360.dp, 320.dp).testTag("emergency_compact"))
                }
            }
        }
        compose.onNodeWithText("DETENER TRANSMISIÓN").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithContentDescription("Detener y volver a Inicio").assertIsDisplayed()
        assertTextFits("DETENER TRANSMISIÓN")
        capture("emergency-compact-font200", "emergency_compact")
    }
}
