package com.gateprep.app.ui.practice

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em

/**
 * Turns "T_{1}(n^{2}) = 4T(n/2)" into styled text: ^{..} becomes a superscript, _{..} a subscript.
 * Scripts can be nested ("n^{log_{2}3}").
 */
fun markup(text: String): AnnotatedString = buildAnnotatedString { parse(text, 0, text.length) }

private fun androidx.compose.ui.text.AnnotatedString.Builder.parse(s: String, from: Int, to: Int) {
    var i = from
    while (i < to) {
        val c = s[i]
        if ((c == '^' || c == '_') && i + 1 < to && s[i + 1] == '{') {
            val close = matching(s, i + 1, to)
            if (close > 0) {
                val style = SpanStyle(
                    baselineShift = if (c == '^') BaselineShift.Superscript else BaselineShift.Subscript,
                    fontSize = 0.78.em,
                )
                withStyle(style) { parse(s, i + 2, close) }
                i = close + 1
                continue
            }
        }
        append(c)
        i++
    }
}

/** index of the '}' that closes the '{' at [open], or -1 */
private fun matching(s: String, open: Int, to: Int): Int {
    var depth = 0
    for (j in open until to) {
        if (s[j] == '{') depth++
        else if (s[j] == '}') {
            depth--
            if (depth == 0) return j
        }
    }
    return -1
}
