package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.Permission.Administrator
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.interaction.response.respond
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.commands.CommandExtensions.mentionCommandOrNull
import io.github.zabuzard.discordplays.discord.commands.CommandExtensions.requireOwnerPermission
import io.github.zabuzard.discordplays.emulation.Emulator
import kotlinx.datetime.Clock
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.BooleanArg
import me.jakejmattson.discordkt.arguments.ChoiceArg
import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.subcommand
import me.jakejmattson.discordkt.dsl.edit

fun ownerCommands(
    config: Config,
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver
) = subcommand(OWNER_COMMAND_NAME, Permissions(Administrator)) {
    sub("start", "Starts the game emulation") {
        execute {
            if (requireOwnerPermission(config)) return@execute

            val hook = interaction!!.deferEphemeralResponse()

            bot.startGame(discord)
            discord.kord.editPresence {
                playing(config.gameTitle)
                since = Clock.System.now()
            }

            val streamCommand = guild.mentionCommandOrNull(
                HOST_COMMAND_NAME,
                "$HOST_COMMAND_NAME $STREAM_SUBCOMMAND_NAME"
            )!!
            hook.respond {
                content = "Game emulation started. Stream is ready, use $streamCommand to host it."
            }
        }
    }

    sub("stop", "Stops the game emulation") {
        execute {
            if (requireOwnerPermission(config)) return@execute

            bot.stopGame()
            discord.kord.editPresence {}

            respond("Game emulation stopped.")
        }
    }

    sub("lock-input", "Only allows user input from owners, blocks any other input") {
        execute(BooleanArg("lock", description = "true to lock, false to unlock")) {
            if (requireOwnerPermission(config)) return@execute

            val lock = args.first
            bot.userInputLockedToOwners = lock

            val actionVerb = if (lock) "Locked" else "Unlocked"
            respond("$actionVerb user input.")
        }
    }

    sub("local-display", "Activates a local display on the bots machine for manual control.") {
        execute(
            BooleanArg("activate", description = "true to activate, false to deactivate"),
            BooleanArg(
                "sound",
                description = "true to activate sound, false to deactivate"
            ).optional(false)
        ) {
            if (requireOwnerPermission(config)) return@execute

            val (activate, sound) = args
            with(bot) {
                if (activate) activateLocalDisplay(sound) else deactivateLocalDisplay()
            }

            val activateVerb = if (activate) "Activated" else "Deactivated"
            val soundVerb = if (sound) "with" else "without"
            respond("$activateVerb local display $soundVerb sound.")
        }
    }

    sub("global-message", "Attaches a global message to the stream") {
        execute(
            AnyArg(
                "message",
                "leave out to clear any existing message"
            ).optionalNullable(null)
        ) {
            if (requireOwnerPermission(config)) return@execute

            val message = args.first
            with(bot) {
                setGlobalMessage(message)
            }

            val actionVerb = if (message == null) "Cleared" else "Set"
            respond("$actionVerb the global message.")
        }
    }

    sub("add-owner", "Give another user owner-permission") {
        execute(UserArg("user", "who to grant owner-permission")) {
            if (requireOwnerPermission(config)) return@execute

            val user = args.first
            config.edit { owners += user.id }

            respond("Added ${user.username} to the owners.")
        }
    }

    sub("game-metadata", "Change the metadata of the game played") {
        execute(
            ChoiceArg(
                "entity",
                "what to modify",
                *GameMetadataEntity.values().map(GameMetadataEntity::name).toTypedArray()
            ),
            AnyArg("value", "the new value for the entity")
        ) {
            if (requireOwnerPermission(config)) return@execute

            val (entity, value) = args
            config.edit {
                when (GameMetadataEntity.valueOf(entity)) {
                    GameMetadataEntity.ROM_PATH -> romPath = value
                    GameMetadataEntity.TITLE -> gameTitle = value
                }
            }

            respond("Changed $entity to $value")
        }
    }

    sub("ban", "Bans an user from the event, their input will be blocked") {
        execute(UserArg("user", "who you want to ban")) {
            if (requireOwnerPermission(config)) return@execute

            val userId = args.first.id
            if (userId in config.owners) {
                respond("Cannot ban an owner of the event.")
            }

            config.edit { bannedUsers += userId }
            respond("Banned the user from the event.")
        }
    }

    sub("save", "Starts the auto-save dialog out of its automatic schedule") {
        execute {
            if (requireOwnerPermission(config)) return@execute

            val dmChannel = author.getDmChannelOrNull()
            if (dmChannel == null) {
                respond("Please open your DMs first.")
                return@execute
            }

            respond("Triggered the auto-save routine. Check your DMs.")
            autoSaveConversation(bot, emulator, autoSaver, author).startPrivately(discord, author)
        }
    }

    sub("clear-stats", "Clears all statistics, use when starting a new run") {
        execute {
            if (requireOwnerPermission(config)) return@execute

            config.edit {
                playtimeMs = 0
                userToInputCount = emptyList()
            }

            respond("Cleared all statistics.")
        }
    }
}

const val OWNER_COMMAND_NAME = "owner"

private enum class GameMetadataEntity {
    ROM_PATH,
    TITLE
}
