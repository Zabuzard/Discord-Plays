package io.github.zabuzard.discordplays.discord.commands

import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.Extensions.replyEphemeral
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button.secondary
import java.util.*

object InputMenu {
    fun SlashCommandInteractionEvent.createInputMenu() = channel.sendMessageComponents(
        ActionRow.of(
            controlButton(Button.A),
            controlButton(Button.UP),
            controlButton(Button.B)
        ),
        ActionRow.of(
            controlButton(Button.LEFT),
            fakeButton(),
            controlButton(Button.RIGHT)
        ),
        ActionRow.of(
            controlButton(Button.START),
            controlButton(Button.DOWN),
            controlButton(Button.SELECT)
        )
    )

    private fun controlButton(button: Button) = secondary(
        "$CONTROL_BUTTON_ID_PREFIX-${button.name}-${UUID.randomUUID()}",
        button.toEmoji()
    )

    private fun fakeButton() = secondary("fake", INVISIBLE_WHITESPACE).asDisabled()
}

fun JDA.onControlButtonClicked(bot: DiscordBot) {
    addEventListener(object : ListenerAdapter() {
        override fun onButtonInteraction(event: ButtonInteractionEvent) {
            val buttonId = event.button.id ?: ""
            if (!buttonId.startsWith(CONTROL_BUTTON_ID_PREFIX)) {
                return
            }

            val buttonName = buttonId.split("-", limit = 3)[1]
            val button = Button.valueOf(buttonName)

            val userInput = UserInput(event.user, button, Clock.System.now())

            with(event) {
                when (bot.onUserInput(userInput)) {
                    DiscordBot.UserInputResult.ACCEPTED -> deferEdit()
                    DiscordBot.UserInputResult.RATE_LIMITED -> replyEphemeral(
                        "You click too fast, please wait a bit."
                    )

                    DiscordBot.UserInputResult.BLOCKED_NON_OWNER -> replyEphemeral(
                        "User input is currently locked. The game is controlled only by the owners."
                    )

                    DiscordBot.UserInputResult.USER_BANNED -> replyEphemeral(
                        """
                        |Sorry, you have been banned from the event. The game is not accepting your input anymore.
                        |Please get in contact with an owner or host of the event.
                        """.trimMargin()
                    )

                    DiscordBot.UserInputResult.GAME_OFFLINE -> replyEphemeral(
                        "The game is currently offline. Please wait until it is back."
                    )
                }.queue()
            }
        }
    })
}

private const val INVISIBLE_WHITESPACE = "‎"
private const val CONTROL_BUTTON_ID_PREFIX = "discord_plays_input"

private fun Button.toEmoji() =
    when (this) {
        Button.A -> Emoji.fromUnicode("🅰")
        Button.B -> Emoji.fromUnicode("🅱")
        Button.UP -> Emoji.fromUnicode("⬆")
        Button.LEFT -> Emoji.fromUnicode("⬅")
        Button.RIGHT -> Emoji.fromUnicode("➡")
        Button.DOWN -> Emoji.fromUnicode("⬇")
        Button.START -> Emoji.fromUnicode("➕")
        Button.SELECT -> Emoji.fromUnicode("➖")
    }
