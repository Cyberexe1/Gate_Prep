package com.gateprep.app.net

import com.gateprep.app.BuildConfig
import kotlinx.serialization.Serializable

object ApiConfig {
    val base: String = BuildConfig.API_BASE_URL.trimEnd('/')

    /** Images need the signed-in user's token; the shared OkHttp client adds it. folder: q = question, s = solution, f = figure. */
    fun imageUrl(folder: String, name: String) = "$base/api/images/$folder/$name.webp"
}

/** status 0 = the server could not be reached. */
class ApiException(val status: Int, override val message: String) : Exception(message) {
    val isOffline get() = status == 0
    val isUnauthorized get() = status == 401
}

@Serializable
data class AuthUser(val id: String, val email: String, val name: String)

@Serializable
data class AuthResponse(val user: AuthUser, val accessToken: String, val refreshToken: String, val expiresIn: Long = 0)

@Serializable
data class MockKey(val answer: String = "", val lo: Double? = null, val hi: Double? = null)

/** A mock-test sitting as the server sees it. */
@Serializable
data class ServerSession(
    val mockId: String,
    val startTs: Long,
    val durationMs: Long,
    val answers: Map<String, String> = emptyMap(),
    val review: Set<String> = emptySet(),
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
    val serverNow: Long = 0L,
    val keys: Map<String, MockKey>? = null,
)

@Serializable
data class MockSummary(val session: ServerSession? = null, val best: Double? = null, val attempts: Int = 0)

@Serializable
data class LeaderRow(val rank: Int, val name: String, val score: Double, val me: Boolean = false)

@Serializable
data class Leaderboard(val top: List<LeaderRow> = emptyList(), val myRank: Int? = null, val myBest: Double? = null, val players: Int = 0)

@Serializable
internal data class LoginBody(val email: String, val password: String)

@Serializable
internal data class RegisterBody(val email: String, val password: String, val name: String)

@Serializable
internal data class RefreshBody(val refreshToken: String)

@Serializable
internal data class AttemptBody(val questionId: String, val selection: String)

@Serializable
internal data class StateBody(val positions: Map<String, Int>, val lastSubject: String? = null, val lastModule: String? = null)

@Serializable
internal data class SaveBody(val answers: Map<String, String>, val review: Set<String>, val visited: Set<String>, val position: Int)

@Serializable
internal data class SubmitBody(val answers: Map<String, String>)

@Serializable
internal data class ErrorBody(val message: String = "")
