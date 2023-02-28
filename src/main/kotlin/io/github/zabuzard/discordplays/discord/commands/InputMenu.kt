package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.toReaction
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import java.util.*

object InputMenu {
    suspend fun GuildChatInputCommandInteraction.createInputMenu(bot: DiscordBot) =
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
        val id = "$CONTROL_BUTTON_ID_PREFIX-${button.name}-${UUID.randomUUID()}"
        interactionButton(ButtonStyle.Secondary, id) {
            emoji = DiscordPartialEmoji(name = button.toEmoji().toReaction().name)
        }
    }

    private fun ActionRowBuilder.fakeButton() =
        interactionButton(ButtonStyle.Secondary, "fake") {
            label = INVISIBLE_WHITESPACE
            disabled = true
        }
}

fun Kord.onControlButtonClicked(bot: DiscordBot) {
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
