package com.gateprep.app.ui.placeholder

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gateprep.app.AppViewModel
import com.gateprep.app.ui.components.StaggerIn
import com.gateprep.app.ui.components.barSpace
import com.gateprep.app.ui.theme.LocalExtra

@Composable
fun ChatScreen() = ComingSoon(
    icon = Icons.Rounded.SmartToy, title = "AI Doubt Solver", tag = "Coming soon",
    description = "Ask a doubt about any question and get a step-by-step explanation in simple words.",
    points = listOf("Ask from any question or topic", "Step-by-step explanations", "Quick revision tips"),
)

@Composable
private fun ComingSoon(icon: ImageVector, title: String, tag: String, description: String, points: List<String>) {
    val ex = LocalExtra.current
    val t = rememberInfiniteTransition(label = "float")
    val bob by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val pulse by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(190.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.clip(CircleShape)
                    .background(Brush.radialGradient(listOf(ex.brandStart.copy(alpha = 0.32f), Color.Transparent))),
            )
            Box(
                Modifier.size(112.dp).graphicsLayer { translationY = bob.dp.toPx() }.clip(RoundedCornerShape(34.dp)).background(ex.brand),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(58.dp)) }
        }
        Spacer(Modifier.height(30.dp))
        Text(
            tag.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ex.brandStart, letterSpacing = 1.2.sp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(ex.brandStart.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 5.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(description, fontSize = 15.sp, color = ex.muted, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(26.dp))
        points.forEachIndexed { i, p ->
            StaggerIn(i) {
                Surface(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), color = ex.card,
                    border = BorderStroke(1.dp, ex.outline),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ex.brand))
                        Spacer(Modifier.width(12.dp))
                        Text(p, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(Modifier.height(barSpace()))
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val ex = LocalExtra.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(30.dp))
        StaggerIn(0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(104.dp).clip(CircleShape).background(ex.brand)
                        .drawBehind { drawCircle(Color.White.copy(alpha = 0.14f), radius = size.width * 0.55f, center = Offset(size.width * 0.85f, 0f)) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.School, null, tint = Color.White, modifier = Modifier.size(52.dp)) }
                Spacer(Modifier.height(14.dp))
                Text("GATE Aspirant", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Computer Science & IT", fontSize = 14.sp, color = ex.muted)
            }
        }
        Spacer(Modifier.height(26.dp))
        StaggerIn(1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStat("Practised", "${vm.doneCount}", Modifier.weight(1f))
                ProfileStat("Correct", "${vm.correctCount}", Modifier.weight(1f))
                ProfileStat("Accuracy", "${vm.accuracy}%", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(22.dp))
        listOf("Appearance" to "Theme follows your phone setting", "Notifications" to "Daily practice reminders", "About" to "GATE Prep 0.1.0").forEachIndexed { i, (title, sub) ->
            StaggerIn(2 + i) {
                Surface(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(20.dp), color = ex.card,
                    border = BorderStroke(1.dp, ex.outline),
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("Soon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ex.brandStart)
                        }
                        Text(sub, fontSize = 13.sp, color = ex.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(barSpace()))
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    val ex = LocalExtra.current
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = ex.card, shadowElevation = 3.dp, border = BorderStroke(1.dp, ex.outline)) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = ex.brandStart)
            Text(label, fontSize = 12.sp, color = ex.muted)
        }
    }
}
