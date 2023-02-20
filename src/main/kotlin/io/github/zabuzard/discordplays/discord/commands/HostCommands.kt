package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.Permission.ModerateMembers
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.Host
import io.github.zabuzard.discordplays.discord.commands.CommandExtensions.clearEmbeds
import io.github.zabuzard.discordplays.discord.commands.InputMenu.createInputMenu
import me.jakejmattson.discordkt.arguments.AnyArg
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

            val streamMessage = createInputMenu(bot)

            streamMessage.edit {
                val coverImage = if (bot.gameCurrentlyRunning) "/starting_soon.png" else "/currently_offline.png"
                addFile("stream.png", javaClass.getResourceAsStream(coverImage)!!)
                clearEmbeds()
            }

            val chatDescriptionMessage = createChat(streamMessage, config)

            bot.addHost(Host(streamMessage, chatDescriptionMessage))
        }
    }

    sub(
        "community-message",
        "Attaches a community-wide message to the stream hosted in this channel"
    ) {
        execute(
            AnyArg(
                "message",
                "leave out to clear any existing message"
            ).optionalNullable(null)
        ) {
            val message = args.first
            with(bot) {
                setCommunityMessage(channel.id, message)
            }

            val actionVerb = if (message == null) "Cleared" else "Set"
            respond("$actionVerb the community message.")
        }
    }
}

const val HOST_COMMAND_NAME = "host"
const val STREAM_SUBCOMMAND_NAME = "stream"

private suspend fun GuildSlashCommandEvent<*>.createChat(
    rootMessage: Message,
    config: Config
) =
    channel.asChannelOf<TextChannel>().startPublicThreadWithMessage(
        rootMessage.id,
        "Discord Plays ${config.gameTitle}",
        ArchiveDuration.Week
    ).createMessage(
        """
        Welcome to **Discord Plays ${config.gameTitle}** - cause the crowd is just better at it! 🐟🎮
        
        Join in and help us to beat the game by simply clicking the buttons below the stream 🙌
        Your input is forwarded immediately, the stream itself lags behind for around 10s. Sometimes input is momentarily blocked to save the game.
        This is a cross-community event, users from other servers participate too 👌
        
        The project is open-source at <https://github.com/Zabuzard/Discord-Plays>, feel free to come over to contribute or tell us how you like it 🤙
        """.trimIndent()
    )
