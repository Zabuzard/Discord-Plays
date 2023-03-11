package io.github.zabuzard.discordplays.discord.commands

import io.github.oshai.KotlinLogging
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.Extensions.replyEphemeral
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.stats.Statistics
import io.github.zabuzard.discordplays.local.FrameRecorder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity.playing
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.notExists

internal const val OWNER_COMMAND_NAME = "owner"
internal const val START_SUB_NAME = "start"
internal const val STOP_SUB_NAME = "stop"

internal const val LOCK_INPUT_SUB_NAME = "lock-input"
internal const val LOCK_INPUT_SUB_LOCK_OPTION = "lock"

internal const val LOCAL_DISPLAY_SUB_NAME = "local-display"
internal const val LOCAL_DISPLAY_SUB_ACTIVATE_OPTION = "activate"
internal const val LOCAL_DISPLAY_SUB_SOUND_OPTION = "sound"

internal const val GLOBAL_MESSAGE_SUB_NAME = "global-message"
internal const val GLOBAL_MESSAGE_SUB_MESSAGE_OPTION = "message"

internal const val CHAT_MESSAGE_SUB_NAME = "chat-message"
internal const val CHAT_MESSAGE_SUB_MESSAGE_OPTION = "message"

internal const val ADD_OWNER_SUB_NAME = "add-owner"
internal const val ADD_OWNER_SUB_USER_OPTION = "user"

internal const val GAME_METADATA_SUB_NAME = "game-metadata"
internal const val GAME_METADATA_SUB_ENTITY_OPTION = "entity"
internal const val GAME_METADATA_SUB_VALUE_OPTION = "value"

internal const val BAN_SUB_NAME = "ban"
internal const val BAN_SUB_USER_OPTION = "user"

internal const val SAVE_SUB_NAME = "save"
internal const val CLEAR_STATS_SUB_NAME = "clear-stats"

internal const val LOG_LEVEL_SUB_NAME = "log-level"
internal const val LOG_LEVEL_SUB_LEVEL_OPTION = "level"

internal const val CREATE_VIDEO_SUB_NAME = "create-video"
internal const val CREATE_VIDEO_SUB_DATE_OPTION = "date"

fun JDA.onOwnerCommands(
    config: Config,
    bot: DiscordBot,
    autoSaver: AutoSaver,
    statistics: Statistics
) {
    addEventListener(object : ListenerAdapter() {
        override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
            if (event.name != OWNER_COMMAND_NAME || event.subcommandName == null) {
                return
            }
            if (event.user.idLong !in config.owners) {
                event.replyEphemeral("Sorry, only owners can use this command.")
                    .queue()
                return
            }

            with(event) {
                when (subcommandName) {
                    START_SUB_NAME -> onStart(config, bot)
                    STOP_SUB_NAME -> onStop(bot)
                    LOCK_INPUT_SUB_NAME -> onLockInput(bot)
                    LOCAL_DISPLAY_SUB_NAME -> onLocalDisplay(bot)
                    GLOBAL_MESSAGE_SUB_NAME -> onGlobalMessage(bot)
                    CHAT_MESSAGE_SUB_NAME -> onChatMessage(bot)
                    ADD_OWNER_SUB_NAME -> onAddOwner(config)
                    GAME_METADATA_SUB_NAME -> onGameMetadata(config)
                    BAN_SUB_NAME -> onBan(config)
                    SAVE_SUB_NAME -> onSave(autoSaver)
                    CLEAR_STATS_SUB_NAME -> onClearStats(statistics)
                    LOG_LEVEL_SUB_NAME -> onLogLevel()
                    CREATE_VIDEO_SUB_NAME -> onCreateVideo(config)
                }
            }
        }
    })
}

private fun SlashCommandInteractionEvent.onStart(
    config: Config,
    bot: DiscordBot
) {
    deferReply(true).queue()

    bot.startGame(jda)
    jda.presence.setPresence(playing(config.gameTitle), false)

    hook.editOriginal("Game emulation started. Stream is ready.").queue()
}

private fun SlashCommandInteractionEvent.onStop(
    bot: DiscordBot
) {
    bot.stopGame()
    jda.presence.setPresence(OnlineStatus.ONLINE, null, false)

    replyEphemeral("Game emulation stopped.").queue()
}

private fun SlashCommandInteractionEvent.onLockInput(
    bot: DiscordBot
) {
    val lock = getOption(LOCK_INPUT_SUB_LOCK_OPTION, OptionMapping::getAsBoolean)!!
    bot.userInputLockedToOwners = lock

    val actionVerb = if (lock) "Locked" else "Unlocked"
    replyEphemeral("$actionVerb user input.").queue()
}

private fun SlashCommandInteractionEvent.onLocalDisplay(
    bot: DiscordBot
) {
    val activate = getOption(LOCAL_DISPLAY_SUB_ACTIVATE_OPTION, OptionMapping::getAsBoolean)!!
    val sound = getOption(LOCAL_DISPLAY_SUB_SOUND_OPTION, OptionMapping::getAsBoolean) ?: false

    with(bot) {
        if (activate) activateLocalDisplay(sound) else deactivateLocalDisplay()
    }

    val activateVerb = if (activate) "Activated" else "Deactivated"
    val soundVerb = if (sound) "with" else "without"
    replyEphemeral("$activateVerb local display $soundVerb sound.").queue()
}

private fun SlashCommandInteractionEvent.onGlobalMessage(
    bot: DiscordBot
) {
    val message = getOption(GLOBAL_MESSAGE_SUB_MESSAGE_OPTION, OptionMapping::getAsString)
    bot.setGlobalMessage(message)

    val actionVerb = if (message == null) "Cleared" else "Set"
    replyEphemeral("$actionVerb the global message.").queue()
}

private fun SlashCommandInteractionEvent.onChatMessage(
    bot: DiscordBot
) {
    deferReply(true).queue()

    val message = getOption(CHAT_MESSAGE_SUB_MESSAGE_OPTION, OptionMapping::getAsString)!!
    bot.sendChatMessage(message)

    hook.editOriginal("Send the chat message.").queue()
}

private fun SlashCommandInteractionEvent.onAddOwner(
    config: Config
) {
    val user = getOption(ADD_OWNER_SUB_USER_OPTION, OptionMapping::getAsUser)!!
    config.edit { owners += user.idLong }

    "Added ${user.name} to the owners.".let {
        logger.info { it }
        replyEphemeral(it).queue()
    }
}

private fun SlashCommandInteractionEvent.onGameMetadata(
    config: Config
) {
    val entity = getOption(GAME_METADATA_SUB_ENTITY_OPTION, OptionMapping::getAsString)!!
    val value = getOption(GAME_METADATA_SUB_VALUE_OPTION, OptionMapping::getAsString)!!

    config.edit {
        when (GameMetadataEntity.valueOf(entity)) {
            GameMetadataEntity.ROM_PATH -> romPath = value
            GameMetadataEntity.TITLE -> gameTitle = value
        }
    }

    "Changed metadata $entity to $value".let {
        logger.info { it }
        replyEphemeral(it).queue()
    }
}

private fun SlashCommandInteractionEvent.onBan(
    config: Config
) {
    val user = getOption(BAN_SUB_USER_OPTION, OptionMapping::getAsUser)!!
    val userId = user.idLong
    if (userId in config.owners) {
        replyEphemeral("Cannot ban an owner of the event.").queue()
        return
    }

    config.edit { bannedUsers += userId }

    "Banned ${user.name} from the event.".let {
        logger.info { it }
        replyEphemeral(it).queue()
    }
}

private fun SlashCommandInteractionEvent.onSave(
    autoSaver: AutoSaver
) {
    deferReply(true).queue()

    user.openPrivateChannel()
        .onSuccess { autoSaveConversation(autoSaver, it) }
        .flatMap {
            logger.info { "Triggered auto-save manually" }
            hook.editOriginal("Triggered the auto-save routine. Check your DMs.")
        }.onErrorFlatMap { hook.editOriginal("Please open your DMs first.") }
        .queue()
}

private fun SlashCommandInteractionEvent.onClearStats(
    statistics: Statistics
) {
    statistics.clearStats()

    "Cleared all statistics.".let {
        logger.info { it }
        replyEphemeral(it).queue()
    }
}

private fun SlashCommandInteractionEvent.onLogLevel() {
    val level =
        Level.getLevel(getOption(LOG_LEVEL_SUB_LEVEL_OPTION, OptionMapping::getAsString)!!)!!
    Configurator.setAllLevels(LogManager.getRootLogger().name, level)

    "Set the log level to $level.".let {
        logger.info { it }
        replyEphemeral(it).queue()
    }
}

private fun SlashCommandInteractionEvent.onCreateVideo(
    config: Config
) {
    val date = getOption(CREATE_VIDEO_SUB_DATE_OPTION, OptionMapping::getAsString)!!

    val frameFolder = Path.of(config.recordingPath, date)
    if (frameFolder.notExists()) {
        replyEphemeral("Could not find any recordings for $date.").queue()
        return
    }
    replyEphemeral("Command invoked, video is being created.").queue()

    CompletableFuture.runAsync {
        logAllExceptions {
            ProcessBuilder(
                "ffmpeg",
                "-framerate",
                "10",
                "-r",
                "10",
                "-i",
                "%d${FrameRecorder.FRAME_SUFFIX}",
                "-pix_fmt",
                "yuv420p",
                "-profile:v",
                "high",
                "-level:v",
                "4.1",
                "-crf:v",
                "20",
                "-movflags",
                "+faststart",
                "$date.mp4"
            ).directory(frameFolder.toFile()).start()
        }
    }
}

private val logger = KotlinLogging.logger {}

internal enum class GameMetadataEntity {
    ROM_PATH,
    TITLE
}
