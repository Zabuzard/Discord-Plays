package io.github.zabuzard.discordplays.discord.commands

import dev.kord.core.behavior.channel.createMessage
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.commands.InputMenu.createInputMenu
import me.jakejmattson.discordkt.commands.GuildSlashCommandEvent
import me.jakejmattson.discordkt.commands.subcommand

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
