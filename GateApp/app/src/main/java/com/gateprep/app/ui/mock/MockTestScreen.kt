package com.gateprep.app.ui.mock

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.gateprep.app.AppViewModel
import com.gateprep.app.data.MockInfo
import com.gateprep.app.data.MockSession
import com.gateprep.app.data.Question
import com.gateprep.app.data.markFor
import com.gateprep.app.ui.components.GradientProgress
import com.gateprep.app.ui.components.clickableScale
import com.gateprep.app.ui.practice.Chip
import com.gateprep.app.ui.practice.OptState
import com.gateprep.app.ui.practice.OptionButton
import com.gateprep.app.ui.practice.OptionRow
import com.gateprep.app.ui.practice.PreviewImage
import com.gateprep.app.ui.practice.PrimaryButton
import com.gateprep.app.ui.practice.SolutionCard
import com.gateprep.app.ui.practice.StemCard
import com.gateprep.app.ui.practice.ZoomDialog
import com.gateprep.app.ui.practice.questionUrl
import com.gateprep.app.ui.practice.solutionUrl
import com.gateprep.app.ui.theme.LocalExtra
import com.gateprep.app.ui.theme.hex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { EXAM, RESULT, REVIEW }

private val ReviewPurple = Color(0xFF8E54E9)

/** One screen for the whole life of a mock test: exam -> result -> answer review. */
@Composable
fun MockTestScreen(vm: AppViewModel, mockId: String, onExit: () -> Unit) {
    val ex = LocalExtra.current
    val info = vm.mock(mockId)
    val session = vm.mockSessions[mockId]
    var qs by remember(mockId) { mutableStateOf<List<Question>?>(null) }
    LaunchedEffect(mockId) {
        vm.refreshMock(mockId)
        vm.mockQuestions(mockId).onSuccess { qs = it }
    }

    val list = qs
    if (info == null || session == null || list == null) {
        Box(Modifier.fillMaxSize().background(ex.bg), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    var phase by remember(mockId) { mutableStateOf(if (session.finished) Phase.RESULT else Phase.EXAM) }
    var reviewAt by remember(mockId) { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmLeave by remember { mutableStateOf(false) }

    val displayList = if (session.finished) vm.withKeys(mockId, list) else list

    fun submit() {
        vm.finishMock(mockId, onDone = { phase = Phase.RESULT })
    }

    // the clock ticks on wall time; at zero the test submits itself
    LaunchedEffect(session.startTs, session.finished) {
        if (session.finished) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            if (session.remainingMs(now) <= 0) { submit(); break }
            delay(500)
        }
    }

    BackHandler {
        when (phase) {
            Phase.EXAM -> confirmLeave = true
            Phase.REVIEW -> phase = Phase.RESULT
            Phase.RESULT -> onExit()
        }
    }

    Box(Modifier.fillMaxSize().background(ex.bg)) {
        when (phase) {
            Phase.EXAM -> key(session.startTs) {
                ExamBody(vm, info, list, session, now, onLeave = { confirmLeave = true }, onSubmit = ::submit)
            }
            Phase.RESULT -> ResultBody(
                info, displayList, session,
                onReview = { reviewAt = it; phase = Phase.REVIEW },
                onRetake = { vm.startMock(info, restart = true) { phase = Phase.EXAM } },
                onExit = onExit,
            )
            Phase.REVIEW -> ReviewBody(info, displayList, session, reviewAt, onBack = { phase = Phase.RESULT })
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the test?") },
            text = { Text("Your answers are saved and the timer keeps running. Resume any time from the Mock Tests tab.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onExit() }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay") } },
            containerColor = ex.card,
        )
    }
}

// ---------------------------------------------------------------------------------------------- exam

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamBody(
    vm: AppViewModel, info: MockInfo, qs: List<Question>, session: MockSession, now: Long,
    onLeave: () -> Unit, onSubmit: () -> Unit,
) {
    val ex = LocalExtra.current
    val c1 = hex(info.c1); val c2 = hex(info.c2)
    val pager = rememberPagerState(initialPage = session.position.coerceIn(0, qs.lastIndex)) { qs.size }
    val scope = rememberCoroutineScope()
    var showPalette by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { vm.mockVisit(info.id, qs[it].id, it) }
    }

    val page = pager.currentPage
    val q = qs[page]
    val remaining = session.remainingMs(now)
    val low = remaining < 10 * 60_000L
    val answered = qs.count { session.answers.containsKey(it.id) }
    val marked = qs.count { it.id in session.review }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.Close, "Leave", onClick = onLeave)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (q.exam == "GA") "GENERAL APTITUDE" else "COMPUTER SCIENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c1, letterSpacing = 0.8.sp)
                Text("Question ${page + 1} of ${qs.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(if (low) ex.danger else ex.card)
                    .border(1.dp, if (low) ex.danger else ex.outline, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Timer, null, tint = if (low) Color.White else c1, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(fmtClock(remaining), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (low) Color.White else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c1, c2))).clickable { showPalette = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.GridView, "Question palette", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        GradientProgress(answered.toFloat() / qs.size, c1, c2, Modifier.padding(horizontal = 16.dp), height = 5.dp)
        Spacer(Modifier.height(10.dp))

        HorizontalPager(
            state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1, pageSpacing = 12.dp,
            key = { qs[it].id },
        ) { p -> ExamQuestion(vm, info.id, qs[p], session, c1, c2) }

        // bottom actions
        Surface(color = ex.bg) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Previous", enabled = page > 0) { scope.launch { pager.animateScrollToPage(page - 1) } }
                val isMarked = q.id in session.review
                PillButton(
                    if (isMarked) "Marked" else "Review", if (isMarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    ReviewPurple, active = isMarked,
                ) { vm.mockToggleReview(info.id, q.id) }
                PillButton("Clear", Icons.Rounded.Close, ex.muted, enabled = session.answers.containsKey(q.id)) { vm.mockAnswer(info.id, q.id, null) }
                val last = page == qs.lastIndex
                Row(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(c1, c2)))
                        .clickable { if (last) confirmSubmit = true else scope.launch { pager.animateScrollToPage(page + 1) } },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Text(if (last) "Submit" else "Next", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Icon(if (last) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showPalette) {
        ModalBottomSheet(
            onDismissRequest = { showPalette = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ex.card,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
                Text("Question palette", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Legend(ex.success, "Answered $answered")
                    Legend(ex.danger, "Not answered ${qs.size - answered}")
                    Legend(ReviewPurple, "Marked $marked")
                }
                Spacer(Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(52.dp), modifier = Modifier.heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(qs) { i, item ->
                        val isAns = session.answers.containsKey(item.id)
                        val isMark = item.id in session.review
                        val seen = item.id in session.visited
                        val bg = when {
                            isMark -> ReviewPurple
                            isAns -> ex.success
                            seen -> ex.danger
                            else -> ex.cardAlt
                        }
                        val fg = if (isMark || isAns || seen) Color.White else MaterialTheme.colorScheme.onSurface
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                                .then(if (i == page) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp)) else Modifier)
                                .clickable { showPalette = false; scope.launch { pager.scrollToPage(i) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", color = fg, fontWeight = FontWeight.Bold)
                            if (isMark && isAns) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(9.dp).clip(CircleShape).background(ex.success).border(1.dp, Color.White, CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Submit test", true, c1, c2) { showPalette = false; confirmSubmit = true }
            }
        }
    }

    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Submit the test?") },
            text = {
                Text(
                    "Answered $answered of ${qs.size}. ${qs.size - answered} not answered" +
                        (if (marked > 0) ", $marked marked for review" else "") + ".\nYou can't change answers after submitting.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmSubmit = false; onSubmit() }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Keep going") } },
            containerColor = ex.card,
        )
    }
}

@Composable
private fun ExamQuestion(vm: AppViewModel, mockId: String, q: Question, session: MockSession, c1: Color, c2: Color) {
    val ex = LocalExtra.current
    val sel = session.answers[q.id].orEmpty()
    var zoomUrl by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("Q${q.qno}", c1)
            Chip("${q.marks} mark${if (q.marks > 1) "s" else ""}", ex.muted)
            Chip(if (q.type == "MCQ") "Single choice" else "Numerical", ex.muted)
            if (q.id in session.review) Chip("Marked", ReviewPurple)
        }
        Spacer(Modifier.height(12.dp))
        QuestionBody(q) { zoomUrl = it }
        Spacer(Modifier.height(16.dp))

        if (q.type == "MCQ") {
            val letters = listOf("A", "B", "C", "D")
            if (q.isText && q.options.size == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    letters.forEachIndexed { i, l ->
                        OptionRow(l, q.options[i], if (sel == l) OptState.SELECTED else OptState.IDLE, c1, Modifier.fillMaxWidth()) {
                            vm.mockAnswer(mockId, q.id, if (sel == l) null else l)
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    letters.forEach { l ->
                        OptionButton(l, if (sel == l) OptState.SELECTED else OptState.IDLE, c1, Modifier.weight(1f)) {
                            vm.mockAnswer(mockId, q.id, if (sel == l) null else l)
                        }
                    }
                }
            }
            Text("Tap the selected option again, or use Clear, to remove your answer.", fontSize = 11.sp, color = ex.muted, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
        } else {
            OutlinedTextField(
                value = sel, onValueChange = { v -> vm.mockAnswer(mockId, q.id, v.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' }) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Your answer") }, shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c1, focusedLabelColor = c1, cursorColor = c1),
            )
            Text("Enter a number. No negative marking for numerical questions.", fontSize = 11.sp, color = ex.muted, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        }
    }
    zoomUrl?.let { ZoomDialog(it) { zoomUrl = null } }
}

/** Text stem when we have a clean rebuild, otherwise the original question image. */
@Composable
private fun QuestionBody(q: Question, onZoom: (String) -> Unit) {
    val ex = LocalExtra.current
    if (q.isText) {
        StemCard(q, onZoom = onZoom)
    } else {
        Surface(
            Modifier.fillMaxWidth().clickable { onZoom(questionUrl(q.id)) },
            shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 4.dp, border = BorderStroke(1.dp, ex.outline),
        ) { Box(Modifier.padding(10.dp)) { PreviewImage(questionUrl(q.id), Modifier.fillMaxWidth()) } }
        Text("Tap the question to zoom", fontSize = 11.sp, color = ex.muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
    }
}

// ---------------------------------------------------------------------------------------------- result

private enum class Outcome { CORRECT, WRONG, SKIPPED }

private fun outcomeOf(q: Question, s: MockSession): Outcome {
    val sel = s.answers[q.id]
    return when {
        sel.isNullOrBlank() -> Outcome.SKIPPED
        q.check(sel) -> Outcome.CORRECT
        else -> Outcome.WRONG
    }
}

@Composable
private fun ResultBody(info: MockInfo, qs: List<Question>, s: MockSession, onReview: (Int) -> Unit, onRetake: () -> Unit, onExit: () -> Unit) {
    val ex = LocalExtra.current
    val c1 = hex(info.c1); val c2 = hex(info.c2)
    val shown = remember { Animatable(0f) }
    LaunchedEffect(s.startTs, s.score) { shown.snapTo(0f); shown.animateTo(s.score.toFloat(), tween(1300, easing = FastOutSlowInEasing)) }
    val pct = (s.score / info.marks).coerceIn(0.0, 1.0).toFloat()
    val attempted = s.correct + s.wrong
    val accuracy = if (attempted == 0) 0 else Math.round(s.correct * 100f / attempted)
    val tookMs = (s.finishTs - s.startTs).coerceAtLeast(0L)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 28.dp).navigationBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onExit)
            Spacer(Modifier.width(12.dp))
            Text("${info.title} · Result", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(c1, c2)))) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(mockIcon(info.n), null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Your score", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(fmtScore(Math.round(shown.value * 100) / 100.0), fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(" / ${info.marks}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 10.dp))
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxWidth(pct).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        s.score >= 60 -> "Excellent. You are well above a typical cut-off."
                        s.score >= 40 -> "Strong attempt. A few more topics and you are there."
                        s.score >= 25 -> "Around cut-off territory. Review the wrong answers."
                        else -> "Plenty of room to grow. Go through the solutions below."
                    },
                    color = Color.White, fontSize = 14.sp, lineHeight = 20.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("${s.correct}", "Correct", ex.success, Modifier.weight(1f))
            StatBox("${s.wrong}", "Wrong", ex.danger, Modifier.weight(1f))
            StatBox("${s.skipped}", "Skipped", ex.muted, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("$accuracy%", "Accuracy", c1, Modifier.weight(1f))
            StatBox(fmtClock(tookMs), "Time taken", c1, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Text("Section-wise", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        info.sections.forEach { sec ->
            val got = if (sec.short == "GA") s.gaScore else s.csScore
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row {
                    Text(sec.name, modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${fmtScore(got)} / ${sec.marks}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(6.dp))
                GradientProgress((got / sec.marks).coerceIn(0.0, 1.0).toFloat(), c1, c2, height = 8.dp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Tap a question to review it", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(10.dp))
        // a plain wrapped grid (the page already scrolls, so no nested lazy list)
        val per = 6
        qs.chunked(per).forEachIndexed { row, chunk ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEachIndexed { j, q ->
                    val i = row * per + j
                    val o = outcomeOf(q, s)
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(when (o) { Outcome.CORRECT -> ex.success; Outcome.WRONG -> ex.danger; Outcome.SKIPPED -> ex.cardAlt })
                            .clickable { onReview(i) },
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", fontWeight = FontWeight.Bold, color = if (o == Outcome.SKIPPED) MaterialTheme.colorScheme.onSurface else Color.White) }
                }
                repeat(per - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Review answers & solutions", true, c1, c2) { onReview(0) }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).border(1.5.dp, c1, RoundedCornerShape(26.dp)).clickable(onClick = onRetake),
            contentAlignment = Alignment.Center,
        ) { Text("Retake this test", fontWeight = FontWeight.Bold, color = c1) }
    }
}

@Composable
private fun StatBox(value: String, label: String, color: Color, modifier: Modifier) {
    val ex = LocalExtra.current
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(ex.card).border(1.dp, ex.outline, RoundedCornerShape(20.dp)).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
        Text(label, fontSize = 12.sp, color = ex.muted)
    }
}

// ---------------------------------------------------------------------------------------------- review

@Composable
private fun ReviewBody(info: MockInfo, qs: List<Question>, s: MockSession, start: Int, onBack: () -> Unit) {
    val ex = LocalExtra.current
    val c1 = hex(info.c1); val c2 = hex(info.c2)
    val pager = rememberPagerState(initialPage = start.coerceIn(0, qs.lastIndex)) { qs.size }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp).padding(top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to result", onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("REVIEW · ${info.title.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c1, letterSpacing = 0.8.sp)
                Text("Question ${page + 1} of ${qs.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            val o = outcomeOf(qs[page], s)
            Text(
                when (o) { Outcome.CORRECT -> "Correct"; Outcome.WRONG -> "Wrong"; Outcome.SKIPPED -> "Skipped" },
                fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = when (o) { Outcome.CORRECT -> ex.success; Outcome.WRONG -> ex.danger; Outcome.SKIPPED -> ex.muted },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background((when (o) { Outcome.CORRECT -> ex.success; Outcome.WRONG -> ex.danger; Outcome.SKIPPED -> ex.muted }).copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1, pageSpacing = 12.dp, key = { qs[it].id }) { p ->
            ReviewQuestion(qs[p], s, c1)
        }
        Surface(color = ex.bg) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val atStart = page == 0
                val atEnd = page == qs.lastIndex
                Row(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).border(1.5.dp, if (atStart) ex.outline else c1.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
                        .clickable(enabled = !atStart) { scope.launch { pager.animateScrollToPage(page - 1) } },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = if (atStart) ex.muted.copy(alpha = 0.5f) else c1, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Previous", fontWeight = FontWeight.SemiBold, color = if (atStart) ex.muted.copy(alpha = 0.5f) else c1)
                }
                Row(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Brush.horizontalGradient(listOf(c1, c2)))
                        .clickable { if (atEnd) onBack() else scope.launch { pager.animateScrollToPage(page + 1) } },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Text(if (atEnd) "Done" else "Next", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Icon(if (atEnd) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewQuestion(q: Question, s: MockSession, c1: Color) {
    val ex = LocalExtra.current
    val sel = s.answers[q.id].orEmpty()
    val o = outcomeOf(q, s)
    var zoomUrl by remember { mutableStateOf<String?>(null) }
    var showSol by remember(q.id) { mutableStateOf(o != Outcome.CORRECT) }
    val delta = markFor(q, sel)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("Q${q.qno}", c1)
            Chip("${q.marks} mark${if (q.marks > 1) "s" else ""}", ex.muted)
            Chip(if (q.type == "MCQ") "Single choice" else "Numerical", ex.muted)
        }
        Spacer(Modifier.height(12.dp))
        QuestionBody(q) { zoomUrl = it }
        Spacer(Modifier.height(16.dp))

        if (q.type == "MCQ") {
            val letters = listOf("A", "B", "C", "D")
            fun st(l: String) = when {
                l == q.answer && l == sel -> OptState.CORRECT
                l == q.answer -> OptState.MISSED
                l == sel -> OptState.WRONG
                else -> OptState.DIM
            }
            if (q.isText && q.options.size == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    letters.forEachIndexed { i, l -> OptionRow(l, q.options[i], st(l), c1, Modifier.fillMaxWidth()) { } }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    letters.forEach { l -> OptionButton(l, st(l), c1, Modifier.weight(1f)) { } }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ex.cardAlt).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your answer: " + (if (sel.isBlank()) "—" else sel), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = when (o) { Outcome.CORRECT -> ex.success; Outcome.WRONG -> ex.danger; Outcome.SKIPPED -> ex.muted })
                Text("Correct answer: ${q.answer}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(14.dp))
        val good = o == Outcome.CORRECT
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (good) ex.successBg else if (o == Outcome.WRONG) ex.dangerBg else ex.cardAlt).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(if (good) ex.success else if (o == Outcome.WRONG) ex.danger else ex.muted),
                contentAlignment = Alignment.Center,
            ) { Icon(if (good) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (o) { Outcome.CORRECT -> "Correct"; Outcome.WRONG -> "Wrong answer"; Outcome.SKIPPED -> "Not attempted" },
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface,
                )
                if (q.type == "MCQ") Text("Correct option: ${q.answer}", fontSize = 12.sp, color = ex.muted)
            }
            Text(
                (if (delta > 0) "+" else if (delta < 0) "−" else "") + fmtScore(Math.abs(Math.round(delta * 100) / 100.0)),
                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (delta > 0) ex.success else if (delta < 0) ex.danger else ex.muted,
            )
        }

        Spacer(Modifier.height(14.dp))
        if (q.hasSol) {
            SolutionCard(q.id, showSol, c1, onToggle = { showSol = !showSol }, onZoom = { zoomUrl = solutionUrl(q.id) })
        } else {
            Text("A written solution isn't available for this question.", fontSize = 13.sp, color = ex.muted, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
    zoomUrl?.let { ZoomDialog(it) { zoomUrl = null } }
}

// ---------------------------------------------------------------------------------------------- small pieces

@Composable
private fun RoundIcon(icon: ImageVector, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    val ex = LocalExtra.current
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(ex.card).border(1.dp, ex.outline, CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = if (enabled) MaterialTheme.colorScheme.onSurface else ex.muted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PillButton(text: String, icon: ImageVector, color: Color, active: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val c = if (enabled) color else color.copy(alpha = 0.35f)
    Row(
        Modifier.height(48.dp).clip(shape).background(if (active) c.copy(alpha = 0.15f) else Color.Transparent).border(1.5.dp, c.copy(alpha = if (active) 1f else 0.6f), shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = c)
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
