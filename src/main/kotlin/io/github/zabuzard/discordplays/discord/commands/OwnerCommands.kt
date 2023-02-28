package io.github.zabuzard.discordplays.discord.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.stats.Statistics
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.local.FrameRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.nio.file.Path
import kotlin.io.path.notExists

internal const val COMMAND_NAME = "owner"
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

fun Kord.onOwnerCommands(
    config: Config,
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver,
    statistics: Statistics
) {
    on<GuildChatInputCommandInteractionCreateEvent> {
        val command = interaction.command
        if (command.rootName != COMMAND_NAME || command !is SubCommand) {
            return@on
        }
        if (interaction.user.id !in config.owners) {
            interaction.respondEphemeral { content = "Sorry, only owners can use this command." }
            return@on
        }

        with(interaction) {
            when (command.name) {
                START_SUB_NAME -> onStart(config, bot)
                STOP_SUB_NAME -> onStop(bot)
                LOCK_INPUT_SUB_NAME -> onLockInput(bot)
                LOCAL_DISPLAY_SUB_NAME -> onLocalDisplay(bot)
                GLOBAL_MESSAGE_SUB_NAME -> onGlobalMessage(bot)
                CHAT_MESSAGE_SUB_NAME -> onChatMessage(bot)
                ADD_OWNER_SUB_NAME -> onAddOwner(config)
                GAME_METADATA_SUB_NAME -> onGameMetadata(config)
                BAN_SUB_NAME -> onBan(config)
                SAVE_SUB_NAME -> onSave(bot, emulator, autoSaver)
                CLEAR_STATS_SUB_NAME -> onClearStats(config, statistics)
                LOG_LEVEL_SUB_NAME -> onLogLevel()
                CREATE_VIDEO_SUB_NAME -> onCreateVideo(config)
            }
        }
    }
}

private suspend fun GuildChatInputCommandInteraction.onStart(
    config: Config,
    bot: DiscordBot
) {
    val hook = deferEphemeralResponse()

    bot.startGame(kord)
    kord.editPresence {
        playing(config.gameTitle)
        since = Clock.System.now()
    }

    hook.respond {
        content = "Game emulation started. Stream is ready."
    }
}

private suspend fun GuildChatInputCommandInteraction.onStop(
    bot: DiscordBot
) {
    bot.stopGame()
    kord.editPresence {}

    respondEphemeral { content = "Game emulation stopped." }
}

private suspend fun GuildChatInputCommandInteraction.onLockInput(
    bot: DiscordBot
) {
    val lock = command.booleans[LOCK_INPUT_SUB_LOCK_OPTION]!!
    bot.userInputLockedToOwners = lock

    val actionVerb = if (lock) "Locked" else "Unlocked"
    respondEphemeral { content = "$actionVerb user input." }
}

private suspend fun GuildChatInputCommandInteraction.onLocalDisplay(
    bot: DiscordBot
) {
    val activate = command.booleans[LOCAL_DISPLAY_SUB_ACTIVATE_OPTION]!!
    val sound = command.booleans[LOCAL_DISPLAY_SUB_SOUND_OPTION] ?: false

    with(bot) {
        if (activate) activateLocalDisplay(sound) else deactivateLocalDisplay()
    }

    val activateVerb = if (activate) "Activated" else "Deactivated"
    val soundVerb = if (sound) "with" else "without"
    respondEphemeral { content = "$activateVerb local display $soundVerb sound." }
}

private suspend fun GuildChatInputCommandInteraction.onGlobalMessage(
    bot: DiscordBot
) {
    val message = command.strings[GLOBAL_MESSAGE_SUB_MESSAGE_OPTION]
    bot.setGlobalMessage(message)

    val actionVerb = if (message == null) "Cleared" else "Set"
    respondEphemeral { content = "$actionVerb the global message." }
}

private suspend fun GuildChatInputCommandInteraction.onChatMessage(
    bot: DiscordBot
) {
    val hook = deferEphemeralResponse()

    val message = command.strings[CHAT_MESSAGE_SUB_MESSAGE_OPTION]!!
    bot.sendChatMessage(message)

    hook.respond { content = "Send the chat message." }
}

private suspend fun GuildChatInputCommandInteraction.onAddOwner(
    config: Config
) {
    val user = command.users[ADD_OWNER_SUB_USER_OPTION]!!
    config.edit { owners += user.id }

    "Added ${user.username} to the owners.".let {
        logger.info { it }
        respondEphemeral { content = it }
    }
}

private suspend fun GuildChatInputCommandInteraction.onGameMetadata(
    config: Config
) {
    val entity = command.strings[GAME_METADATA_SUB_ENTITY_OPTION]!!
    val value = command.strings[GAME_METADATA_SUB_VALUE_OPTION]!!

    config.edit {
        when (GameMetadataEntity.valueOf(entity)) {
            GameMetadataEntity.ROM_PATH -> romPath = value
            GameMetadataEntity.TITLE -> gameTitle = value
        }
    }

    "Changed metadata $entity to $value".let {
        logger.info { it }
        respondEphemeral { content = it }
    }
}

private suspend fun GuildChatInputCommandInteraction.onBan(
    config: Config
) {
    val user = command.users[BAN_SUB_USER_OPTION]!!
    val userId = user.id
    if (userId in config.owners) {
        respondEphemeral { content = "Cannot ban an owner of the event." }
        return
    }

    config.edit { bannedUsers += userId }

    "Banned ${user.username} from the event.".let {
        logger.info { it }
        respondEphemeral { content = it }
    }
}

private suspend fun GuildChatInputCommandInteraction.onSave(
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver
) {
    val dmChannel = user.getDmChannelOrNull()
    if (dmChannel == null) {
        respondEphemeral { content = "Please open your DMs first." }
        return
    }

    logger.info { "Triggered auto-save manually" }
    respondEphemeral { content = "Triggered the auto-save routine. Check your DMs." }
    autoSaveConversation(autoSaver, dmChannel)
}

private suspend fun GuildChatInputCommandInteraction.onClearStats(
    config: Config,
    statistics: Statistics
) {
    statistics.clearStats()

    "Cleared all statistics.".let {
        logger.info { it }
        respondEphemeral { content = it }
    }
}

private suspend fun GuildChatInputCommandInteraction.onLogLevel() {
    val level = Level.getLevel(command.strings[LOG_LEVEL_SUB_LEVEL_OPTION]!!)!!
    Configurator.setAllLevels(LogManager.getRootLogger().name, level)

    "Set the log level to $level.".let {
        logger.info { it }
        respondEphemeral { content = it }
    }
}

private suspend fun GuildChatInputCommandInteraction.onCreateVideo(
    config: Config
) {
    val date = command.strings[CREATE_VIDEO_SUB_DATE_OPTION]!!

    val frameFolder = Path.of(config.recordingPath, date)
    if (frameFolder.notExists()) {
        respondEphemeral { content = "Could not find any recordings for $date." }
        return
    }
    respondEphemeral { content = "Command invoked, video is being created." }

    withContext(Dispatchers.IO) {
        ProcessBuilder(
            "ffmpeg",
            "-framerate",
            "5",
            "-r",
            "5",
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

private val logger = KotlinLogging.logger {}

internal enum class GameMetadataEntity {
    ROM_PATH,
    TITLE
}
