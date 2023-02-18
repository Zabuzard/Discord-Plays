package io.github.zabuzard.discordplays.stream

import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
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

        val limitedHistory = history.asReversed().take(HISTORY_MAX_ENTRIES)
        val oldBefore = Clock.System.now() - historyEntryOldAfter

        g.font = Font("Arial", Font.BOLD, 20)
        limitedHistory.withIndex().forEach { (i, userInput) ->
            userInput.render(g, i, oldBefore)
        }
    }

    private fun UserInput.render(g: Graphics2D, i: Int, oldBefore: Instant) {
        val buttonLabel = button.label()
        val displayName = user.username.take(NAME_MAX_LENGTH)

        g.color = if (sendAt < oldBefore) Color.GRAY else Color.WHITE
        val y = HISTORY_OFFSET_Y + HISTORY_ENTRY_PADDING_Y * i

        g.drawString(buttonLabel, BUTTON_LABEL_OFFSET_X, y)
        g.drawString(displayName, NAME_OFFSET_X, y)
    }
}

private const val HISTORY_OFFSET_Y = 40
private const val HISTORY_ENTRY_PADDING_Y = 45
private const val HISTORY_MAX_ENTRIES = 12
private const val BUTTON_LABEL_OFFSET_X = 10
private const val NAME_OFFSET_X = 35
private const val NAME_MAX_LENGTH = 12

private val historyEntryOldAfter = (10).seconds
private val historyEntryExpiresAfter = (2).minutes

private fun Button.label() = when (this) {
    Button.A -> "A"
    Button.B -> "B"
    Button.START -> "+"
    Button.SELECT -> "-"
    Button.UP -> "^"
    Button.DOWN -> "v"
    Button.LEFT -> "<"
    Button.RIGHT -> ">"
}
