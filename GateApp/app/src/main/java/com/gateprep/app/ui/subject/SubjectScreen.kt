package com.gateprep.app.ui.subject

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import com.gateprep.app.data.ModuleInfo
import com.gateprep.app.data.SubjectInfo
import com.gateprep.app.ui.components.GradientProgress
import com.gateprep.app.ui.components.StaggerIn
import com.gateprep.app.ui.components.barSpace
import com.gateprep.app.ui.components.clickableScale
import com.gateprep.app.ui.home.EmojiTile
import com.gateprep.app.ui.theme.LocalExtra
import com.gateprep.app.ui.theme.hex

@Composable
fun SubjectScreen(vm: AppViewModel, subjectId: String, onBack: () -> Unit, onOpenModule: (String) -> Unit) {
    val ex = LocalExtra.current
    val subject = vm.subject(subjectId)
    if (subject == null) {
        Box(Modifier.fillMaxSize().background(ex.bg))
        return
    }
    val c1 = hex(subject.c1); val c2 = hex(subject.c2)

    LazyColumn(Modifier.fillMaxSize()) {
        item("header") { Header(subject, c1, c2, onBack) }
        item("stats") {
            Box(Modifier.padding(horizontal = 20.dp).offset(y = (-28).dp)) { StatsCard(vm, subject, c1) }
        }
        item("title") {
            Text(
                "Modules", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp).offset(y = (-8).dp),
            )
        }
        itemsIndexed(subject.modules, key = { _, m -> m.id }) { i, m ->
            Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
                StaggerIn(i) { ModuleRow(i + 1, m, vm, c1, c2) { onOpenModule(m.id) } }
            }
        }
        item("end") { Spacer(Modifier.height(barSpace())) }
    }
}

@Composable
private fun Header(subject: SubjectInfo, c1: Color, c2: Color, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Brush.linearGradient(listOf(c1, c2)))
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.12f), radius = size.width * 0.42f, center = Offset(size.width * 0.92f, size.height * 0.1f))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.25f, center = Offset(size.width * 0.05f, size.height * 0.95f))
            }
            .statusBarsPadding()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 58.dp),
    ) {
        Column {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    Icon(com.gateprep.app.ui.components.subjectIcon(subject.id), null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(subject.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 27.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("${subject.count} questions · ${subject.modules.size} modules", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StatsCard(vm: AppViewModel, subject: SubjectInfo, c1: Color) {
    val ex = LocalExtra.current
    val done = vm.subjectDone(subject)
    val attempted = vm.subjectAttempted(subject)
    val acc = if (attempted == 0) 0 else Math.round(vm.subjectCorrect(subject) * 100f / attempted)
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = ex.card, shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ex.outline),
    ) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Stat("Practised", "$done", c1, Modifier.weight(1f))
            Divider()
            Stat("Remaining", "${subject.count - done}", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            Divider()
            Stat("Accuracy", "$acc%", ex.success, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).height(34.dp).background(LocalExtra.current.outline))
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
        Text(label, fontSize = 12.sp, color = LocalExtra.current.muted)
    }
}

@Composable
private fun ModuleRow(number: Int, module: ModuleInfo, vm: AppViewModel, c1: Color, c2: Color, onClick: () -> Unit) {
    val ex = LocalExtra.current
    val done = vm.moduleDone(module.id)
    val fraction = if (module.count == 0) 0f else done.toFloat() / module.count
    Surface(
        Modifier.fillMaxWidth().clickableScale(onClick),
        shape = RoundedCornerShape(22.dp), color = ex.card, shadowElevation = 3.dp, border = BorderStroke(1.dp, ex.outline),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(c1, c2))),
                contentAlignment = Alignment.Center,
            ) { Text("$number", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    module.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                GradientProgress(fraction, c1, c2, height = 6.dp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$done / ${module.count} practised", fontSize = 12.sp, color = ex.muted)
                    if (done >= module.count && module.count > 0) Text("Completed ✓", fontSize = 12.sp, color = ex.success, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = ex.muted)
        }
    }
}
