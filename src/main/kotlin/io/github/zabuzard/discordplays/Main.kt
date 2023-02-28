package io.github.zabuzard.discordplays

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.commands.AutoSaver
import io.github.zabuzard.discordplays.discord.commands.onAutoSaveConversation
import io.github.zabuzard.discordplays.discord.commands.onChatMessage
import io.github.zabuzard.discordplays.discord.commands.onControlButtonClicked
import io.github.zabuzard.discordplays.discord.commands.onHostCommands
import io.github.zabuzard.discordplays.discord.commands.onOwnerCommands
import io.github.zabuzard.discordplays.discord.commands.registerCommands
import io.github.zabuzard.discordplays.discord.stats.Statistics
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.local.FrameRecorder
import io.github.zabuzard.discordplays.local.LocalDisplay
import io.github.zabuzard.discordplays.stream.OverlayRenderer
import io.github.zabuzard.discordplays.stream.StreamRenderer
import mu.KotlinLogging

suspend fun main(args: Array<String>) {
    require(args.size == 1) {
        "Incorrect number of arguments. The first argument must be the bot token. " +
            "Supplied arguments: ${args.contentToString()}"
    }

    val token = args.first()

    val kord = Kord(token)

    // Create services
    val config = Config.loadOrDefault()

    val emulator = Emulator(config)

    val overlayRenderer = OverlayRenderer(config)
    val streamRenderer = StreamRenderer(config, emulator, overlayRenderer)

    val localDisplay = LocalDisplay(streamRenderer, emulator)
    val frameRecorder = FrameRecorder(config, streamRenderer)

    val statistics = Statistics(config)
    val autoSaver = AutoSaver(config, streamRenderer)
    val bot = DiscordBot(
        config,
        emulator,
        streamRenderer,
        overlayRenderer,
        localDisplay,
        statistics,
        autoSaver,
        frameRecorder
    )

    kord.registerCommands()

    kord.onOwnerCommands(config, bot, emulator, autoSaver, statistics)
    kord.onHostCommands(config, bot)
    kord.onControlButtonClicked(bot)
    kord.onChatMessage(config, bot)
    kord.onAutoSaveConversation(bot, emulator, autoSaver)

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent

        logger.info { "Bot started, ready" }
    }
}

private val logger = KotlinLogging.logger {}
