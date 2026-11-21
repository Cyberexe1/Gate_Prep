package com.gateprep.app.ui.mock

import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gateprep.app.AppViewModel
import com.gateprep.app.data.MockInfo
import com.gateprep.app.ui.components.StaggerIn
import com.gateprep.app.ui.components.barSpace
import com.gateprep.app.ui.components.clickableScale
import com.gateprep.app.ui.theme.LocalExtra
import com.gateprep.app.ui.theme.hex
import kotlinx.coroutines.delay

fun mockIcon(n: Int): ImageVector = when (n) {
    1 -> Icons.Rounded.RocketLaunch
    2 -> Icons.Rounded.Waves
    3 -> Icons.Rounded.LocalFireDepartment
    4 -> Icons.Rounded.Eco
    else -> Icons.Rounded.AutoAwesome
}

fun fmtScore(v: Double): String = if (v == Math.floor(v)) v.toInt().toString() else String.format("%.2f", v)

fun fmtClock(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/** The Mock Test tab: five boxes, one per full-length paper. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockListScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val ex = LocalExtra.current
    val mocks = vm.mockIndex?.mocks.orEmpty()
    var sheet by remember { mutableStateOf<MockInfo?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    LazyColumn(
        Modifier.fillMaxSize().background(ex.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = barSpace() + 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StaggerIn(0) {
                Column {
                    Text("Mock Tests", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "5 full-length GATE papers · 65 questions · 100 marks · 3 hours",
                        fontSize = 14.sp, color = ex.muted,
                    )
                }
            }
        }
        itemsIndexed(mocks, key = { _, m -> m.id }) { i, m ->
            StaggerIn(i + 1) { MockCard(vm, m, now) { sheet = m } }
        }
    }

    sheet?.let { m ->
        val s = vm.mockSessions[m.id]
        val running = s != null && !s.finished && s.remainingMs(now) > 0
        val finishedOrExpired = s != null && (s.finished || s.remainingMs(now) <= 0)
        val c1 = hex(m.c1); val c2 = hex(m.c2)
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ex.card,
        ) {
            Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(c1, c2))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(mockIcon(m.n), null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(m.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${m.questions} questions · ${m.marks} marks · ${m.minutes} min", fontSize = 13.sp, color = ex.muted)
                    }
                }
                Spacer(Modifier.height(16.dp))
                m.sections.forEach { sec ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(c1))
                        Spacer(Modifier.width(10.dp))
                        Text("${sec.name}: Q${sec.from}–${sec.to} · ${sec.marks} marks", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ex.cardAlt).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Marking scheme", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Rule("Correct answer: +1 or +2 marks")
                    Rule("Wrong single-choice answer: −1/3 or −2/3 mark")
                    Rule("Numerical answer questions (${m.nat}): no negative marking")
                    Rule("Unanswered questions: 0")
                    Rule("The timer keeps running if you leave; the test submits itself at 0:00:00")
                }
                Spacer(Modifier.height(18.dp))

                if (finishedOrExpired) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BigButton("View result", Icons.Rounded.Check, c1, c2, outlined = true, modifier = Modifier.weight(1f)) {
                            sheet = null; onOpen(m.id)
                        }
                        BigButton("Retake", Icons.Rounded.Replay, c1, c2, modifier = Modifier.weight(1f)) {
                            vm.startMock(m, restart = true) { sheet = null; onOpen(m.id) }
                        }
                    }
                } else {
                    BigButton(if (running) "Resume test · ${fmtClock(s!!.remainingMs(now))} left" else "Start test", Icons.Rounded.PlayArrow, c1, c2, modifier = Modifier.fillMaxWidth()) {
                        if (running) {
                            sheet = null; onOpen(m.id)
                        } else {
                            vm.startMock(m) { sheet = null; onOpen(m.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Rule(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = LocalExtra.current.muted, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = LocalExtra.current.muted)
    }
}

@Composable
private fun BigButton(
    text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, c1: Color, c2: Color,
    modifier: Modifier = Modifier, outlined: Boolean = false, onClick: () -> Unit,
) {
    val ex = LocalExtra.current
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier.height(54.dp).clip(shape)
            .then(if (outlined) Modifier.background(ex.card).border(BorderStroke(1.5.dp, c1), shape) else Modifier.background(Brush.horizontalGradient(listOf(c1, c2))))
            .clickableScale(onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (outlined) c1 else Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, color = if (outlined) c1 else Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun MockCard(vm: AppViewModel, m: MockInfo, now: Long, onClick: () -> Unit) {
    val ex = LocalExtra.current
    val c1 = hex(m.c1); val c2 = hex(m.c2)
    val s = vm.mockSessions[m.id]
    val best = vm.mockBest[m.id]
    val running = s != null && !s.finished && s.remainingMs(now) > 0
    val expired = s != null && !s.finished && s.remainingMs(now) <= 0

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(c1, c2))).clickableScale(onClick),
    ) {
        // decorative circles
        Box(Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-30).dp).size(130.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-30).dp, y = 40.dp).size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    Icon(mockIcon(m.n), null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("${m.questions} Q · ${m.marks} marks · ${m.minutes} min", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                }
                val label = when {
                    s?.finished == true -> "Retake"
                    running -> "Resume"
                    else -> "Start"
                }
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (s?.finished == true) Icons.Rounded.Replay else Icons.Rounded.PlayArrow, null, tint = c1, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(label, color = c1, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val text = when {
                    s?.finished == true -> "Last score ${fmtScore(s.score)} / ${m.marks}" + (best?.let { "  ·  Best ${fmtScore(it)}" } ?: "")
                    running -> "In progress · ${fmtClock(s!!.remainingMs(now))} left · ${s.answers.size} answered"
                    expired -> "Time is up · tap to see your result"
                    else -> "Not attempted yet"
                }
                if (running) Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(16.dp).padding(end = 0.dp))
                if (running) Spacer(Modifier.width(6.dp))
                Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
