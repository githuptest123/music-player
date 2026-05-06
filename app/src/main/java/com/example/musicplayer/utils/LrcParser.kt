package com.example.musicplayer.utils

object LrcParser {
    data class LyricLine(val timeMs: Long, val text: String)

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        lrcContent.lines().forEach { line ->
            regex.find(line)?.let { match ->
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val frac = match.groupValues[3].padEnd(3, '0').take(3).toLong()
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(min * 60_000 + sec * 1000 + frac, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
