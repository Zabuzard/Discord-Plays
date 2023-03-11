package io.github.zabuzard.discordplays

import io.github.oshai.KotlinLogging
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
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent

fun main(args: Array<String>) {
    require(args.size == 1) {
        "Incorrect number of arguments. The first argument must be the bot token. " +
            "Supplied arguments: ${args.contentToString()}"
    }

    val token = args.first()

    val jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .build()
    jda.awaitReady()

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
        jda,
        config,
        emulator,
        streamRenderer,
        overlayRenderer,
        localDisplay,
        statistics,
        autoSaver,
        frameRecorder
    )

    jda.registerCommands()

    jda.onOwnerCommands(config, bot, autoSaver, statistics)
    jda.onHostCommands(config, bot)
    jda.onControlButtonClicked(bot)
    jda.onChatMessage(config, bot)
    jda.onAutoSaveConversation(bot, emulator, autoSaver)

    logger.info { "Bot started, ready" }
}

private val logger = KotlinLogging.logger {}
