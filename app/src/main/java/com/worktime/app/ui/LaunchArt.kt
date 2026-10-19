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

/**
 * 加载完成后决定一次是否展示：这样启动时关着开关、进设置再打开，也不会当场又弹一遍。
 * （原先在 enabled == false 时提前 return，shownThisProcess 没置位，正好漏掉这种情况。）
 */
private var decidedThisProcess = false

@Composable
fun LaunchArt(loaded: Boolean, enabled: Boolean) {
    if (!loaded || decidedThisProcess) return
    decidedThisProcess = true
    if (!enabled) return

    val art = remember { SplashArts.random() }
    var active by remember { mutableStateOf(true) }
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
