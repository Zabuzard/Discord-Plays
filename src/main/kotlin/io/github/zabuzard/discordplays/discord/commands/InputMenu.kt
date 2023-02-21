package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import me.jakejmattson.discordkt.commands.GuildSlashCommandEvent
import me.jakejmattson.discordkt.dsl.listeners
import me.jakejmattson.discordkt.extensions.button
import me.jakejmattson.discordkt.extensions.toPartialEmoji
import me.jakejmattson.discordkt.extensions.uuid

object InputMenu {
    suspend fun GuildSlashCommandEvent<*>.createInputMenu(bot: DiscordBot) =
        channel.createMessage {
            actionRow {
                controlButton(Button.A)
                controlButton(Button.UP)
                controlButton(Button.B)
            }
            actionRow {
                controlButton(Button.LEFT)
                fakeButton()
                controlButton(Button.RIGHT)
            }
            actionRow {
                controlButton(Button.START)
                controlButton(Button.DOWN)
                controlButton(Button.SELECT)
            }
        }

    private fun ActionRowBuilder.controlButton(button: Button) {
        val id = "$CONTROL_BUTTON_ID_PREFIX-${button.name}-${uuid()}"
        interactionButton(ButtonStyle.Secondary, id) {
            emoji = button.toEmoji().toPartialEmoji()
        }
    }

    private fun ActionRowBuilder.fakeButton() =
        button(INVISIBLE_WHITESPACE, null, disabled = true) {}
}

fun onControlButtonClicked(bot: DiscordBot) = listeners {
    on<ButtonInteractionCreateEvent> {
        with(interaction) {
            val buttonId = component.data.customId.value ?: ""
            if (!buttonId.startsWith(CONTROL_BUTTON_ID_PREFIX)) {
                return@on
            }

            val buttonName = buttonId.split("-", limit = 3)[1]
            val button = Button.valueOf(buttonName)

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

private const val INVISIBLE_WHITESPACE = "‎"
private const val CONTROL_BUTTON_ID_PREFIX = "discord_plays_input"

private fun Button.toEmoji() =
    when (this) {
        Button.A -> Emojis.a
        Button.B -> Emojis.b
        Button.UP -> Emojis.arrowUp
        Button.LEFT -> Emojis.arrowLeft
        Button.RIGHT -> Emojis.arrowRight
        Button.DOWN -> Emojis.arrowDown
        Button.START -> Emojis.heavyPlusSign
        Button.SELECT -> Emojis.heavyMinusSign
    }
