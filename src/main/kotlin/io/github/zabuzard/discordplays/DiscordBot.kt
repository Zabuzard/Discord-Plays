package io.github.zabuzard.discordplays

import dev.kord.core.behavior.edit
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.jakejmattson.discordkt.extensions.createMenu
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

fun commands(gameService: GameService) = me.jakejmattson.discordkt.commands.commands("game") {
    slash("game-start", "Start a new game") {
        execute {
            respond("Starting a game")

            gameService.start()

            val displayMessage = channel.createMessage("display...")

            channel.createMenu {
                page { description = "controls" }

                buttons {
                    button(null, Emojis.a) {
                        gameService.clickButton(ButtonListener.Button.A)
                    }
                    button(null, Emojis.arrowUp) {
                        gameService.clickButton(ButtonListener.Button.UP)
                    }
                    button(null, Emojis.b) {
                        gameService.clickButton(ButtonListener.Button.B)
                    }
                }
                buttons {
                    button(null, Emojis.arrowLeft) {
                        gameService.clickButton(ButtonListener.Button.LEFT)
                    }
                    button("‎", null, disabled = true) {}
                    button(null, Emojis.arrowRight) {
                        gameService.clickButton(ButtonListener.Button.RIGHT)
                    }
                }
                buttons {
                    button(null, Emojis.heavyPlusSign) {
                        gameService.clickButton(ButtonListener.Button.START)
                    }
                    button(null, Emojis.arrowDown) {
                        gameService.clickButton(ButtonListener.Button.DOWN)
                    }
                    button(null, Emojis.heavyMinusSign) {
                        gameService.clickButton(ButtonListener.Button.SELECT)
                    }
                }
            }

            coroutineScope {
                while (isActive) {
                    val g = image.createGraphics()
                    gameService.render(g, SCALE)
                    g.dispose()

                    displayMessage.edit {
                        files?.clear()
                        addFile("image.$IMAGE_FORMAT", image.toInputStream())
                        //content = "```\n${image.toAscii()}\n```"
                    }

                    delay(refreshRate)
                }
            }
        }
    }
}

private val refreshRate = (3).seconds//(1).seconds

private const val SCALE = 7.0//0.28
private val image = BufferedImage(
    (ImageDisplay.RESOLUTION_WIDTH * SCALE).toInt(),
    (ImageDisplay.RESOLUTION_HEIGHT * SCALE).toInt(),
    BufferedImage.TYPE_INT_RGB
)

private const val IMAGE_FORMAT = "png"

private fun BufferedImage.toInputStream() =
    ByteArrayOutputStream().also {
        ImageIO.write(this, IMAGE_FORMAT, it)
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
