package io.github.zabuzard.discordplays.discord.commands

import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.replyEphemeral
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.Host
import io.github.zabuzard.discordplays.discord.commands.InputMenu.createInputMenu
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload

internal const val HOST_COMMAND_NAME = "host"
internal const val MIRROR_SUB_NAME = "mirror"
internal const val REMOVE_MIRROR_SUB_NAME = "remove-mirror"
internal const val COMMUNITY_MESSAGE_SUB_NAME = "community-message"
internal const val COMMUNITY_MESSAGE_SUB_MESSAGE_OPTION = "message"

fun JDA.onHostCommands(
    config: Config,
    bot: DiscordBot
) {
    addEventListener(object : ListenerAdapter() {
        override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
            if (event.name != HOST_COMMAND_NAME || event.subcommandName == null) {
                return
            }
            with(event) {
                when (subcommandName) {
                    MIRROR_SUB_NAME -> onMirror(config, bot)
                    REMOVE_MIRROR_SUB_NAME -> onRemoveMirror(bot)
                    COMMUNITY_MESSAGE_SUB_NAME -> onCommunityMessage(bot)
                }
            }
        }
    })
}

private fun SlashCommandInteractionEvent.onMirror(
    config: Config,
    bot: DiscordBot
) {
    if (bot.hasHost(guild!!)) {
        reply("Only one mirror per guild allowed, please first delete the existing mirror.").setEphemeral(
            true
        ).queue()
        return
    }

    replyEphemeral(
        """
                |Starting to mirror the stream in this channel.
                |To stop the stream, just delete the message that contains it.
        """.trimMargin()
    ).queue()

    val mirrorMessageAction = createInputMenu()

    val coverImage =
        if (bot.gameCurrentlyRunning) DiscordBot.STARTING_SOON_COVER_RESOURCE else DiscordBot.OFFLINE_COVER_RESOURCE
    mirrorMessageAction.setFiles(
        FileUpload.fromData(
            javaClass.getResourceAsStream(coverImage)!!,
            "stream.png"
        )
    )

    mirrorMessageAction.flatMap { mirrorMessage ->
        mirrorMessage.createChat(config).map { mirrorMessage to it }
    }.onSuccess { (mirrorMessage, chatDescriptionMessage) ->
        bot.addHost(Host(guild!!, mirrorMessage, chatDescriptionMessage))
    }.queue()
}

private fun SlashCommandInteractionEvent.onRemoveMirror(
    bot: DiscordBot
) {
    bot.removeHost(guild!!)
    replyEphemeral("Removed any existing mirror for this community.").queue()
}

private fun SlashCommandInteractionEvent.onCommunityMessage(
    bot: DiscordBot
) {
    val message = getOption(COMMUNITY_MESSAGE_SUB_MESSAGE_OPTION, OptionMapping::getAsString)
    bot.setCommunityMessage(guild!!, message)

    val actionVerb = if (message == null) "Cleared" else "Set"
    replyEphemeral("$actionVerb the community message.").queue()
}

private fun Message.createChat(
    config: Config
) =
    createThreadChannel("Discord Plays ${config.gameTitle}")
        .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
        .flatMap {
            it.sendMessage(
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
        }.flatMap { message ->
            message.pin().map { message }
        }
