package com.gateprep.app.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.gateprep.app.AppViewModel
import com.gateprep.app.data.Question
import com.gateprep.app.ui.components.Shimmer
import com.gateprep.app.ui.components.clickableScale
import com.gateprep.app.ui.theme.LocalExtra
import com.gateprep.app.ui.theme.hex
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

fun questionUrl(id: String) = com.gateprep.app.net.ApiConfig.imageUrl("q", id)
fun solutionUrl(id: String) = com.gateprep.app.net.ApiConfig.imageUrl("s", id)

@Composable
fun PracticeScreen(vm: AppViewModel, subjectId: String, moduleId: String, onBack: () -> Unit) {
    val ex = LocalExtra.current
    val subject = vm.subject(subjectId)
    val module = subject?.modules?.firstOrNull { it.id == moduleId }
    val c1 = hex(subject?.c1 ?: "#5B5BF0")
    val c2 = hex(subject?.c2 ?: "#8E54E9")

    var questions by remember(moduleId) { mutableStateOf<List<Question>?>(null) }
    var loadError by remember(moduleId) { mutableStateOf<String?>(null) }
    var attempt by remember(moduleId) { mutableStateOf(0) }
    LaunchedEffect(moduleId, attempt) {
        loadError = null
        vm.questions(moduleId).fold(
            onSuccess = { questions = it; vm.enter(subjectId, moduleId) },
            onFailure = { loadError = it.message ?: "Could not load the questions." },
        )
    }

    Column(Modifier.fillMaxSize().background(ex.bg).statusBarsPadding()) {
        val qs = questions
        if (qs == null || qs.isEmpty()) {
            TopBar(module?.name ?: "", subject?.short ?: "", 0, 0, 0f, c1, c2, onBack, onGrid = {})
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val err = loadError
                if (err == null) CircularProgressIndicator(color = c1)
                else Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("Couldn't load the questions", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(err, fontSize = 13.sp, color = ex.muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Try again", true, c1, c2) { attempt++ }
                }
            }
        } else {
            PracticeBody(vm, qs, moduleId, module?.name ?: "", subject?.short ?: "", c1, c2, onBack)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.PracticeBody(
    vm: AppViewModel, qs: List<Question>, moduleId: String, moduleName: String, subjectName: String,
    c1: Color, c2: Color, onBack: () -> Unit,
) {
    val ex = LocalExtra.current
    val start = remember(moduleId) { (vm.positions[moduleId] ?: 0).coerceIn(0, qs.lastIndex) }
    val pager = rememberPagerState(initialPage = start) { qs.size }
    val scope = rememberCoroutineScope()
    var showGrid by remember { mutableStateOf(false) }

    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { vm.setPosition(moduleId, it) }
    }

    val done = vm.moduleDone(moduleId)
    TopBar(
        moduleName, subjectName, pager.currentPage + 1, qs.size,
        if (qs.isEmpty()) 0f else done.toFloat() / qs.size, c1, c2, onBack, onGrid = { showGrid = true },
    )

    HorizontalPager(
        state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1,
        pageSpacing = 12.dp, key = { qs[it].id },
    ) { page ->
        val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
        Box(
            Modifier.graphicsLayer {
                val s = lerp(0.92f, 1f, 1f - offset)
                scaleX = s; scaleY = s
                alpha = lerp(0.4f, 1f, 1f - offset)
            },
        ) {
            QuestionPage(vm, qs[page], moduleId, c1, c2)
        }
    }

    // previous / next
    Surface(color = ex.bg, shadowElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val atStart = pager.currentPage == 0
            val atEnd = pager.currentPage == qs.lastIndex
            Row(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).border(1.5.dp, if (atStart) ex.outline else c1.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
                    .clickable(enabled = !atStart) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = if (atStart) ex.muted.copy(alpha = 0.5f) else c1, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Previous", fontWeight = FontWeight.SemiBold, color = if (atStart) ex.muted.copy(alpha = 0.5f) else c1)
            }
            Row(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Brush.horizontalGradient(listOf(c1, c2)))
                    .clickable { if (atEnd) onBack() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Text(if (atEnd) "Finish" else "Next", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(6.dp))
                Icon(if (atEnd) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showGrid) {
        ModalBottomSheet(
            onDismissRequest = { showGrid = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ex.card,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
                Text("Jump to question", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(ex.success, "Correct"); Legend(ex.danger, "Wrong"); Legend(c1, "Practised"); Legend(ex.outline, "New")
                }
                Spacer(Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(52.dp), modifier = Modifier.heightIn(max = 440.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(qs) { i, q ->
                        val rec = vm.attempts[q.id]
                        val seen = vm.viewed.containsKey(q.id)
                        val bg = when { rec?.ok == true -> ex.success; rec != null -> ex.danger; seen -> c1; else -> ex.cardAlt }
                        val fg = if (rec != null || seen) Color.White else MaterialTheme.colorScheme.onSurface
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                                .then(if (i == pager.currentPage) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp)) else Modifier)
                                .clickable { showGrid = false; scope.launch { pager.scrollToPage(i) } },
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", color = fg, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, color = LocalExtra.current.muted)
    }
}

@Composable
private fun TopBar(
    title: String, subtitle: String, current: Int, total: Int, progress: Float,
    c1: Color, c2: Color, onBack: () -> Unit, onGrid: () -> Unit,
) {
    val ex = LocalExtra.current
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(ex.card).border(1.dp, ex.outline, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(subtitle.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c1, letterSpacing = 0.8.sp)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(21.dp)).background(Brush.linearGradient(listOf(c1, c2))).clickable(onClick = onGrid)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (total == 0) "–" else "$current / $total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.GridView, "Questions", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        com.gateprep.app.ui.components.GradientProgress(progress, c1, c2, height = 5.dp)
        Spacer(Modifier.height(10.dp))
    }
}

// ------------------------------------------------------------------ one question

internal enum class OptState { IDLE, SELECTED, CORRECT, WRONG, MISSED, DIM }

@Composable
private fun QuestionPage(vm: AppViewModel, q: Question, moduleId: String, c1: Color, c2: Color) {
    val ex = LocalExtra.current
    val rec = vm.attempts[q.id]
    val answered = rec != null
    val seen = vm.viewed.containsKey(q.id)

    var sel by remember(q.id, rec?.ts) { mutableStateOf(rec?.sel?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var nat by remember(q.id, rec?.ts) { mutableStateOf(if (q.type == "NAT") rec?.sel.orEmpty() else "") }
    var showSol by remember(q.id, rec?.ok) { mutableStateOf(rec?.ok == false) }
    var fresh by remember(q.id, rec?.ts) { mutableStateOf(false) }
    var zoomUrl by remember { mutableStateOf<String?>(null) }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(rec?.ts) {
        if (fresh && rec?.ok == false) {
            repeat(3) { shake.animateTo(12f, tween(55)); shake.animateTo(-12f, tween(55)) }
            shake.animateTo(0f, tween(55))
        }
    }

    fun submit(selection: String) {
        fresh = true
        vm.submit(moduleId, q, selection, q.check(selection))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp),
    ) {
        // meta chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(if (q.src == "paper" && q.qno.isNotBlank()) "${q.label} · ${q.qno}" else q.label, c1)
            if (q.marks > 0) Chip("${q.marks} mark${if (q.marks > 1) "s" else ""}", ex.muted)
            if (q.hasKey) Chip(
                when (q.type) { "MCQ" -> "Single choice"; "MSQ" -> "Multiple choice"; else -> "Numerical" }, ex.muted,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (q.isText) {
            StemCard(q, onZoom = { zoomUrl = it })
        } else {
            // no clean text for this question yet: show the original image
            Surface(
                Modifier.fillMaxWidth().clickable { zoomUrl = questionUrl(q.id) },
                shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 4.dp,
                border = BorderStroke(1.dp, ex.outline),
            ) {
                Box(Modifier.padding(10.dp)) { PreviewImage(questionUrl(q.id), Modifier.fillMaxWidth()) }
            }
            Text("Tap the question to zoom", fontSize = 11.sp, color = ex.muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
        }
        Spacer(Modifier.height(16.dp))

        if (!q.hasKey) {
            if (q.isText && q.options.size == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("A", "B", "C", "D").forEachIndexed { i, letter ->
                        OptionRow(letter, q.options[i], OptState.IDLE, c1, Modifier.fillMaxWidth()) { }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            NoKeyBox(seen, q.hasSol) { vm.toggleViewed(moduleId, q.id) }
        } else {
            when (q.type) {
                "MCQ", "MSQ" -> {
                    val correctSet = q.answer.split(",").toSet()
                    val withText = q.isText && q.options.size == 4
                    val optionModifier = if (withText) Modifier.fillMaxWidth() else Modifier.fillMaxWidth()
                    val letters = listOf("A", "B", "C", "D")
                    fun stateOf(letter: String): OptState {
                        val picked = letter in sel
                        return when {
                            answered && letter in correctSet && picked -> OptState.CORRECT
                            answered && letter in correctSet -> OptState.MISSED
                            answered && picked -> OptState.WRONG
                            answered -> OptState.DIM
                            picked -> OptState.SELECTED
                            else -> OptState.IDLE
                        }
                    }
                    fun choose(letter: String) {
                        if (answered) return
                        if (q.type == "MCQ") { sel = setOf(letter); submit(letter) }
                        else sel = if (letter in sel) sel - letter else sel + letter
                    }
                    if (withText) {
                        Column(Modifier.graphicsLayer { translationX = shake.value.dp.toPx() }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            letters.forEachIndexed { i, letter ->
                                OptionRow(letter, q.options[i], stateOf(letter), c1, optionModifier) { choose(letter) }
                            }
                        }
                    } else Row(Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value.dp.toPx() }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        letters.forEach { letter ->
                            val picked = letter in sel
                            val state = when {
                                answered && letter in correctSet && picked -> OptState.CORRECT
                                answered && letter in correctSet -> OptState.MISSED
                                answered && picked -> OptState.WRONG
                                answered -> OptState.DIM
                                picked -> OptState.SELECTED
                                else -> OptState.IDLE
                            }
                            OptionButton(letter, state, c1, Modifier.weight(1f)) {
                                if (answered) return@OptionButton
                                if (q.type == "MCQ") { sel = setOf(letter); submit(letter) }
                                else sel = if (picked) sel - letter else sel + letter
                            }
                        }
                    }
                    if (q.type == "MSQ" && !answered) {
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton("Submit answer", sel.isNotEmpty(), c1, c2) { submit(sel.sorted().joinToString(",")) }
                        Text("More than one option can be correct", fontSize = 12.sp, color = ex.muted, modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
                else -> {
                    Column(Modifier.graphicsLayer { translationX = shake.value.dp.toPx() }) {
                        OutlinedTextField(
                            value = nat, onValueChange = { if (!answered) nat = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' } },
                            modifier = Modifier.fillMaxWidth(), enabled = !answered, singleLine = true,
                            label = { Text("Your answer") }, shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = c1, focusedLabelColor = c1, cursorColor = c1,
                                disabledBorderColor = if (rec?.ok == true) ex.success else ex.danger,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledLabelColor = ex.muted,
                            ),
                        )
                    }
                    if (!answered) {
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton("Check answer", nat.toDoubleOrNull() != null, c1, c2) { submit(nat.trim()) }
                    }
                }
            }
        }

        // result
        AnimatedVisibility(
            visible = answered && q.hasKey,
            enter = fadeIn(tween(300)) + expandVertically(tween(350)),
            exit = fadeOut() + shrinkVertically(),
        ) {
            val ok = rec?.ok == true
            Column {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (ok) ex.successBg else ex.dangerBg).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(if (ok) ex.success else ex.danger), contentAlignment = Alignment.Center) {
                        Icon(if (ok) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (ok) "Correct!" else "Not quite", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (ok) ex.success else ex.danger)
                        Text(
                            if (ok) "Nice work. Keep the streak going." else "Correct answer: ${q.displayAnswer}",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = { vm.clearAttempt(q.id) }) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp), tint = ex.muted)
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", color = ex.muted, fontSize = 13.sp)
                    }
                }
            }
        }

        // solution
        if ((answered && q.hasKey) || (!q.hasKey && q.hasSol)) {
            Spacer(Modifier.height(14.dp))
            if (q.hasSol) {
                SolutionCard(q.id, showSol, c1, onToggle = { showSol = !showSol }, onZoom = { zoomUrl = solutionUrl(q.id) })
            } else {
                Text("A written solution isn't available for this question.", fontSize = 13.sp, color = ex.muted, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }

    zoomUrl?.let { ZoomDialog(it) { zoomUrl = null } }
}

@Composable
private fun NoKeyBox(seen: Boolean, hasSol: Boolean, onToggle: () -> Unit) {
    val ex = LocalExtra.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ex.cardAlt).padding(16.dp),
    ) {
        Text("Answer key not available", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(
            if (hasSol) "Solve it on paper, then open the solution below to check your work."
            else "Solve it on paper and mark it as practised to track your coverage.",
            fontSize = 13.sp, color = ex.muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).background(if (seen) ex.success else ex.card).border(1.dp, if (seen) ex.success else ex.outline, RoundedCornerShape(16.dp))
                .clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Check, null, tint = if (seen) Color.White else ex.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (seen) "Practised" else "Mark as practised", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (seen) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
internal fun SolutionCard(id: String, expanded: Boolean, accent: Color, onToggle: () -> Unit, onZoom: () -> Unit) {
    val ex = LocalExtra.current
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = ex.card, shadowElevation = 3.dp, border = BorderStroke(1.dp, ex.outline)) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Solution", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                val rot by androidx.compose.animation.core.animateFloatAsState(if (expanded) 180f else 0f, tween(300), label = "chev")
                Icon(Icons.Rounded.ExpandMore, null, tint = ex.muted, modifier = Modifier.graphicsLayer { rotationZ = rot })
            }
            AnimatedVisibility(expanded, enter = expandVertically(tween(380)) + fadeIn(tween(380)), exit = shrinkVertically(tween(280)) + fadeOut(tween(200))) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(onClick = onZoom).padding(8.dp)) {
                        PreviewImage(solutionUrl(id), Modifier.fillMaxWidth())
                    }
                    Text("Tap to zoom", fontSize = 11.sp, color = ex.muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                }
            }
        }
    }
}

@Composable
internal fun PreviewImage(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(url).crossfade(250).build(),
        contentDescription = null, modifier = modifier, contentScale = ContentScale.FillWidth,
        loading = { Shimmer(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))) },
    )
}

@Composable
internal fun Chip(text: String, color: Color) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.13f)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
internal fun PrimaryButton(text: String, enabled: Boolean, c1: Color, c2: Color, onClick: () -> Unit) {
    val ex = LocalExtra.current
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
            .background(if (enabled) Brush.horizontalGradient(listOf(c1, c2)) else Brush.horizontalGradient(listOf(ex.cardAlt, ex.cardAlt)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.Bold, color = if (enabled) Color.White else ex.muted) }
}

/** The question itself, rebuilt from text: paragraphs, code blocks and small figure images. */
@Composable
internal fun StemCard(q: Question, onZoom: (String) -> Unit) {
    val ex = LocalExtra.current
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = ex.card, shadowElevation = 3.dp,
        border = BorderStroke(1.dp, ex.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            q.stem.forEach { b ->
                when (b.k) {
                    "code" -> Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (ex.isDark) Color(0xFF0F1226) else Color(0xFFF1F3FC))
                            .horizontalScroll(rememberScrollState()).padding(12.dp),
                    ) {
                        Text(
                            markup(b.t), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp,
                            softWrap = false, color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    "fig" -> {
                        val url = com.gateprep.app.net.ApiConfig.imageUrl("f", b.src)
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).clickable { onZoom(url) }.padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) { PreviewImage(url, Modifier.fillMaxWidth()) }
                    }
                    else -> Text(
                        markup(b.t), fontSize = 16.sp, lineHeight = 25.sp, color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** An answer option with its own text, used when the question was rebuilt from text. */
@Composable
internal fun OptionRow(letter: String, text: String, state: OptState, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val ex = LocalExtra.current
    val container by androidx.compose.animation.animateColorAsState(
        when (state) {
            OptState.CORRECT -> ex.success
            OptState.WRONG -> ex.danger
            OptState.MISSED -> ex.successBg
            OptState.SELECTED -> accent.copy(alpha = 0.14f)
            OptState.DIM -> ex.cardAlt.copy(alpha = 0.45f)
            OptState.IDLE -> ex.card
        }, tween(300), label = "optrow-bg",
    )
    val border by androidx.compose.animation.animateColorAsState(
        when (state) {
            OptState.CORRECT -> ex.success
            OptState.WRONG -> ex.danger
            OptState.MISSED -> ex.success
            OptState.SELECTED -> accent
            else -> ex.outline
        }, tween(300), label = "optrow-border",
    )
    val onColor = when (state) {
        OptState.CORRECT, OptState.WRONG -> Color.White
        OptState.DIM -> ex.muted.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val badgeBg = when (state) {
        OptState.CORRECT, OptState.WRONG -> Color.White.copy(alpha = 0.25f)
        OptState.MISSED -> ex.success.copy(alpha = 0.2f)
        OptState.SELECTED -> accent
        else -> ex.cardAlt
    }
    val badgeFg = when (state) {
        OptState.CORRECT, OptState.WRONG -> Color.White
        OptState.MISSED -> ex.success
        OptState.SELECTED -> Color.White
        else -> ex.muted
    }
    val pop by androidx.compose.animation.core.animateFloatAsState(
        if (state == OptState.CORRECT) 1.03f else 1f,
        androidx.compose.animation.core.spring(androidx.compose.animation.core.Spring.DampingRatioMediumBouncy), label = "optrow-pop",
    )
    Row(
        modifier.graphicsLayer { scaleX = pop; scaleY = pop }.clip(RoundedCornerShape(20.dp)).background(container)
            .border(2.dp, border, RoundedCornerShape(20.dp)).clickableScale(onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
            Text(letter, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = badgeFg)
        }
        Spacer(Modifier.width(12.dp))
        Text(markup(text), fontSize = 15.sp, lineHeight = 21.sp, color = onColor, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun OptionButton(letter: String, state: OptState, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val ex = LocalExtra.current
    val container by androidx.compose.animation.animateColorAsState(
        when (state) {
            OptState.CORRECT -> ex.success
            OptState.WRONG -> ex.danger
            OptState.MISSED -> ex.successBg
            OptState.SELECTED -> accent.copy(alpha = 0.16f)
            OptState.DIM -> ex.cardAlt.copy(alpha = 0.5f)
            OptState.IDLE -> ex.card
        }, tween(300), label = "opt-bg",
    )
    val border by androidx.compose.animation.animateColorAsState(
        when (state) {
            OptState.CORRECT -> ex.success
            OptState.WRONG -> ex.danger
            OptState.MISSED -> ex.success
            OptState.SELECTED -> accent
            else -> ex.outline
        }, tween(300), label = "opt-border",
    )
    val content = when (state) {
        OptState.CORRECT, OptState.WRONG -> Color.White
        OptState.MISSED -> ex.success
        OptState.SELECTED -> accent
        OptState.DIM -> ex.muted.copy(alpha = 0.6f)
        OptState.IDLE -> MaterialTheme.colorScheme.onSurface
    }
    val pop by androidx.compose.animation.core.animateFloatAsState(
        if (state == OptState.CORRECT) 1.07f else 1f,
        androidx.compose.animation.core.spring(androidx.compose.animation.core.Spring.DampingRatioMediumBouncy), label = "opt-pop",
    )
    Box(
        modifier.height(64.dp).graphicsLayer { scaleX = pop; scaleY = pop }
            .clip(RoundedCornerShape(20.dp)).background(container).border(2.dp, border, RoundedCornerShape(20.dp)).clickableScale(onClick),
        contentAlignment = Alignment.Center,
    ) { Text(letter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = content) }
}
