package io.github.zabuzard.discordplays

import dev.kord.core.behavior.edit
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener
import me.jakejmattson.discordkt.extensions.createMenu

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
                        displayMessage.edit { content = "you pressed a" }
                        gameService.clickButton(ButtonListener.Button.A)
                    }
                    button(null, Emojis.arrowUp) {
                        displayMessage.edit { content = "you pressed up" }
                        gameService.clickButton(ButtonListener.Button.UP)
                    }
                    button(null, Emojis.b) {
                        displayMessage.edit { content = "you pressed b" }
                        gameService.clickButton(ButtonListener.Button.B)
                    }
                }
                buttons {
                    button(null, Emojis.arrowLeft) {
                        displayMessage.edit { content = "you pressed left" }
                        gameService.clickButton(ButtonListener.Button.LEFT)
                    }
                    button("‎", null, disabled = true) {}
                    button(null, Emojis.arrowRight) {
                        displayMessage.edit { content = "you pressed right" }
                        gameService.clickButton(ButtonListener.Button.RIGHT)
                    }
                }
                buttons {
                    button(null, Emojis.heavyPlusSign) {
                        displayMessage.edit { content = "you pressed start" }
                        gameService.clickButton(ButtonListener.Button.START)
                    }
                    button(null, Emojis.arrowDown) {
                        displayMessage.edit { content = "you pressed down" }
                        gameService.clickButton(ButtonListener.Button.DOWN)
                    }
                    button(null, Emojis.heavyMinusSign) {
                        displayMessage.edit { content = "you pressed select" }
                        gameService.clickButton(ButtonListener.Button.SELECT)
                    }
                }
            }
        }
    }
}
