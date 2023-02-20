package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.Permission.Administrator
import dev.kord.common.entity.Permissions
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.commands.CommandExtensions.mentionCommandOrNull
import io.github.zabuzard.discordplays.discord.commands.CommandExtensions.requireOwnerPermission
import kotlinx.datetime.Clock
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.BooleanArg
import me.jakejmattson.discordkt.arguments.ChoiceArg
import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.subcommand
import me.jakejmattson.discordkt.dsl.edit

fun ownerCommands(
    config: Config,
    bot: DiscordBot
) = subcommand(OWNER_COMMAND_NAME, Permissions(Administrator)) {
    sub("start", "Starts the game emulation") {
        execute {
            if (requireOwnerPermission(config)) return@execute

            bot.startGame()
            discord.kord.editPresence {
                playing(config.gameTitle)
                since = Clock.System.now()
            }

            val streamCommand = guild.mentionCommandOrNull(
                HOST_COMMAND_NAME,
                "$HOST_COMMAND_NAME $STREAM_SUBCOMMAND_NAME"
            )!!
            respond("Game emulation started. Stream is ready, use $streamCommand to host it.")
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
                "leave away to clear any existing message"
            ).optionalNullable(null)
        ) {
            if (requireOwnerPermission(config)) return@execute

            val message = args.first
            with(bot) {
                if (message == null) clearGlobalMessage() else sendGlobalMessage(message)
            }

            val actionVerb = if (message == null) "Cleared" else "Set"
            respond("$actionVerb the global message.")
        }
    }

    sub("add-owner", "Give another user owner-permission") {
        execute(UserArg("user", "who to grant owner-permission")) {
            if (requireOwnerPermission(config)) return@execute

            val user = args.first
            config.edit { owners += user.id.value }

            respond("Added ${user.username} to the owners.")
        }
    }

    sub("remove-owner", "Revokes owner-permission from an owner") {
        execute(UserArg("user", "who to revoke owner-permission from")) {
            if (requireOwnerPermission(config)) return@execute

            if (config.owners.size <= 1) {
                respond("Sorry, cannot remove the last owner.")
                return@execute
            }

            val user = args.first
            config.edit { owners -= user.id.value }

            respond("Removed ${user.username} from the owners.")
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
}

const val OWNER_COMMAND_NAME = "owner"

private enum class GameMetadataEntity {
    ROM_PATH,
    TITLE
}
