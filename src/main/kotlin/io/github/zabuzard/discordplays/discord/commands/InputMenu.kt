package io.github.zabuzard.discordplays.discord.commands

import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.x.emoji.DiscordEmoji
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import me.jakejmattson.discordkt.commands.GuildSlashCommandEvent
import me.jakejmattson.discordkt.dsl.MenuButtonRowBuilder
import me.jakejmattson.discordkt.extensions.createMenu

object InputMenu {
    suspend fun GuildSlashCommandEvent<*>.createInputMenu(bot: DiscordBot) =
        channel.createMenu {
            page { description = "Click to play!" }

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

                DiscordBot.UserInputResult.BLOCKED_NON_OWNER -> respondEphemeral {
                    content =
                        "User input is currently locked. The game is controlled only by the owners."
                }

                DiscordBot.UserInputResult.USER_BANNED -> respondEphemeral {
                    content = """
                        Sorry, you have been banned from the event. The game is not accepting your input anymore.
                        Please get in contact with an owner or host of the event.
                    """.trimIndent()
                }
            }
        }
    }
}
