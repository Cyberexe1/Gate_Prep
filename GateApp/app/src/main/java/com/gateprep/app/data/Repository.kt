package com.gateprep.app.data

import android.content.Context
import com.gateprep.app.net.ApiClient
import com.gateprep.app.net.ApiException
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Where the app gets its data.
 *  - The catalog (subject / module / mock names) is public: live from the server, else the last downloaded copy,
 *    else a small copy bundled in the app, so the home screen always opens.
 *  - Questions and progress need a signed-in user. They come from the server and are cached on the phone for
 *    offline use; that cache is erased when the user signs out.
 */
class Repository(private val context: Context, private val api: ApiClient) {

    private val json get() = api.json
    private val publicDir get() = File(context.filesDir, "public").also { it.mkdirs() }
    private val userDir get() = File(context.filesDir, "user").also { it.mkdirs() }
    private val progressFile get() = File(userDir, "progress.json")

    // ------------------------------------------------------------------ public catalog

    /** Instant, no network: last downloaded copy or the bundled one. */
    fun quickIndex(): IndexFile =
        readCached(File(publicDir, "catalog.json"), IndexFile.serializer()) ?: json.decodeFromString(asset("data/index.json"))

    suspend fun fetchIndex(): IndexFile = api.catalog().also { File(publicDir, "catalog.json").writeText(json.encodeToString(IndexFile.serializer(), it)) }

    fun quickMockIndex(): MockIndexFile =
        readCached(File(publicDir, "mocks.json"), MockIndexFile.serializer()) ?: json.decodeFromString(asset("data/mock/index.json"))

    suspend fun fetchMockIndex(): MockIndexFile = api.mockCatalog().also { File(publicDir, "mocks.json").writeText(json.encodeToString(MockIndexFile.serializer(), it)) }

    // ------------------------------------------------------------------ questions (signed-in users only)

    suspend fun questions(moduleId: String): List<Question> = cachedList(File(userDir, "q_$moduleId.json")) { api.moduleQuestions(moduleId) }

    suspend fun mockQuestions(mockId: String): List<Question> = cachedList(File(userDir, "mock_$mockId.json")) { api.mockQuestions(mockId) }

    private suspend fun cachedList(file: File, fetch: suspend () -> List<Question>): List<Question> {
        val ser = ListSerializer(Question.serializer())
        return try {
            fetch().also { file.writeText(json.encodeToString(ser, it)) }
        } catch (e: ApiException) {
            // no connection: use the copy from the last time, if there is one
            if (e.isOffline && file.exists()) json.decodeFromString(ser, file.readText()) else throw e
        }
    }

    // ------------------------------------------------------------------ local copy of progress (shown instantly, then refreshed from the server)

    fun loadProgress(): ProgressFile =
        runCatching { json.decodeFromString(ProgressFile.serializer(), progressFile.readText()) }.getOrDefault(ProgressFile())

    fun saveProgress(progress: ProgressFile) {
        val tmp = File(userDir, "progress.json.tmp")
        tmp.writeText(json.encodeToString(ProgressFile.serializer(), progress))
        if (!tmp.renameTo(progressFile)) {
            progressFile.writeText(tmp.readText())
            tmp.delete()
        }
    }

    /** Called on sign-out: nothing of the account stays on the phone. */
    fun clearUserData() {
        userDir.deleteRecursively()
    }

    // ------------------------------------------------------------------ helpers

    private fun <T> readCached(file: File, ser: kotlinx.serialization.KSerializer<T>): T? =
        runCatching { json.decodeFromString(ser, file.readText()) }.getOrNull()

    private fun asset(path: String): String = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
