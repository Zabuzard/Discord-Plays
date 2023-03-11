package io.github.zabuzard.discordplays.discord

import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

@Serializable
data class MessageId(val channelId: Long, val messageId: Long) {
    fun toMessage(jda: JDA) =
        jda.getChannelById(MessageChannel::class.java, channelId)
            ?.retrieveMessageById(messageId)
            ?.onErrorMap { null }
            ?.complete()
}

fun Message.toId() = MessageId(channel.idLong, idLong)
