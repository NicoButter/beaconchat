package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.R
import com.nicobutter.beaconchat.ui.theme.BeaconChatTheme

/** Institutional identity only. The navigation host owns the short, one-time transition to Home. */
@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().padding(24.dp).testTag("welcome")
    ) {
        val logoHeight = minOf(280.dp, maxHeight * 0.5f)
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painterResource(R.drawable.logo), contentDescription = "Logo original de BeaconChat",
                modifier = Modifier.height(logoHeight).width(logoHeight * (768f / 1408f)),
                contentScale = ContentScale.Fit
            )
            Text("BeaconChat", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Image(
                painterResource(R.drawable.vetrabyte_logo), contentDescription = "Logo original de Vetrabyte",
                modifier = Modifier.width(210.dp).aspectRatio(790f / 316f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Preview(name = "Institucional 360", widthDp = 360, heightDp = 800)
@Preview(name = "Institucional horizontal", widthDp = 640, heightDp = 360)
@Composable
private fun WelcomePreview() { BeaconChatTheme { WelcomeScreen() } }
