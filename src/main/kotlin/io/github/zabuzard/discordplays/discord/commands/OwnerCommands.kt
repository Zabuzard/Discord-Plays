package io.github.zabuzard.discordplays.discord.commands

import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import kotlinx.datetime.Clock
import me.jakejmattson.discordkt.arguments.BooleanArg
import me.jakejmattson.discordkt.commands.subcommand

fun ownerCommands(
    config: Config,
    bot: DiscordBot
) = subcommand("owner") {
    sub("start", "Starts the game emulation") {
        execute {
            bot.startGame()
            discord.kord.editPresence {
                playing(config.gameTitle)
                since = Clock.System.now()
            }

            respond("Game emulation started. Stream is ready, use `/host stream` to host it.")
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
}
