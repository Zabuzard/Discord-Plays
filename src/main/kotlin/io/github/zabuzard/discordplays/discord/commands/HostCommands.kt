package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.Permission.ModerateMembers
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.commands.InputMenu.createInputMenu
import me.jakejmattson.discordkt.commands.GuildSlashCommandEvent
import me.jakejmattson.discordkt.commands.subcommand

fun hostCommands(
    config: Config,
    bot: DiscordBot
) = subcommand(HOST_COMMAND_NAME, Permissions(ModerateMembers)) {
    sub(STREAM_SUBCOMMAND_NAME, "Starts your game stream in this channel") {
        execute {
            respond(
                """
                Starting to host the stream in this channel.
                To stop the stream, just delete the message that contains it.
                """.trimIndent()
            )

            createStreamMessage(bot)
            val inputMenuMessage = createInputMenu(bot)
            createChat(inputMenuMessage, config)
        }
    }
}

const val HOST_COMMAND_NAME = "host"
const val STREAM_SUBCOMMAND_NAME = "stream"

private suspend fun GuildSlashCommandEvent<*>.createStreamMessage(bot: DiscordBot) {
    val streamMessage = channel.createMessage {
        addFile("stream.png", javaClass.getResourceAsStream("/starting_soon.png")!!)
    }
    bot.addStreamTarget(streamMessage)
}

private suspend fun GuildSlashCommandEvent<*>.createChat(
    rootMessage: Message,
    config: Config
) {
    val chatThread = channel.asChannelOf<TextChannel>().startPublicThreadWithMessage(
        rootMessage.id,
        "Discord Plays ${config.gameTitle}",
        ArchiveDuration.Week
    )

    val descriptionMessage = chatThread.createMessage(
        """
        Welcome to **Discord Plays ${config.gameTitle}** - cause the crowd is better at gaming! 🐟🎮
        
        Join in and help us to beat the game by simply clicking on the buttons below the stream 🙌
        Your input is forwarded immediately, the stream itself lags behind for around 10s. Sometimes input is momentarily blocked to save the game.
        This is a cross-community event, users from other servers participate too 👌
        
        The project is open-source at <https://github.com/Zabuzard/Discord-Plays>, feel free to come over to contribute or tell us how you like it 🤙
        """.trimIndent()
    )
}
