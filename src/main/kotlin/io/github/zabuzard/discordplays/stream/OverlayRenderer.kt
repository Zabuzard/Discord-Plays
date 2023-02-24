package io.github.zabuzard.discordplays.stream

import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import me.jakejmattson.discordkt.annotations.Service
import org.checkerframework.checker.units.qual.g
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Service
class OverlayRenderer {
    private val history: MutableList<UserInput> = ArrayDeque()

    fun recordUserInput(input: UserInput) {
        history += input

        removeOldEntries()
    }

    private fun removeOldEntries() {
        val now = Clock.System.now()
        history.removeIf { now - it.sendAt >= historyEntryExpiresAfter }
    }

    fun renderOverlay(g: Graphics2D, width: Int, height: Int) {
        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)

        val limitedHistory = history.asReversed().take(HISTORY_MAX_ENTRIES).asReversed()
        val oldBefore = Clock.System.now() - historyEntryOldAfter

        g.font = textFont
        limitedHistory.withIndex().forEach { (i, userInput) ->
            userInput.render(g, i, oldBefore)
        }

        renderTimestamp(g, width, height)
    }

    private fun UserInput.render(g: Graphics2D, i: Int, oldBefore: Instant) {
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )

        val buttonLabel = button.label()
        val displayName = user.username.take(NAME_MAX_LENGTH)

        g.color = if (sendAt < oldBefore) Color.GRAY else Color.WHITE
        // Start top left
        val y = HISTORY_OFFSET_Y + HISTORY_ENTRY_PADDING_Y * i

        val buttonX =
            BUTTON_LABEL_OFFSET_X + ((BUTTON_LABEL_WIDTH - g.lineData(buttonLabel).width) / 2)
        g.drawString(buttonLabel, buttonX, y)
        g.drawString(displayName, NAME_OFFSET_X, y)
    }

    private fun renderTimestamp(g: Graphics2D, width: Int, height: Int) {
        g.color = Color.WHITE
        g.font = timestampFont

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime()
            .let { timestampFormat.format(it) }

        // Bottom right
        val lineData = g.lineData(now)
        val x = width - lineData.width - TIMESTAMP_PADDING
        val y = height - lineData.height + lineData.ascent - TIMESTAMP_PADDING

        g.drawString(now, x, y)
    }
}

private const val HISTORY_OFFSET_Y = 20
private const val HISTORY_ENTRY_PADDING_Y = 25
private const val HISTORY_MAX_ENTRIES = 13
private const val BUTTON_LABEL_OFFSET_X = 7
private const val BUTTON_LABEL_WIDTH = 15
private const val NAME_OFFSET_X = 35
private const val NAME_MAX_LENGTH = 15
private val textFont = Font("Arial", Font.BOLD, 15)

private const val TIMESTAMP_PADDING = 5
private val timestampFont = Font("Arial", Font.PLAIN, 12)
private val timestampFormat = DateTimeFormatter.ofPattern("yyy-MM-dd hh:mm:ss", Locale.US)

private val historyEntryOldAfter = (10).seconds
private val historyEntryExpiresAfter = (2).minutes

private fun Button.label() = when (this) {
    Button.A -> "A"
    Button.B -> "B"
    Button.START -> "+"
    Button.SELECT -> "–"
    Button.UP -> "▲"
    Button.DOWN -> "▼"
    Button.LEFT -> "◄"
    Button.RIGHT -> "►"
}
