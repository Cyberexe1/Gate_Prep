package com.gateprep.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gateprep.app.ui.theme.LocalExtra
import kotlinx.coroutines.delay

/** Space the floating bottom bar needs at the end of a scrolling screen. */
@Composable
fun barSpace(): Dp = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Fades and slides content in, one after the other. */
@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    // saved per back-stack entry, so coming back to a screen does not replay the entrance animation
    var show by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!show) {
            delay(55L * index.coerceAtMost(14))
            show = true
        }
    }
    val alpha by animateFloatAsState(if (show) 1f else 0f, tween(420), label = "stagger-alpha")
    val offset by animateDpAsState(if (show) 0.dp else 26.dp, tween(520, easing = FastOutSlowInEasing), label = "stagger-offset")
    Box(Modifier.graphicsLayer { this.alpha = alpha; translationY = offset.toPx() }) { content() }
}

/** Click with a little press-in scale animation. */
fun Modifier.clickableScale(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Slim gradient progress bar that animates to its value. */
@Composable
fun GradientProgress(progress: Float, c1: Color, c2: Color, modifier: Modifier = Modifier, height: Dp = 8.dp, track: Color = LocalExtra.current.cardAlt) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(c1, c2)))
        )
    }
}

/** Animated loading placeholder. */
@Composable
fun Shimmer(modifier: Modifier = Modifier) {
    val ex = LocalExtra.current
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-300f, 900f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val base = if (ex.isDark) Color(0xFF232849) else Color(0xFFE9EBF6)
    val shine = if (ex.isDark) Color(0xFF30365F) else Color(0xFFF6F7FC)
    Box(modifier.background(Brush.linearGradient(listOf(base, shine, base), start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x + 300f, 120f))))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val ex = LocalExtra.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        if (trailing != null) Text(trailing, fontSize = 13.sp, color = ex.muted, fontWeight = FontWeight.Medium)
    }
}
