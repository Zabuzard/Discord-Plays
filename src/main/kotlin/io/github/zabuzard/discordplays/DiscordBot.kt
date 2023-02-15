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
                    editButton(null, Emojis.arrowUp) {
                        displayMessage.edit { content = "you pressed up" }
                        gameService.clickButton(ButtonListener.Button.UP)
                    }
                    editButton(null, Emojis.arrowLeft) {
                        displayMessage.edit { content = "you pressed left" }
                        gameService.clickButton(ButtonListener.Button.LEFT)
                    }
                    editButton(null, Emojis.arrowRight) {
                        displayMessage.edit { content = "you pressed right" }
                        gameService.clickButton(ButtonListener.Button.RIGHT)
                    }
                    editButton(null, Emojis.arrowDown) {
                        displayMessage.edit { content = "you pressed down" }
                        gameService.clickButton(ButtonListener.Button.DOWN)
                    }
                }
                buttons {
                    editButton(null, Emojis.a) {
                        displayMessage.edit { content = "you pressed a" }
                        gameService.clickButton(ButtonListener.Button.A)
                    }
                    editButton(null, Emojis.b) {
                        displayMessage.edit { content = "you pressed b" }
                        gameService.clickButton(ButtonListener.Button.B)
                    }
                    editButton("start", null) {
                        displayMessage.edit { content = "you pressed start" }
                        gameService.clickButton(ButtonListener.Button.START)
                    }
                    editButton("select", null) {
                        displayMessage.edit { content = "you pressed select" }
                        gameService.clickButton(ButtonListener.Button.SELECT)
                    }
                }
            }
        }
    }
}
