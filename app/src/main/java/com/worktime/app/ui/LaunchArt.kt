package com.worktime.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.worktime.app.R
import kotlinx.coroutines.delay

private val SplashArts = intArrayOf(
    R.drawable.splash_1,
    R.drawable.splash_2,
    R.drawable.splash_3,
    R.drawable.splash_4,
    R.drawable.splash_5,
)

private const val ShowMillis = 700L
private const val FadeOutMillis = 260L

@Composable
fun LaunchArt() {
    val art = rememberSaveable { SplashArts.random() }
    var active by rememberSaveable { mutableStateOf(true) }
    if (!active) return

    var fading by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (fading) 0f else 1f,
        animationSpec = tween(FadeOutMillis.toInt()),
        label = "launchArtAlpha",
    )

    LaunchedEffect(fading) {
        if (fading) {
            delay(FadeOutMillis)
            active = false
        } else {
            delay(ShowMillis)
            fading = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { fading = true },
            ),
    ) {
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
