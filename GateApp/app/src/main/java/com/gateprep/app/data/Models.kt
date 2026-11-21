package com.gateprep.app.data

import kotlinx.serialization.Serializable

@Serializable
data class IndexFile(val subjects: List<SubjectInfo> = emptyList())

@Serializable
data class SubjectInfo(
    val id: String,
    val name: String,
    val short: String,
    val emoji: String,
    val c1: String,
    val c2: String,
    val count: Int,
    val modules: List<ModuleInfo> = emptyList(),
)

@Serializable
data class ModuleInfo(val id: String, val name: String, val count: Int)

/** One question. The question and its solution are images (assets/images/q/<id>.webp and /s/<id>.webp). */
@Serializable
data class Question(
    val id: String,
    val year: Int = 0,
    val label: String = "",
    val qno: String = "",
    val marks: Int = 0,
    val type: String = "",          // MCQ | MSQ | NAT | "" (no answer key)
    val answer: String = "",        // "B" | "A,C" | "42"
    val lo: Double? = null,         // NAT accepted range
    val hi: Double? = null,
    val raw: String = "",
    val exam: String = "",
    val src: String = "",           // paper | book
    val hasSol: Boolean = false,
    val mode: String = "image",     // "text" = stem/options below are used, "image" = show the original question image
    val stem: List<Block> = emptyList(),
    val options: List<String> = emptyList(),
) {
    val isText: Boolean get() = mode == "text" && stem.isNotEmpty()

    val hasKey: Boolean get() = answer.isNotBlank() && type.isNotBlank()

    /** Is [selection] ("B", "A,C" or "4.5") the right answer? */
    fun check(selection: String): Boolean {
        if (!hasKey) return false
        return when (type) {
            "MCQ", "MSQ" -> {
                val picked = selection.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                picked == answer.split(",").map { it.trim() }.toSet()
            }
            "NAT" -> {
                val v = selection.trim().toDoubleOrNull() ?: return false
                val low = lo ?: return false
                val high = hi ?: low
                v >= low - 1e-6 && v <= high + 1e-6
            }
            else -> false
        }
    }

    /** Human readable correct answer. */
    val displayAnswer: String
        get() = if (type == "MSQ") answer.replace(",", ", ") else answer
}

/**
 * One piece of a question stem.
 * k = "p" paragraph | "code" monospaced block | "fig" figure image (src = file name in assets/images/f)
 * Text may contain ^{superscript} and _{subscript} markup.
 */
@Serializable
data class Block(val k: String, val t: String = "", val src: String = "")

/** A stored attempt. [m] is the id of the module the question was practised in. */
@Serializable
data class AttemptRec(val sel: String, val ok: Boolean, val ts: Long, val m: String)

@Serializable
data class ProgressFile(
    val attempts: Map<String, AttemptRec> = emptyMap(),
    val viewed: Map<String, String> = emptyMap(),          // question id -> module id (questions with no answer key)
    val positions: Map<String, Int> = emptyMap(),          // module id -> last question index
    val lastSubject: String? = null,
    val lastModule: String? = null,
    val lastTs: Long = 0L,
)
