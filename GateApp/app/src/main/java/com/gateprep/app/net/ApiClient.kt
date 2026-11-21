package com.gateprep.app.net

import com.gateprep.app.data.IndexFile
import com.gateprep.app.data.MockIndexFile
import com.gateprep.app.data.ProgressFile
import com.gateprep.app.data.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * All calls to the Spring Boot backend.
 * The OkHttp client adds the access token to every request and, on a 401, trades the refresh token for a new
 * access token once and retries. If the refresh token is no longer valid the user is signed out.
 */
class ApiClient(private val session: SessionStore) {

    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true; explicitNulls = false }

    private val apiHost = ApiConfig.base.toHttpUrl().host
    private val lock = Any()

    private val bare: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).writeTimeout(25, TimeUnit.SECONDS).build()

    /** Shared with the image loader, so images are fetched with the token as well. */
    val http: OkHttpClient = bare.newBuilder()
        .addInterceptor(Interceptor { chain ->
            val req = chain.request()
            val token = session.accessToken
            if (token != null && req.url.host == apiHost && req.header("Authorization") == null) {
                chain.proceed(req.newBuilder().header("Authorization", "Bearer $token").build())
            } else chain.proceed(req)
        })
        .authenticator(Authenticator { _, response -> reauthenticate(response) })
        .build()

    private fun reauthenticate(response: Response): Request? {
        val req = response.request
        if (req.url.encodedPath.startsWith("/api/auth/")) return null           // a wrong password is not an expired session
        val sent = req.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (responseCount(response) >= 2) return null
        synchronized(lock) {
            val current = session.accessToken
            if (current != null && current != sent) {                           // another request already refreshed
                return req.newBuilder().header("Authorization", "Bearer $current").build()
            }
            val refresh = session.refreshToken ?: return null
            return try {
                val call = bare.newCall(post("/api/auth/refresh", json.encodeToString(RefreshBody(refresh))))
                call.execute().use { r ->
                    if (r.isSuccessful) {
                        val auth = json.decodeFromString<AuthResponse>(r.body!!.string())
                        session.save(auth)
                        req.newBuilder().header("Authorization", "Bearer ${auth.accessToken}").build()
                    } else {
                        if (r.code == 401 || r.code == 400) session.clear()     // refresh token expired, revoked or re-used
                        null
                    }
                }
            } catch (e: IOException) {
                null                                                            // offline: keep the session, fail this call
            }
        }
    }

    private fun responseCount(r: Response): Int {
        var n = 1
        var p = r.priorResponse
        while (p != null) { n++; p = p.priorResponse }
        return n
    }

    // ------------------------------------------------------------------ plumbing

    private fun post(path: String, body: String?) =
        Request.Builder().url(ApiConfig.base + path).post((body ?: "").toRequestBody("application/json".toMediaType())).build()

    private suspend fun execute(req: Request): String? = withContext(Dispatchers.IO) {
        try {
            http.newCall(req).execute().use { r ->
                val text = r.body?.string().orEmpty()
                when {
                    r.code == 204 -> null
                    r.isSuccessful -> text
                    else -> throw ApiException(r.code, errorText(r.code, text))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw ApiException(0, "Can't reach the server. Check your internet connection.")
        }
    }

    private fun errorText(code: Int, body: String): String {
        val msg = runCatching { json.decodeFromString<ErrorBody>(body).message }.getOrNull().orEmpty()
        return when {
            msg.isNotBlank() -> msg
            code == 401 -> "Please sign in again."
            code == 429 -> "Too many attempts. Try again in a few minutes."
            code >= 500 -> "Server problem. Please try again."
            else -> "Request failed ($code)."
        }
    }

    private fun url(path: String) = ApiConfig.base + path

    private suspend fun get(path: String) = execute(Request.Builder().url(url(path)).get().build())
    private suspend fun send(method: String, path: String, body: String?) =
        execute(Request.Builder().url(url(path)).method(method, body?.toRequestBody("application/json".toMediaType())).build())

    private suspend inline fun <reified T> getAs(path: String): T = json.decodeFromString(serializer<T>(), get(path) ?: throw ApiException(204, "Empty response"))
    private suspend inline fun <reified T> sendAs(method: String, path: String, body: String? = ""): T =
        json.decodeFromString(serializer<T>(), send(method, path, body) ?: throw ApiException(204, "Empty response"))

    // ------------------------------------------------------------------ auth

    suspend fun login(email: String, password: String): AuthResponse =
        sendAs<AuthResponse>("POST", "/api/auth/login", json.encodeToString(LoginBody(email, password))).also { session.save(it) }

    suspend fun register(name: String, email: String, password: String): AuthResponse =
        sendAs<AuthResponse>("POST", "/api/auth/register", json.encodeToString(RegisterBody(email, password, name))).also { session.save(it) }

    suspend fun logout() {
        val refresh = session.refreshToken
        session.clear()
        if (refresh != null) runCatching { send("POST", "/api/auth/logout", json.encodeToString(RefreshBody(refresh))) }
    }

    // ------------------------------------------------------------------ content

    suspend fun catalog(): IndexFile = getAs("/api/catalog")
    suspend fun mockCatalog(): MockIndexFile = getAs("/api/catalog/mocks")
    suspend fun moduleQuestions(moduleId: String): List<Question> = getAs("/api/modules/$moduleId/questions")
    suspend fun mockQuestions(mockId: String): List<Question> = getAs("/api/mocks/$mockId/questions")

    // ------------------------------------------------------------------ practice progress

    suspend fun progress(): ProgressFile = getAs("/api/progress")
    suspend fun attempt(questionId: String, selection: String) {
        send("POST", "/api/progress/attempts", json.encodeToString(AttemptBody(questionId, selection)))
    }
    suspend fun clearAttempt(questionId: String) { send("DELETE", "/api/progress/attempts/$questionId", null) }
    suspend fun setViewed(questionId: String, value: Boolean) { send("PUT", "/api/progress/viewed/$questionId?value=$value", "") }
    suspend fun saveState(positions: Map<String, Int>, lastSubject: String?, lastModule: String?) {
        send("PUT", "/api/progress/state", json.encodeToString(StateBody(positions, lastSubject, lastModule)))
    }

    // ------------------------------------------------------------------ mock tests

    suspend fun mockSummary(): Map<String, MockSummary> = getAs("/api/mock-summary")
    suspend fun mockSession(mockId: String): ServerSession? = get("/api/mocks/$mockId/session")?.let { json.decodeFromString(it) }
    suspend fun mockStart(mockId: String, restart: Boolean): ServerSession = sendAs("POST", "/api/mocks/$mockId/start?restart=$restart")
    suspend fun mockSave(mockId: String, answers: Map<String, String>, review: Set<String>, visited: Set<String>, position: Int) {
        send("PUT", "/api/mocks/$mockId/session", json.encodeToString(SaveBody(answers, review, visited, position)))
    }
    suspend fun mockSubmit(mockId: String, answers: Map<String, String>): ServerSession =
        sendAs("POST", "/api/mocks/$mockId/submit", json.encodeToString(SubmitBody(answers)))
    suspend fun leaderboard(mockId: String): Leaderboard = getAs("/api/mocks/$mockId/leaderboard")
}
