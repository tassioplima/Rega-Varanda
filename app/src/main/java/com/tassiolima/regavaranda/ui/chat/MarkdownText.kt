package com.tassiolima.regavaranda.ui.chat

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val MARKDOWN_PATTERN = Regex("\\*\\*(.+?)\\*\\*|`(.+?)`|\\*(.+?)\\*|_(.+?)_")
private val HEADING_PATTERN = Regex("^(#{1,6})\\s+(.*)$")

// Conversor leve de markdown (negrito, itálico, código, listas, títulos "#") para
// AnnotatedString, para as respostas da IA no chat não aparecerem com símbolos literais.
fun parseSimpleMarkdown(raw: String): AnnotatedString {
    val withBullets = raw.lineSequence().joinToString("\n") { line ->
        val indent = line.takeWhile { it == ' ' }
        val trimmed = line.trimStart()
        val headingMatch = HEADING_PATTERN.find(trimmed)
        when {
            headingMatch != null -> "$indent**${headingMatch.groupValues[2]}**"
            trimmed.startsWith("- ") -> "$indent• ${trimmed.removePrefix("- ")}"
            trimmed.startsWith("* ") -> "$indent• ${trimmed.removePrefix("* ")}"
            else -> line
        }
    }

    return buildAnnotatedString {
        var lastIndex = 0
        for (match in MARKDOWN_PATTERN.findAll(withBullets)) {
            append(withBullets.substring(lastIndex, match.range.first))
            val bold = match.groups[1]?.value
            val code = match.groups[2]?.value
            val italic = match.groups[3]?.value ?: match.groups[4]?.value
            when {
                bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                code != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(code) }
                italic != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
            }
            lastIndex = match.range.last + 1
        }
        append(withBullets.substring(lastIndex))
    }
}
