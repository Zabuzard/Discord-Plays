package io.github.zabuzard.discordplays.stream

import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.ChatMessage
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Service
class OverlayRenderer(
    private val config: Config
) {
    private val inputHistory: MutableList<UserInput> = ArrayDeque()
    private val chatHistory: MutableList<ChatMessage> = Collections.synchronizedList(ArrayDeque())

    fun recordUserInput(input: UserInput) {
        inputHistory += input

        removeOldEntries()
    }

    private fun removeOldEntries() {
        val now = Clock.System.now()
        inputHistory.removeIf { now - it.sendAt >= inputHistoryEntryExpiresAfter }
    }

    fun recordChatMessage(message: ChatMessage) {
        chatHistory += message

        if (chatHistory.size > CHAT_HISTORY_MAX_ENTRIES) {
            chatHistory.removeFirst()
        }
    }

    fun renderOverlay(g: Graphics2D, width: Int, height: Int) {
        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)

        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )

        renderInput(g)
        renderChat(g)

        renderTimestamp(g, width, height)
    }

    private fun renderInput(g: Graphics2D) {
        val limitedHistory = inputHistory.asReversed().take(INPUT_HISTORY_MAX_ENTRIES).asReversed()
        val oldBefore = Clock.System.now() - inputHistoryEntryOldAfter

        g.font = inputTextFont
        limitedHistory.withIndex().forEach { (i, userInput) ->
            userInput.render(g, i, oldBefore)
        }
    }

    private fun UserInput.render(g: Graphics2D, i: Int, oldBefore: Instant) {
        val buttonLabel = button.label()
        val displayName = user.username.take(INPUT_NAME_MAX_LENGTH)

        g.color = if (sendAt < oldBefore) Color.GRAY else Color.WHITE
        // Start top left
        val y = INPUT_HISTORY_OFFSET_Y + INPUT_HISTORY_ENTRY_PADDING_Y * i

        val buttonX =
            BUTTON_LABEL_OFFSET_X + ((BUTTON_LABEL_WIDTH - g.lineData(buttonLabel).width) / 2)
        g.drawString(buttonLabel, buttonX, y)
        g.drawString(displayName, INPUT_NAME_OFFSET_X, y)
    }

    private fun renderChat(g: Graphics2D) {
        chatHistory.filterNot { it.author.id in config.bannedUsers }.withIndex()
            .forEach { (i, message) ->
                message.render(g, i)
            }
    }

    private fun ChatMessage.render(g: Graphics2D, i: Int) {
        val displayName = author.username.take(CHAT_NAME_MAX_LENGTH).trim() + ":"
        val displayContent = content.take(CHAT_CONTENT_MAX_LENGTH).trim()

        // Start top left
        g.font = chatNameFont

        val y = CHAT_HISTORY_OFFSET_Y + CHAT_HISTORY_ENTRY_PADDING_Y * i
        val contentX = CHAT_NAME_OFFSET_X + g.lineData(displayName).width + CHAT_CONTENT_OFFSET_X

        g.color = Random(author.id.value.toLong()).let {
            Color(
                it.nextInt(CHAT_NAME_MAX_COLOR_RGB),
                it.nextInt(CHAT_NAME_MAX_COLOR_RGB),
                it.nextInt(CHAT_NAME_MAX_COLOR_RGB)
            )
        }
        g.drawString(displayName, CHAT_NAME_OFFSET_X, y)
        g.font = chatContentFont
        g.color = Color.WHITE
        g.drawString(displayContent, contentX, y)
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

private const val INPUT_HISTORY_OFFSET_Y = 17
private const val INPUT_HISTORY_ENTRY_PADDING_Y = 20
private const val INPUT_HISTORY_MAX_ENTRIES = 12
private const val BUTTON_LABEL_OFFSET_X = 7
private const val BUTTON_LABEL_WIDTH = 15
private const val INPUT_NAME_OFFSET_X = 35
private const val INPUT_NAME_MAX_LENGTH = 18
private val inputTextFont = Font("Arial", Font.BOLD, 13)

private val inputHistoryEntryOldAfter = (10).seconds
private val inputHistoryEntryExpiresAfter = (5).minutes

private const val CHAT_HISTORY_OFFSET_Y = 255
private const val CHAT_HISTORY_ENTRY_PADDING_Y = 10
private const val CHAT_HISTORY_MAX_ENTRIES = 10
private const val CHAT_NAME_OFFSET_X = 5
private const val CHAT_CONTENT_OFFSET_X = 2
private const val CHAT_NAME_MAX_LENGTH = 10
private const val CHAT_CONTENT_MAX_LENGTH = 20
private const val CHAT_NAME_MAX_COLOR_RGB = 150
private val chatNameFont = Font("Arial", Font.BOLD, 10)
private val chatContentFont = Font("Arial", Font.PLAIN, 10)

private const val TIMESTAMP_PADDING = 1
private val timestampFont = Font("Arial", Font.PLAIN, 10)
private val timestampFormat = DateTimeFormatter.ofPattern("yyy-MM-dd hh:mm:ss", Locale.US)

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
