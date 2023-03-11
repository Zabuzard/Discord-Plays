package io.github.zabuzard.discordplays.discord.commands

import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.discord.toChatMessage
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

fun JDA.onChatMessage(
    config: Config,
    bot: DiscordBot
) {
    addEventListener(object : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            if (event.author.isBot ||
                event.isWebhookMessage ||
                event.author.idLong in config.bannedUsers
            ) {
                return
            }

            if (config.hosts.none { it.chatDescriptionMessageId.channelId == event.channel.idLong }) return

            bot.onChatMessage(event.message.toChatMessage())
        }
    })
}
