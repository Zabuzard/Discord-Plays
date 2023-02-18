package io.github.zabuzard.discordplays.discord.commands

import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.commands.CommandExtensions.mentionCommandOrNull
import kotlinx.datetime.Clock
import me.jakejmattson.discordkt.arguments.BooleanArg
import me.jakejmattson.discordkt.commands.subcommand

fun ownerCommands(
    config: Config,
    bot: DiscordBot
) = subcommand(OWNER_COMMAND_NAME) {
    sub("start", "Starts the game emulation") {
        execute {
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
            bot.stopGame()
            discord.kord.editPresence {}

            respond("Game emulation stopped.")
        }
    }

    sub("lock-input", "Only allows user input from owners, blocks any other input") {
        execute(BooleanArg("lock", description = "true to lock, false to unlock")) {
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
            val (activate, sound) = args

            with(bot) {
                if (activate) activateLocalDisplay(sound) else deactivateLocalDisplay()
            }

            val activateVerb = if (activate) "Activated" else "Deactivated"
            val soundVerb = if (sound) "with" else "without"
            respond("$activateVerb local display $soundVerb sound.")
        }
    }
}

const val OWNER_COMMAND_NAME = "owner"
