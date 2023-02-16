package io.github.zabuzard.discordplays

import com.sksamuel.aedile.core.caffeineBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ComponentInteraction
import dev.kord.x.emoji.DiscordEmoji
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.dsl.MenuButtonRowBuilder
import me.jakejmattson.discordkt.extensions.createMenu
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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

            val displayMessage = channel.createMessage("display...")

            channel.createMenu {
                page { description = "controls" }

                buttons {
                    controlButton(Emojis.a, ButtonListener.Button.A, gameService)
                    controlButton(Emojis.arrowUp, ButtonListener.Button.UP, gameService)
                    controlButton(Emojis.b, ButtonListener.Button.B, gameService)
                }
                buttons {
                    controlButton(Emojis.arrowLeft, ButtonListener.Button.LEFT, gameService)
                    button("‎", null, disabled = true) {}
                    controlButton(Emojis.arrowRight, ButtonListener.Button.RIGHT, gameService)
                }
                buttons {
                    controlButton(Emojis.heavyPlusSign, ButtonListener.Button.START, gameService)
                    controlButton(Emojis.arrowDown, ButtonListener.Button.DOWN, gameService)
                    controlButton(Emojis.heavyMinusSign, ButtonListener.Button.SELECT, gameService)
                }
            }

            coroutineScope {
                while (isActive) {
                    val g = image.createGraphics()
                    gameService.render(g, SCALE)
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

private fun MenuButtonRowBuilder.controlButton(
    emoji: DiscordEmoji,
    button: ButtonListener.Button,
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
            }

            else -> respondEphemeral {
                val timeUntilInputAllowed = userInputRateLimit - timeSinceLastInput
                content = "You click too fast, please wait $timeUntilInputAllowed"
            }
        }
    }
}

private val userInputCache = caffeineBuilder<Snowflake, Instant> {
    maximumSize = 1_000
    expireAfterWrite = (10).seconds
}.build()
private val userInputRateLimit = (2).seconds

private val imageBuffer = mutableListOf<BufferedImage>()
private const val FLUSH_IMAGE_BUFFER_AT_SIZE = 30

private val frameCaptureRefreshRate = (150).milliseconds

// GIF plays slower to account for the loading times, that way the experience is
// smoother and does not display the last frame for a longer time
private val gifFrameReplayRefreshRate = (220).milliseconds

private const val SCALE = 7.0//0.28
private val image = BufferedImage(
    (ImageDisplay.RESOLUTION_WIDTH * SCALE).toInt(),
    (ImageDisplay.RESOLUTION_HEIGHT * SCALE).toInt(),
    BufferedImage.TYPE_INT_RGB
)

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
