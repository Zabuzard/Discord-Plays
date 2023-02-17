package io.github.zabuzard.discordplays.discord.commands

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.x.emoji.DiscordEmoji
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import me.jakejmattson.discordkt.commands.GuildSlashCommandEvent
import me.jakejmattson.discordkt.commands.subcommand
import me.jakejmattson.discordkt.dsl.MenuButtonRowBuilder
import me.jakejmattson.discordkt.extensions.createMenu

fun hostCommands(
    bot: DiscordBot
) = subcommand("host") {
    sub("stream", "Starts your game stream in this channel") {
        execute {
            respond(
                """
                Starting to host the stream in this channel.
                To stop the stream, just delete the message that contains it.
                """.trimIndent()
            )

            createStreamMessage(bot)
            createInputMenu(bot)
        }
    }
}

private suspend fun GuildSlashCommandEvent<*>.createStreamMessage(bot: DiscordBot) {
    val streamMessage = channel.createMessage {
        addFile("stream.png", javaClass.getResourceAsStream("/starting_soon.png")!!)
    }
    bot.addStreamTarget(streamMessage)
}

private suspend fun GuildSlashCommandEvent<*>.createInputMenu(bot: DiscordBot) {
    channel.createMenu {
        page { this.description = "Click to play!" }

        buttons {
            controlButton(Emojis.a, ButtonListener.Button.A, bot)
            controlButton(Emojis.arrowUp, ButtonListener.Button.UP, bot)
            controlButton(Emojis.b, ButtonListener.Button.B, bot)
        }
        buttons {
            controlButton(Emojis.arrowLeft, ButtonListener.Button.LEFT, bot)
            button("‎", null, disabled = true) {}
            controlButton(Emojis.arrowRight, ButtonListener.Button.RIGHT, bot)
        }
        buttons {
            controlButton(Emojis.heavyPlusSign, ButtonListener.Button.START, bot)
            controlButton(Emojis.arrowDown, ButtonListener.Button.DOWN, bot)
            controlButton(Emojis.heavyMinusSign, ButtonListener.Button.SELECT, bot)
        }
    }
}

private fun MenuButtonRowBuilder.controlButton(
    emoji: DiscordEmoji,
    button: ButtonListener.Button,
    bot: DiscordBot
) {
    actionButton(null, emoji) {
        val userInput = UserInput(user, button, Clock.System.now())

        when (bot.onUserInput(userInput)) {
            DiscordBot.UserInputResult.ACCEPTED -> deferEphemeralMessageUpdate()
            DiscordBot.UserInputResult.RATE_LIMITED -> respondEphemeral {
                content = "You click too fast, please wait a bit."
            }
        }
    }
}
