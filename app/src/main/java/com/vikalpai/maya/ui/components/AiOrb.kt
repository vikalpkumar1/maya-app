package com.vikalpai.maya.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.vikalpai.maya.ui.theme.MayaOrbGradientEnd
import com.vikalpai.maya.ui.theme.MayaOrbGradientStart

@Composable
fun AiOrb(isThinking: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue = if (isThinking) 0.9f else 1f,
        targetValue = if (isThinking) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 450 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    Box(
        modifier = modifier
            .size(96.dp)
            .scale(scale)
            .background(
                brush = Brush.radialGradient(colors = listOf(MayaOrbGradientStart, MayaOrbGradientEnd)),
                shape = CircleShape
            )
    )
}
