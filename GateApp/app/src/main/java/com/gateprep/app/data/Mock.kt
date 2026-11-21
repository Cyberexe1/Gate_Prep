package com.gateprep.app.data

import kotlinx.serialization.Serializable

@Serializable
data class MockIndexFile(val mocks: List<MockInfo> = emptyList())

@Serializable
data class MockSection(val name: String, val short: String, val from: Int, val to: Int, val marks: Int)

@Serializable
data class MockInfo(
    val id: String,
    val n: Int,
    val title: String,
    val questions: Int,
    val marks: Int,
    val minutes: Int,
    val c1: String,
    val c2: String,
    val emoji: String,
    val sections: List<MockSection> = emptyList(),
    val nat: Int = 0,
)

/** One sitting of a mock test. The clock runs on wall time: remaining = start + duration - now. */
@Serializable
data class MockSession(
    val mockId: String,
    val startTs: Long,
    val durationMs: Long,
    val answers: Map<String, String> = emptyMap(),      // question id -> "B" or "4.5"
    val review: Set<String> = emptySet(),               // marked for review
    val visited: Set<String> = emptySet(),
    val position: Int = 0,
    val finished: Boolean = false,
    val finishTs: Long = 0L,
    val score: Double = 0.0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val skipped: Int = 0,
    val gaScore: Double = 0.0,
    val csScore: Double = 0.0,
) {
    fun remainingMs(now: Long): Long = (startTs + durationMs - now).coerceAtLeast(0L)
}

@Serializable
data class MockProgressFile(
    val sessions: Map<String, MockSession> = emptyMap(),   // latest session per mock
    val best: Map<String, Double> = emptyMap(),
    val attempts: Map<String, Int> = emptyMap(),           // finished attempts per mock
)

/** GATE marking: +marks if right; MCQ wrong costs 1/3 of the marks; NAT wrong or unanswered costs nothing. */
fun markFor(q: Question, selection: String?): Double {
    if (selection.isNullOrBlank()) return 0.0
    return when {
        q.check(selection) -> q.marks.toDouble()
        q.type == "MCQ" -> -q.marks / 3.0
        else -> 0.0
    }
}
