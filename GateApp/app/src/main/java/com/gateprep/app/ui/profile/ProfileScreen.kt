package com.gateprep.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gateprep.app.AppViewModel
import com.gateprep.app.ui.components.StaggerIn
import com.gateprep.app.ui.components.barSpace
import com.gateprep.app.ui.components.clickableScale
import com.gateprep.app.ui.theme.LocalExtra

@Composable
fun ProfileScreen(vm: AppViewModel, onLogin: () -> Unit, onSignup: () -> Unit) {
    val ex = LocalExtra.current
    val user = vm.user

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
                Text(user?.name ?: "GATE Aspirant", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user != null) {
                        Icon(Icons.Rounded.Email, null, tint = ex.muted, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        user?.email ?: "Sign in to synchronize your preparation",
                        fontSize = 14.sp, color = ex.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
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

        StaggerIn(2) {
            if (user == null) SignedOutCard(onLogin, onSignup) else SignedInCard(vm)
        }
        Spacer(Modifier.height(14.dp))

        listOf(
            "Appearance" to "Theme follows your phone setting",
            "Notifications" to "Daily practice reminders",
            "About" to "GATE Prep 0.1.0",
        ).forEachIndexed { i, (title, sub) ->
            StaggerIn(3 + i) {
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
private fun SignedOutCard(onLogin: () -> Unit, onSignup: () -> Unit) {
    val ex = LocalExtra.current
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = ex.card,
        shadowElevation = 3.dp, border = BorderStroke(1.dp, ex.outline),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(ex.brandStart.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Sync, null, tint = ex.brandStart, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Keep progress on every device", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Sign in before opening questions or mock tests.", fontSize = 13.sp, color = ex.muted)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileButton("Sign in", Icons.AutoMirrored.Rounded.Login, filled = true, modifier = Modifier.weight(1f), onClick = onLogin)
                ProfileButton("Create account", Icons.Rounded.PersonAdd, filled = false, modifier = Modifier.weight(1f), onClick = onSignup)
            }
        }
    }
}

@Composable
private fun SignedInCard(vm: AppViewModel) {
    val ex = LocalExtra.current
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = ex.card,
        shadowElevation = 3.dp, border = BorderStroke(1.dp, ex.outline),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(ex.successBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Sync, null, tint = ex.success, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Progress synchronization active", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Questions and mock sessions are linked to your account.", fontSize = 12.sp, color = ex.muted)
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(ex.dangerBg)
                    .clickable { vm.signOut() },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Logout, null, tint = ex.danger, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text("Sign out", color = ex.danger, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ex = LocalExtra.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.height(46.dp).clip(shape)
            .then(if (filled) Modifier.background(ex.brand) else Modifier.background(ex.card).then(Modifier.drawBorder(ex.brandStart, shape)))
            .clickableScale(onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (filled) Color.White else ex.brandStart, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (filled) Color.White else ex.brandStart, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

private fun Modifier.drawBorder(color: Color, shape: RoundedCornerShape): Modifier =
    this.then(Modifier.drawBehind {
        val stroke = 1.5.dp.toPx()
        drawRoundRect(color, topLeft = Offset(stroke / 2, stroke / 2), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx(), 22.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    })

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
