package com.gateprep.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.gateprep.app.data.AttemptRec
import com.gateprep.app.data.IndexFile
import com.gateprep.app.data.MockIndexFile
import com.gateprep.app.data.MockInfo
import com.gateprep.app.data.MockSession
import com.gateprep.app.data.ProgressFile
import com.gateprep.app.data.Question
import com.gateprep.app.data.SubjectInfo
import com.gateprep.app.net.ApiException
import com.gateprep.app.net.AuthUser
import com.gateprep.app.net.Leaderboard
import com.gateprep.app.net.MockKey
import com.gateprep.app.net.MockSummary
import com.gateprep.app.net.ServerSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val gate = app as GateApplication
    private val api = gate.api
    private val repo = gate.repo
    private val session = gate.session
    private val main = Handler(Looper.getMainLooper())

    // ------------------------------------------------------------------ account
    var user by mutableStateOf<AuthUser?>(session.user)
        private set
    val loggedIn: Boolean get() = user != null

    var authBusy by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)

    /** A one-line message for the UI to show once (e.g. "Session expired"). */
    var notice by mutableStateOf<String?>(null)

    /** Destination requested before the user was sent to Login. */
    var pendingDestination by mutableStateOf<String?>(null)
        private set

    /** The mock whose start sheet should open as soon as the user has signed in. */
    var pendingMock by mutableStateOf<String?>(null)
        private set

    // ------------------------------------------------------------------ public catalog
    var index by mutableStateOf<IndexFile?>(null)
        private set
    var mockIndex by mutableStateOf<MockIndexFile?>(null)
        private set

    // ------------------------------------------------------------------ practice progress
    val attempts = mutableStateMapOf<String, AttemptRec>()
    val viewed = mutableStateMapOf<String, String>()
    val positions = mutableStateMapOf<String, Int>()

    var lastSubject by mutableStateOf<String?>(null)
        private set
    var lastModule by mutableStateOf<String?>(null)
        private set
    private var lastTs = 0L

    private val questionCache = mutableMapOf<String, List<Question>>()
    private var saveJob: Job? = null
    private var stateJob: Job? = null

    // ------------------------------------------------------------------ mock tests
    val mockSessions = mutableStateMapOf<String, MockSession>()
    val mockBest = mutableStateMapOf<String, Double>()
    val mockAttempts = mutableStateMapOf<String, Int>()
    val mockKeys = mutableStateMapOf<String, Map<String, MockKey>>()

    /** Id of the mock that is being started or submitted (the UI shows a spinner). */
    var mockBusy by mutableStateOf<String?>(null)
        private set
    private val mockCache = mutableMapOf<String, List<Question>>()
    private val mockSaveJobs = mutableMapOf<String, Job>()
    private val mockDirty = mutableSetOf<String>()

    init {
        session.onUserChanged = { u -> main.post { applyUser(u) } }
        index = repo.quickIndex()
        mockIndex = repo.quickMockIndex()
        viewModelScope.launch { runCatching { repo.fetchIndex() }.onSuccess { index = it } }
        viewModelScope.launch { runCatching { repo.fetchMockIndex() }.onSuccess { mockIndex = it } }
        if (session.user != null) {
            applyProgress(repo.loadProgress())
            viewModelScope.launch { refreshUserData() }
        }
    }

    // ================================================================== account actions

    fun signIn(email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            authBusy = true; authError = null
            try {
                val response = api.login(email.trim(), password)
                applyUser(response.user)
                launch { refreshUserData() }
                onDone()
            } catch (e: ApiException) {
                authError = e.message
            } finally {
                authBusy = false
            }
        }
    }

    fun signUp(name: String, email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            authBusy = true; authError = null
            try {
                val response = api.register(name.trim(), email.trim(), password)
                applyUser(response.user)
                launch { refreshUserData() }
                onDone()
            } catch (e: ApiException) {
                authError = e.message
            } finally {
                authBusy = false
            }
        }
    }

    fun clearAuthError() {
        authError = null
    }

    fun requireAuth(destination: String, mockId: String? = null) {
        pendingDestination = destination
        pendingMock = mockId
        authError = null
    }

    fun consumePendingDestination(): String? = pendingDestination.also { pendingDestination = null }

    fun consumePendingMock(): String? = pendingMock.also { pendingMock = null }

    fun cancelPendingAuth() {
        pendingDestination = null
        pendingMock = null
        authError = null
    }

    fun signOut() {
        viewModelScope.launch { api.logout() }       // clears the session, which triggers applyUser(null)
    }

    private fun applyUser(u: AuthUser?) {
        val wasSignedIn = user != null
        user = u
        if (u == null && wasSignedIn) {
            // nothing of the account stays on the phone
            attempts.clear(); viewed.clear(); positions.clear()
            lastSubject = null; lastModule = null; lastTs = 0L
            questionCache.clear(); mockCache.clear(); mockSessions.clear(); mockBest.clear(); mockAttempts.clear(); mockKeys.clear()
            mockDirty.clear(); mockSaveJobs.values.forEach { it.cancel() }; mockSaveJobs.clear()
            viewModelScope.launch(Dispatchers.IO) {
                repo.clearUserData()
                val loader = Coil.imageLoader(getApplication())
                loader.diskCache?.clear(); loader.memoryCache?.clear()
            }
        }
    }

    /** Pulls the user's progress and mock status from the server. */
    suspend fun refreshUserData() {
        runCatching { val p = api.progress(); applyProgress(p); withContext(Dispatchers.IO) { repo.saveProgress(p) } }
        runCatching { applyMockSummary(api.mockSummary()) }
    }

    private fun applyProgress(p: ProgressFile) {
        attempts.clear(); attempts.putAll(p.attempts)
        viewed.clear(); viewed.putAll(p.viewed)
        positions.clear(); positions.putAll(p.positions)
        lastSubject = p.lastSubject; lastModule = p.lastModule; lastTs = p.lastTs
    }

    // ================================================================== lookups

    fun subject(id: String?): SubjectInfo? = index?.subjects?.firstOrNull { it.id == id }

    /** Questions of one module (needs a signed-in user). */
    suspend fun questions(moduleId: String): Result<List<Question>> {
        questionCache[moduleId]?.let { return Result.success(it) }
        return try {
            Result.success(repo.questions(moduleId).also { questionCache[moduleId] = it })
        } catch (e: ApiException) {
            if (e.isUnauthorized) notice = "Your session expired. Please sign in again."
            Result.failure(e)
        }
    }

    // ================================================================== stats

    val totalQuestions: Int get() = index?.subjects?.sumOf { it.count } ?: 0
    val doneCount: Int get() = attempts.size + viewed.size
    val correctCount: Int get() = attempts.values.count { it.ok }
    val wrongCount: Int get() = attempts.values.count { !it.ok }
    val accuracy: Int
        get() {
            val n = attempts.size
            return if (n == 0) 0 else Math.round(correctCount * 100f / n)
        }

    fun moduleDone(moduleId: String): Int =
        attempts.values.count { it.m == moduleId } + viewed.values.count { it == moduleId }

    fun subjectDone(subject: SubjectInfo): Int = subject.modules.sumOf { moduleDone(it.id) }

    fun subjectCorrect(subject: SubjectInfo): Int {
        val ids = subject.modules.map { it.id }.toSet()
        return attempts.values.count { it.ok && it.m in ids }
    }

    fun subjectAttempted(subject: SubjectInfo): Int {
        val ids = subject.modules.map { it.id }.toSet()
        return attempts.values.count { it.m in ids }
    }

    // ================================================================== practice actions (shown at once, then sent to the server)

    fun submit(moduleId: String, q: Question, selection: String, ok: Boolean) {
        attempts[q.id] = AttemptRec(selection, ok, System.currentTimeMillis(), moduleId)
        persist()
        fire { api.attempt(q.id, selection) }
    }

    fun clearAttempt(questionId: String) {
        attempts.remove(questionId)
        persist()
        fire { api.clearAttempt(questionId) }
    }

    fun toggleViewed(moduleId: String, questionId: String) {
        val now = !viewed.containsKey(questionId)
        if (now) viewed[questionId] = moduleId else viewed.remove(questionId)
        persist()
        fire { api.setViewed(questionId, now) }
    }

    fun enter(subjectId: String, moduleId: String) {
        lastSubject = subjectId
        lastModule = moduleId
        lastTs = System.currentTimeMillis()
        persist(); syncState()
    }

    fun setPosition(moduleId: String, i: Int) {
        if (positions[moduleId] != i) {
            positions[moduleId] = i
            persist(); syncState()
        }
    }

    private fun fire(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }

    private fun syncState() {
        stateJob?.cancel()
        stateJob = viewModelScope.launch {
            delay(1500)                    // coalesce quick changes (swiping through questions)
            runCatching { api.saveState(positions.toMap(), lastSubject, lastModule) }
        }
    }

    private fun persist() {
        val snapshot = ProgressFile(attempts.toMap(), viewed.toMap(), positions.toMap(), lastSubject, lastModule, lastTs)
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(250)
            if (loggedIn) repo.saveProgress(snapshot)
        }
    }

    // ================================================================== mock tests

    fun mock(id: String?): MockInfo? = mockIndex?.mocks?.firstOrNull { it.id == id }

    suspend fun mockQuestions(mockId: String): Result<List<Question>> {
        mockCache[mockId]?.let { return Result.success(it) }
        return try {
            Result.success(repo.mockQuestions(mockId).also { mockCache[mockId] = it })
        } catch (e: ApiException) {
            if (e.isUnauthorized) notice = "Your session expired. Please sign in again."
            Result.failure(e)
        }
    }

    /** The questions with the answer key filled in; the server only sends the key after the test is submitted. */
    fun withKeys(mockId: String, qs: List<Question>): List<Question> {
        val keys = mockKeys[mockId] ?: return qs
        return qs.map { q -> keys[q.id]?.let { q.copy(answer = it.answer, lo = it.lo, hi = it.hi) } ?: q }
    }

    /** Begins a sitting (restart = true gives a fresh one, e.g. a retake). The clock starts on the server. */
    fun startMock(info: MockInfo, restart: Boolean = false, onDone: () -> Unit) {
        if (mockBusy != null) return
        viewModelScope.launch {
            mockBusy = info.id
            try {
                applyServerSession(api.mockStart(info.id, restart))
                onDone()
            } catch (e: ApiException) {
                notice = e.message
            } finally {
                mockBusy = null
            }
        }
    }

    /** Syncs one mock with the server (when the screen opens, or after the app was closed during a test). */
    suspend fun refreshMock(id: String) {
        if (id in mockDirty) return                       // local changes not sent yet: keep them
        try {
            val s = api.mockSession(id)
            if (s != null) applyServerSession(s) else mockSessions.remove(id)
        } catch (_: ApiException) {
        }
    }

    private fun editMock(id: String, f: (MockSession) -> MockSession) {
        val s = mockSessions[id] ?: return
        if (s.finished) return
        mockSessions[id] = f(s)
        mockDirty += id
        mockSaveJobs[id]?.cancel()
        mockSaveJobs[id] = viewModelScope.launch {
            delay(1200)                                   // answers go to the server a moment after the last change
            flushMock(id)
        }
    }

    private suspend fun flushMock(id: String) {
        val s = mockSessions[id] ?: return
        try {
            api.mockSave(id, s.answers, s.review, s.visited, s.position)
            mockDirty -= id
        } catch (e: ApiException) {
            if (e.status == 409) mockDirty -= id          // the server already closed this sitting
        }
    }

    fun mockAnswer(id: String, qid: String, sel: String?) = editMock(id) {
        it.copy(answers = if (sel.isNullOrBlank()) it.answers - qid else it.answers + (qid to sel))
    }

    fun mockToggleReview(id: String, qid: String) = editMock(id) {
        it.copy(review = if (qid in it.review) it.review - qid else it.review + qid)
    }

    fun mockVisit(id: String, qid: String, position: Int) {
        val s = mockSessions[id] ?: return
        if (s.finished || (qid in s.visited && s.position == position)) return
        editMock(id) { it.copy(visited = it.visited + qid, position = position) }
    }

    /** Sends the answers; the server marks the test and returns the score and the answer key. */
    fun finishMock(id: String, onDone: () -> Unit = {}, onFail: (String) -> Unit = {}) {
        val s = mockSessions[id] ?: return
        if (s.finished) { onDone(); return }
        if (mockBusy != null) return
        viewModelScope.launch {
            mockBusy = id
            try {
                mockSaveJobs[id]?.cancel()
                applyServerSession(api.mockSubmit(id, s.answers))
                mockDirty -= id
                mockAttempts[id] = (mockAttempts[id] ?: 0) + 1
                onDone()
            } catch (e: ApiException) {
                onFail(e.message)
            } finally {
                mockBusy = null
            }
        }
    }

    suspend fun leaderboard(mockId: String): Leaderboard? = try {
        api.leaderboard(mockId)
    } catch (_: ApiException) {
        null
    }

    private fun applyMockSummary(all: Map<String, MockSummary>) {
        all.forEach { (id, sum) ->
            if (id !in mockDirty) {
                if (sum.session != null) applyServerSession(sum.session) else mockSessions.remove(id)
            }
            sum.best?.let { mockBest[id] = it }
            mockAttempts[id] = sum.attempts
        }
    }

    /** The phone's clock may differ from the server's, so times are shifted to the phone's clock. */
    private fun applyServerSession(s: ServerSession) {
        val skew = s.serverNow - System.currentTimeMillis()
        mockSessions[s.mockId] = MockSession(
            mockId = s.mockId, startTs = s.startTs - skew, durationMs = s.durationMs, answers = s.answers, review = s.review,
            visited = s.visited, position = s.position, finished = s.finished, finishTs = if (s.finishTs > 0) s.finishTs - skew else 0L,
            score = s.score, correct = s.correct, wrong = s.wrong, skipped = s.skipped, gaScore = s.gaScore, csScore = s.csScore,
        )
        s.keys?.let { mockKeys[s.mockId] = it }
        if (s.finished) {
            if ((mockBest[s.mockId] ?: Double.NEGATIVE_INFINITY) < s.score && s.score.isFinite()) mockBest[s.mockId] = s.score
        }
    }
}
