package io.github.zabuzard.discordplays.discord.commands

import dev.kord.core.event.message.MessageCreateEvent
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.toChatMessage
import me.jakejmattson.discordkt.dsl.listeners

fun onChatMessage(
    config: Config,
    bot: DiscordBot
) = listeners {
    on<MessageCreateEvent> {
        val host = config.hosts.find { it.chatDescriptionMessageId.channelId == message.channelId }
            ?: return@on

        if (message.author?.isBot == true) {
            return@on
        }
        if (message.author?.id in config.bannedUsers) {
            return@on
        }

        bot.onChatMessage(message.toChatMessage(host.guildId))
    }
}
