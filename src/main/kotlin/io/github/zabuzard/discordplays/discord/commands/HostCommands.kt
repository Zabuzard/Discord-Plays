package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.ArchiveDuration
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.asChannelProvider
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.Host
import io.github.zabuzard.discordplays.discord.commands.InputMenu.createInputMenu

internal const val HOST_COMMAND_NAME = "host"
internal const val MIRROR_SUB_NAME = "mirror"
internal const val COMMUNITY_MESSAGE_SUB_NAME = "community-message"
internal const val COMMUNITY_MESSAGE_SUB_MESSAGE_OPTION = "message"

fun Kord.onHostCommands(
    config: Config,
    bot: DiscordBot
) {
    on<GuildChatInputCommandInteractionCreateEvent> {
        val command = interaction.command
        if (command.rootName != HOST_COMMAND_NAME || command !is SubCommand) {
            return@on
        }
        with(interaction) {
            when (command.name) {
                MIRROR_SUB_NAME -> onMirror(config, bot)
                COMMUNITY_MESSAGE_SUB_NAME -> onCommunityMessage(bot)
            }
        }
    }
}

private suspend fun GuildChatInputCommandInteraction.onMirror(
    config: Config,
    bot: DiscordBot
) {
    respondEphemeral {
        content = """
                |Starting to mirror the stream in this channel.
                |To stop the stream, just delete the message that contains it.
        """.trimMargin()
    }

    val mirrorMessage = createInputMenu(bot)

    mirrorMessage.edit {
        val coverImage =
            if (bot.gameCurrentlyRunning) DiscordBot.STARTING_SOON_COVER_RESOURCE else DiscordBot.OFFLINE_COVER_RESOURCE
        addFile(
            "stream.png",
            { javaClass.getResourceAsStream(coverImage)!! }.asChannelProvider()
        )
    }

    val chatDescriptionMessage = createChat(mirrorMessage, config).also { it.pin() }

    bot.addHost(Host(getGuild(), mirrorMessage, chatDescriptionMessage))
}

private suspend fun GuildChatInputCommandInteraction.onCommunityMessage(
    bot: DiscordBot
) {
    val message = command.strings[COMMUNITY_MESSAGE_SUB_MESSAGE_OPTION]
    bot.setCommunityMessage(getGuild(), message)

    val actionVerb = if (message == null) "Cleared" else "Set"
    respondEphemeral { content = "$actionVerb the community message." }
}

private suspend fun GuildChatInputCommandInteraction.createChat(
    rootMessage: Message,
    config: Config
) =
    channel.asChannelOf<TextChannel>().startPublicThreadWithMessage(
        rootMessage.id,
        "Discord Plays ${config.gameTitle}",
        ArchiveDuration.Week
    ).createMessage(
        """
        |Welcome to **Discord Plays ${config.gameTitle}** - cause the crowd is just better at it! 🐟🎮
        |
        |Join in and help us to beat the game by simply clicking the buttons below the stream 🙌
        |Your input is forwarded immediately, the stream itself lags behind for around 10s. Sometimes input is momentarily blocked to save the game.
        |This is a cross-community event, users from other servers participate too 👌
        |
        |The project is open-source at <https://github.com/Zabuzard/Discord-Plays>, feel free to come over to contribute or tell us how you like it 🤙
        """.trimMargin()
    )
