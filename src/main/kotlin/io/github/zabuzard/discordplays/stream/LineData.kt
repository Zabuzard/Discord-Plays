package io.github.zabuzard.discordplays.stream

import java.awt.Graphics2D

data class LineData(
    val line: String,
    val width: Int,
    val height: Int,
    val ascent: Int,
    val leading: Int
)

fun Graphics2D.lineData(line: String) =
    fontMetrics.getLineMetrics(line, this).let {
        LineData(
            line,
            fontMetrics.stringWidth(line),
            it.height.toInt(),
            it.ascent.toInt(),
            it.leading.toInt()
        )
    }
