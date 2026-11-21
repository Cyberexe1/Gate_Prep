package com.gateprep.app.ui.home

import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gateprep.app.AppViewModel
import com.gateprep.app.data.SubjectInfo
import com.gateprep.app.ui.components.GradientProgress
import com.gateprep.app.ui.components.SectionTitle
import com.gateprep.app.ui.components.StaggerIn
import com.gateprep.app.ui.components.SubjectTile
import com.gateprep.app.ui.components.barSpace
import com.gateprep.app.ui.components.clickableScale
import com.gateprep.app.ui.theme.LocalExtra
import com.gateprep.app.ui.theme.hex
import java.util.Calendar

@Composable
fun HomeScreen(vm: AppViewModel, onOpenSubject: (String) -> Unit, onResume: (String, String) -> Unit) {
    val ex = LocalExtra.current
    val index = vm.index
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        StaggerIn(0) { Header() }
        Spacer(Modifier.height(22.dp))

        if (index == null) {
            Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ex.brandStart)
            }
        } else {
            StaggerIn(1) { CoverageCard(vm) }
            Spacer(Modifier.height(26.dp))

            StaggerIn(2) {
                Column {
                    SectionTitle("Continue learning")
                    Spacer(Modifier.height(12.dp))
                    ContinueCard(vm, onResume, onOpenSubject = { index.subjects.firstOrNull()?.let { onOpenSubject(it.id) } })
                }
            }
            Spacer(Modifier.height(26.dp))

            StaggerIn(3) { SectionTitle("All topics", trailing = "${index.subjects.size} subjects") }
            Spacer(Modifier.height(14.dp))
            index.subjects.chunked(2).forEachIndexed { row, pair ->
                StaggerIn(4 + row) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        pair.forEach { s ->
                            SubjectCard(s, vm, Modifier.weight(1f)) { onOpenSubject(s.id) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
        Spacer(Modifier.height(barSpace()))
    }
}

@Composable
private fun Header() {
    val ex = LocalExtra.current
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night study?"
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(greeting, fontSize = 14.sp, color = ex.muted, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.WavingHand, null, tint = Color(0xFFFFB300), modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text("GATE CS Practice", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(ex.brand),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.TrackChanges, null, tint = Color.White, modifier = Modifier.size(25.dp)) }
    }
}

// ------------------------------------------------------------------ coverage

@Composable
private fun CoverageCard(vm: AppViewModel) {
    val ex = LocalExtra.current
    val total = vm.totalQuestions
    val done = vm.doneCount
    val fraction = if (total == 0) 0f else done.toFloat() / total

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(ex.brand)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.10f), radius = size.width * 0.38f, center = Offset(size.width * 0.95f, -size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.28f, center = Offset(-size.width * 0.05f, size.height * 1.05f))
            }
            .padding(22.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoverageRing(fraction, Modifier.size(118.dp))
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("Overall coverage", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$done / $total", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    Text("questions practised", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("Correct", vm.correctCount.toString(), Modifier.weight(1f))
                StatPill("Wrong", vm.wrongCount.toString(), Modifier.weight(1f))
                StatPill("Accuracy", "${vm.accuracy}%", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CoverageRing(fraction: Float, modifier: Modifier = Modifier) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val animated by animateFloatAsState(if (go) fraction else 0f, tween(1400, easing = FastOutSlowInEasing), label = "ring")
    val percent = animated * 100f
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 13.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.22f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // keep a visible dot even at 0% so the ring never looks empty/broken
            val sweep = (animated * 360f).coerceAtLeast(0.1f)
            drawArc(Color.White, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when {
                    fraction > 0f && animated * 100f < 1f -> "<1%"
                    animated * 100f < 10f && fraction * 100f >= 1f -> "%.1f%%".format(percent)
                    else -> "%.0f%%".format(percent)
                },
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text("done", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ last topic

@Composable
private fun ContinueCard(vm: AppViewModel, onResume: (String, String) -> Unit, onOpenSubject: () -> Unit) {
    val ex = LocalExtra.current
    val subject = vm.subject(vm.lastSubject)
    val module = subject?.modules?.firstOrNull { it.id == vm.lastModule }

    if (subject == null || module == null) {
        // nothing opened yet
        Surface(
            Modifier.fillMaxWidth().clickableScale { onOpenSubject() },
            shape = RoundedCornerShape(24.dp), color = ex.card, shadowElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, ex.outline),
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(ex.brand), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoStories, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Start your first topic", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Pick any subject below. Your last topic shows up here.", fontSize = 13.sp, color = ex.muted)
                }
            }
        }
        return
    }

    val c1 = hex(subject.c1); val c2 = hex(subject.c2)
    val done = vm.moduleDone(module.id)
    Surface(
        Modifier.fillMaxWidth().clickableScale { onResume(subject.id, module.id) },
        shape = RoundedCornerShape(24.dp), color = ex.card, shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ex.outline),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectTile(subject.id, c1, c2, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(subject.short.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c1, letterSpacing = 0.8.sp)
                    Text(
                        module.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface, lineHeight = 21.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c1, c2))), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Resume", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            GradientProgress(if (module.count == 0) 0f else done.toFloat() / module.count, c1, c2)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$done of ${module.count} practised", fontSize = 12.sp, color = ex.muted)
                Text("Resume", fontSize = 12.sp, color = c1, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------------------------------------------------------ subjects

@Composable
fun EmojiTile(emoji: String, c1: Color, c2: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * 0.46f).sp) }
}

@Composable
private fun SubjectCard(subject: SubjectInfo, vm: AppViewModel, modifier: Modifier, onClick: () -> Unit) {
    val ex = LocalExtra.current
    val c1 = hex(subject.c1); val c2 = hex(subject.c2)
    val done = vm.subjectDone(subject)
    val fraction = if (subject.count == 0) 0f else done.toFloat() / subject.count
    Surface(
        modifier.clickableScale(onClick),
        shape = RoundedCornerShape(26.dp), color = ex.card, shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ex.outline),
    ) {
        Box(
            Modifier.drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(c1.copy(alpha = if (ex.isDark) 0.28f else 0.20f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.9f),
                    radius = size.width * 0.9f, center = Offset(size.width, 0f),
                )
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                SubjectTile(subject.id, c1, c2, 52.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    subject.short, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp, modifier = Modifier.height(42.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text("${subject.count} questions · ${subject.modules.size} modules", fontSize = 11.sp, color = ex.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                GradientProgress(fraction, c1, c2, height = 6.dp)
                Spacer(Modifier.height(6.dp))
                Text("${(fraction * 100).toInt()}% done", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c1)
            }
        }
    }
}
