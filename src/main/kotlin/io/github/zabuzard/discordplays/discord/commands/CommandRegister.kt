package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.rest.builder.interaction.GlobalMultiApplicationCommandBuilder
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.user
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.logging.log4j.Level

fun Kord.registerCommands() {
    launch {
        createGlobalApplicationCommands {
            hostCommands()
            ownerCommands()
        }.collect()
    }
}

private fun GlobalMultiApplicationCommandBuilder.hostCommands() {
    input(HOST_COMMAND_NAME, "commands for streaming the event in your community") {
        defaultMemberPermissions = Permissions(Permission.ModerateMembers)

        subCommand(MIRROR_SUB_NAME, "Mirrors the game stream in this channel")
        subCommand(
            COMMUNITY_MESSAGE_SUB_NAME,
            "Attaches a community-wide message to the stream hosted in this channel"
        ) {
            string(
                COMMUNITY_MESSAGE_SUB_MESSAGE_OPTION,
                "leave out to clear any existing message"
            )
        }
    }
}

private fun GlobalMultiApplicationCommandBuilder.ownerCommands() {
    input(COMMAND_NAME, "commands for managing the event") {
        defaultMemberPermissions = Permissions(Permission.Administrator)

        subCommand(START_SUB_NAME, "Starts the game emulation")
        subCommand(STOP_SUB_NAME, "Stops the game emulation")
        subCommand(
            LOCK_INPUT_SUB_NAME,
            "Only allows user input from owners, blocks any other input"
        ) {
            boolean(
                LOCK_INPUT_SUB_LOCK_OPTION,
                "true to lock, false to unlock"
            ) { required = true }
        }
        subCommand(
            LOCAL_DISPLAY_SUB_NAME,
            "Activates a local display on the bots machine for manual control."
        ) {
            boolean(
                LOCAL_DISPLAY_SUB_ACTIVATE_OPTION,
                "true to activate, false to deactivate"
            ) { required = true }
            boolean(
                LOCAL_DISPLAY_SUB_SOUND_OPTION,
                "true to activate sound, false to deactivate"
            )
        }
        subCommand(GLOBAL_MESSAGE_SUB_NAME, "Attaches a global message to the stream") {
            string(
                GLOBAL_MESSAGE_SUB_MESSAGE_OPTION,
                "leave out to clear any existing message"
            )
        }
        subCommand(CHAT_MESSAGE_SUB_NAME, "Sends a message to the chats of all hosts") {
            string(CHAT_MESSAGE_SUB_MESSAGE_OPTION, "message to send") {
                required = true
            }
        }
        subCommand(ADD_OWNER_SUB_NAME, "Give another user owner-permission") {
            user(
                ADD_OWNER_SUB_USER_OPTION,
                "who to grant owner-permission"
            ) { required = true }
        }
        subCommand(GAME_METADATA_SUB_NAME, "Change the metadata of the game played") {
            string(GAME_METADATA_SUB_ENTITY_OPTION, "the entity to change") {
                required = true
                GameMetadataEntity.values().map(GameMetadataEntity::name)
                    .forEach { entity ->
                        choice(entity, entity)
                    }
            }
            string(
                GAME_METADATA_SUB_VALUE_OPTION,
                "the new value for the entity"
            ) { required = true }
        }
        subCommand(
            BAN_SUB_NAME,
            "Bans an user from the event, their input will be blocked"
        ) {
            user(BAN_SUB_USER_OPTION, "who you want to ban") { required = true }
        }
        subCommand(
            SAVE_SUB_NAME,
            "Starts the auto-save dialog out of its automatic schedule"
        )
        subCommand(
            CLEAR_STATS_SUB_NAME,
            "Clears all statistics, use when starting a new run"
        )
        subCommand(LOG_LEVEL_SUB_NAME, "Changes the log level") {
            string(LOG_LEVEL_SUB_LEVEL_OPTION, "the level to set") {
                required = true
                Level.values().map(Level::name).forEach { level: String ->
                    choice(level, level)
                }
            }
        }
        subCommand(
            CREATE_VIDEO_SUB_NAME,
            "Creates a video out of the recorded frames"
        ) {
            string(
                CREATE_VIDEO_SUB_DATE_OPTION,
                "to use frames of, e.g. 2023-02-23, also folder name"
            ) { required = true }
        }
    }
}
