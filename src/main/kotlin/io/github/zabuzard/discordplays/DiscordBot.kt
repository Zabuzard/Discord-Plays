package io.github.zabuzard.discordplays

import com.sksamuel.aedile.core.caffeineBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.x.emoji.DiscordEmoji
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.dsl.MenuButtonRowBuilder
import me.jakejmattson.discordkt.extensions.createMenu
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun commands(gameService: GameService) = me.jakejmattson.discordkt.commands.commands("game") {
    slash("game-start", "Start a new game") {
        execute {
            respond("Starting a game")

            gameService.start()
            discord.kord.editPresence {
                playing(gameService.title)
                since = Clock.System.now()
            }

            val displayMessage = channel.createMessage {
                addFile("image.png", image.toInputStream())
            }

            channel.createMenu {
                page { this.description = "Click to play!" }

                buttons {
                    controlButton(Emojis.a, Button.A, gameService)
                    controlButton(Emojis.arrowUp, Button.UP, gameService)
                    controlButton(Emojis.b, Button.B, gameService)
                }
                buttons {
                    controlButton(Emojis.arrowLeft, Button.LEFT, gameService)
                    button("‎", null, disabled = true) {}
                    controlButton(Emojis.arrowRight, Button.RIGHT, gameService)
                }
                buttons {
                    controlButton(Emojis.heavyPlusSign, Button.START, gameService)
                    controlButton(Emojis.arrowDown, Button.DOWN, gameService)
                    controlButton(Emojis.heavyMinusSign, Button.SELECT, gameService)
                }
            }

            coroutineScope {
                while (isActive) {
                    val g = image.createGraphics()
                    gameService.render(g, SCALE)
                    synchronized(lock) {
                        renderOverlay(g)
                    }
                    g.dispose()

                    imageBuffer += image.copy()

                    if (imageBuffer.size >= FLUSH_IMAGE_BUFFER_AT_SIZE) {
                        val gif = imageBuffer.toGif()

                        displayMessage.edit {
                            files?.clear()
                            addFile("image.gif", gif.toInputStream())
                            //content = "```\n${image.toAscii()}\n```"
                        }

                        imageBuffer.clear()
                    }

                    delay(frameCaptureRefreshRate)
                }
            }
        }
    }

    slash("game-stop", "Quitting game") {
        execute {

            respondPublic("Quitting game")
            gameService.stop()

            discord.kord.editPresence {}
        }
    }
}

private fun renderOverlay(g: Graphics2D) {
    g.color = Color.BLACK
    g.fillRect(overlayStartX, 0, INPUT_OVERLAY_WIDTH, image.height)

    g.font = Font("Arial", Font.BOLD, 20)

    val limitedUserInputHistory =
        userInputHistory.asReversed().take(USER_INPUT_HISTORY_MAX_ENTRIES_SHOWN)

    val buttonLabelOffsetX = 15
    val nameOffsetX = 40
    val now = Clock.System.now()
    for ((i, userInput) in limitedUserInputHistory.withIndex()) {
        g.color =
            if (now - userInput.sendAt < userInputHistoryEntryOldAfter) Color.WHITE else Color.GRAY
        val y = 40 + 45 * i

        val buttonLabel = when (userInput.input) {
            Button.A -> "A"
            Button.B -> "B"
            Button.START -> "+"
            Button.SELECT -> "-"
            Button.UP -> "^"
            Button.DOWN -> "v"
            Button.LEFT -> "<"
            Button.RIGHT -> ">"
        }

        g.drawString(buttonLabel, overlayStartX + buttonLabelOffsetX, y)
        g.drawString(
            userInput.userName.take(USER_INPUT_HISTORY_MAX_NAME_LENGTH),
            overlayStartX + nameOffsetX,
            y
        )
    }
}

private fun MenuButtonRowBuilder.controlButton(
    emoji: DiscordEmoji,
    button: Button,
    gameService: GameService
) {
    actionButton(null, emoji) {
        val lastInput = userInputCache.getIfPresent(user.id)

        val now = Clock.System.now()
        val timeSinceLastInput = if (lastInput == null) userInputRateLimit else now - lastInput
        when {
            timeSinceLastInput >= userInputRateLimit -> {
                gameService.clickButton(button)
                deferEphemeralMessageUpdate()

                userInputCache.put(user.id, now)
                synchronized(lock) {
                    userInputHistory += UserInput(user.username, button, Clock.System.now())
                }
            }

            else -> respondEphemeral {
                val timeUntilInputAllowed = userInputRateLimit - timeSinceLastInput
                content = "You click too fast, please wait $timeUntilInputAllowed"
            }
        }
    }
}

private val lock = Object()

private fun sanitizeUserInputHistory() {
    val now = Clock.System.now()
    userInputHistory.removeIf { now - it.sendAt >= userInputHistoryExpiresAfter }
}

private data class UserInput(val userName: String, val input: Button, val sendAt: Instant)

private val userInputCache = caffeineBuilder<Snowflake, Instant> {
    maximumSize = 1_000
    expireAfterWrite = (10).seconds
}.build()
private val userInputRateLimit = (1.5).seconds
private var userInputHistory: MutableList<UserInput> = ArrayDeque()
private val userInputHistoryExpiresAfter = (2).minutes
private const val USER_INPUT_HISTORY_MAX_ENTRIES_SHOWN = 12
private const val USER_INPUT_HISTORY_MAX_NAME_LENGTH = 12
private val userInputHistoryEntryOldAfter = (10).seconds

private val imageBuffer = mutableListOf<BufferedImage>()
private const val FLUSH_IMAGE_BUFFER_AT_SIZE = 30

private val frameCaptureRefreshRate = (150).milliseconds

// GIF plays slower to account for the loading times, that way the experience is
// smoother and does not display the last frame for a longer time
private val gifFrameReplayRefreshRate = (220).milliseconds

private const val INPUT_OVERLAY_WIDTH = 170
private const val SCALE = 4.0//0.28
private val image = BufferedImage(
    (ImageDisplay.RESOLUTION_WIDTH * SCALE).toInt() + INPUT_OVERLAY_WIDTH,
    (ImageDisplay.RESOLUTION_HEIGHT * SCALE).toInt(),
    BufferedImage.TYPE_INT_RGB
)

private val overlayStartX = image.width - INPUT_OVERLAY_WIDTH

private fun List<BufferedImage>.toGif() =
    ByteArrayOutputStream().also {
        val stream = MemoryCacheImageOutputStream(it)

        val gifSequence =
            GifSequenceWriter(
                stream,
                image.type,
                gifFrameReplayRefreshRate.inWholeMilliseconds.toInt()
            )
        forEach(gifSequence::writeToSequence)
        gifSequence.close()
        stream.close()
        it.close()
    }.toByteArray()

private fun ByteArray.toInputStream() =
    ByteArrayInputStream(this)

private fun BufferedImage.copy() =
    BufferedImage(width, height, type).also {
        val g = it.createGraphics()
        g.drawImage(this, 0, 0, null)
        g.dispose()
    }

private fun BufferedImage.toInputStream() =
    ByteArrayOutputStream().also {
        ImageIO.write(this, "png", it)
    }.toByteArray().let(::ByteArrayInputStream)


private fun BufferedImage.toAscii(): String {
    val result = StringBuilder((this.width + 1) * this.height)
    for (y in 0 until this.height) {
        if (result.isNotEmpty()) result.append("\n")
        for (x in 0 until this.width) {
            val pixelColor = Color(this.getRGB(x, y))
            val grayScaleValue =
                pixelColor.red.toDouble() * 0.2989 + pixelColor.blue.toDouble() * 0.5870 + pixelColor.green.toDouble() * 0.1140
            val symbol = grayScaleValue.grayScaleToAscii()
            result.append(symbol)
        }
    }
    return result.toString()
}

private fun Double.grayScaleToAscii() =
    when {
        this >= 230.0 -> ' '
        this >= 200.0 -> '.'
        this >= 180.0 -> '*'
        this >= 160.0 -> ':'
        this >= 130.0 -> 'o'
        this >= 100.0 -> '&'
        this >= 70.0 -> '8'
        this >= 50.0 -> '#'
        else -> '@'
    }
