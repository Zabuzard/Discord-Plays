package io.github.zabuzard.discordplays.discord.commands

import dev.kord.core.entity.Guild
import kotlinx.coroutines.flow.toList

object CommandExtensions {
    suspend fun Guild.mentionCommandOrNull(commandName: String, fullCommandQuery: String) =
        kord.getGuildApplicationCommands(id).toList().find { it.name == commandName }
            ?.id
            ?.let { "</$fullCommandQuery:$it>" }
}
