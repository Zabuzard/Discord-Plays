package io.github.zabuzard.discordplays.discord.commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.apache.logging.log4j.Level

fun JDA.registerCommands() =
    updateCommands().addCommands(
        hostCommands(),
        ownerCommands()
    )

private fun hostCommands() =
    Commands.slash(HOST_COMMAND_NAME, "commands for streaming the event in your community")
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
        .addSubcommands(
            SubcommandData(MIRROR_SUB_NAME, "Mirrors the game stream in this channel"),
            SubcommandData(
                REMOVE_MIRROR_SUB_NAME,
                "Removes any existing mirrors for this community"
            ),
            SubcommandData(
                COMMUNITY_MESSAGE_SUB_NAME,
                "Attaches a community-wide message to the stream hosted in this channel"
            )
                .addOption(
                    OptionType.STRING,
                    COMMUNITY_MESSAGE_SUB_MESSAGE_OPTION,
                    "leave out to clear any existing message"
                )
        )

private fun ownerCommands() =
    Commands.slash(OWNER_COMMAND_NAME, "commands for managing the event")
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        .addSubcommands(
            SubcommandData(START_SUB_NAME, "Starts the game emulation"),
            SubcommandData(STOP_SUB_NAME, "Stops the game emulation"),
            SubcommandData(
                LOCK_INPUT_SUB_NAME,
                "Only allows user input from owners, blocks any other input"
            )
                .addOption(
                    OptionType.BOOLEAN,
                    LOCK_INPUT_SUB_LOCK_OPTION,
                    "true to lock, false to unlock",
                    true
                ),
            SubcommandData(
                LOCAL_DISPLAY_SUB_NAME,
                "Activates a local display on the bots machine for manual control."
            )
                .addOptions(
                    OptionData(
                        OptionType.BOOLEAN,
                        LOCAL_DISPLAY_SUB_ACTIVATE_OPTION,
                        "true to activate, false to deactivate",
                        true
                    ),
                    OptionData(
                        OptionType.BOOLEAN,
                        LOCAL_DISPLAY_SUB_SOUND_OPTION,
                        "true to activate sound, false to deactivate"
                    )
                ),
            SubcommandData(GLOBAL_MESSAGE_SUB_NAME, "Attaches a global message to the stream")
                .addOption(
                    OptionType.STRING,
                    GLOBAL_MESSAGE_SUB_MESSAGE_OPTION,
                    "leave out to clear any existing message"
                ),
            SubcommandData(CHAT_MESSAGE_SUB_NAME, "Sends a message to the chats of all hosts")
                .addOption(
                    OptionType.STRING,
                    CHAT_MESSAGE_SUB_MESSAGE_OPTION,
                    "message to send",
                    true
                ),
            SubcommandData(ADD_OWNER_SUB_NAME, "Give another user owner-permission")
                .addOption(
                    OptionType.USER,
                    ADD_OWNER_SUB_USER_OPTION,
                    "who to grant owner-permission",
                    true
                ),
            SubcommandData(GAME_METADATA_SUB_NAME, "Change the metadata of the game played")
                .addOptions(
                    OptionData(
                        OptionType.STRING,
                        GAME_METADATA_SUB_ENTITY_OPTION,
                        "the entity to change",
                        true
                    )
                        .addChoices(
                            GameMetadataEntity.values().map(GameMetadataEntity::name)
                                .map { Command.Choice(it, it) }
                        ),
                    OptionData(
                        OptionType.STRING,
                        GAME_METADATA_SUB_VALUE_OPTION,
                        "the new value for the entity",
                        true
                    )
                ),
            SubcommandData(BAN_SUB_NAME, "Bans an user from the event, their input will be blocked")
                .addOption(OptionType.USER, BAN_SUB_USER_OPTION, "who you want to ban", true),
            SubcommandData(
                SAVE_SUB_NAME,
                "Starts the auto-save dialog out of its automatic schedule"
            ),
            SubcommandData(
                CLEAR_STATS_SUB_NAME,
                "Clears all statistics, use when starting a new run"
            ),
            SubcommandData(LOG_LEVEL_SUB_NAME, "Changes the log level")
                .addOptions(
                    OptionData(
                        OptionType.STRING,
                        LOG_LEVEL_SUB_LEVEL_OPTION,
                        "the level to set",
                        true
                    )
                        .addChoices(Level.values().map(Level::name).map { Command.Choice(it, it) })
                ),
            SubcommandData(CREATE_VIDEO_SUB_NAME, "Creates a video out of the recorded frames")
                .addOption(
                    OptionType.STRING,
                    CREATE_VIDEO_SUB_DATE_OPTION,
                    "to use frames of, e.g. 2023-02-23, also folder name",
                    true
                )
        )
