package io.github.zabuzard.discordplays.stream

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints

object BannerRendering {
    enum class Placement {
        TOP,
        CENTER,
        BOTTOM
    }

    fun renderBanner(
        message: String,
        g: Graphics2D,
        fontType: String,
        width: Int,
        screenHeight: Int,
        placement: Placement
    ) {
        g.font = Font(fontType, Font.PLAIN, 20)
        val allLineData = message.chunked(LINE_MAX_LENGTH)
            .take(MAX_LINES)
            .map { g.lineData(it) }

        g.color = Color(0, 0, 0, 200)
        val bannerHeight = BANNER_PADDING * 2 +
            allLineData.first().height +
            allLineData.drop(1).sumOf { it.height + it.leading }
        val topMargin = when (placement) {
            Placement.TOP -> 0
            Placement.CENTER -> (screenHeight - bannerHeight) / 2
            Placement.BOTTOM -> screenHeight - bannerHeight
        }
        g.fillRect(0, topMargin, width, bannerHeight)

        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )
        g.color = Color.WHITE
        var lineTopY = topMargin + BANNER_PADDING - allLineData.first().leading
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
