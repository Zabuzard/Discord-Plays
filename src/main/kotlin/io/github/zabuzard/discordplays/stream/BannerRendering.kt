package io.github.zabuzard.discordplays.stream

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

object BannerRendering {
    fun renderBanner(message: String, g: Graphics2D, width: Int) {
        g.font = Font("Arial", Font.PLAIN, 20)
        val allLineData = message.chunked(LINE_MAX_LENGTH)
            .take(MAX_LINES)
            .map { g.lineData(it) }

        g.color = Color(0, 0, 0, 200)
        val bannerHeight = BANNER_PADDING * 2 +
            allLineData.first().height +
            allLineData.drop(1).sumOf { it.height + it.leading }
        g.fillRect(0, 0, width, bannerHeight)

        g.color = Color.WHITE
        var lineTopY = BANNER_PADDING - allLineData.first().leading
        allLineData.forEach {
            lineTopY += it.leading
            val lineBottomY = lineTopY + it.height
            val lineHeight = lineBottomY - lineTopY

            val lineStartX = (width - it.width) / 2
            val lineBaselineY = lineTopY + ((lineHeight - it.height) / 2) + it.ascent
            g.drawString(it.line, lineStartX, lineBaselineY)

            lineTopY = lineBottomY
        }
    }
}

private const val BANNER_PADDING = 10
private const val LINE_MAX_LENGTH = 50
private const val MAX_LINES = 4

private data class LineData(
    val line: String,
    val width: Int,
    val height: Int,
    val ascent: Int,
    val leading: Int
)

private fun Graphics2D.lineData(line: String) =
    fontMetrics.getLineMetrics(line, this).let {
        LineData(
            line,
            fontMetrics.stringWidth(line),
            it.height.toInt(),
            it.ascent.toInt(),
            it.leading.toInt()
        )
    }
