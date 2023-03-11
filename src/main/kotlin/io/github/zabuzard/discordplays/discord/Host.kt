package io.github.zabuzard.discordplays.discord

import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message

data class Host(val guild: Guild, val mirrorMessage: Message, val chatDescriptionMessage: Message) {
    fun toHostId() = HostId(guild.idLong, mirrorMessage.toId(), chatDescriptionMessage.toId())
}

@Serializable
data class HostId(
    val guildId: Long,
    val mirrorMessageId: MessageId,
    val chatDescriptionMessageId: MessageId
) {
    fun toHost(jda: JDA): Host? {
        val guild = jda.getGuildById(guildId)
        val mirrorMessage = mirrorMessageId.toMessage(jda)
        val chatDescriptionMessage = chatDescriptionMessageId.toMessage(jda)

        if (guild == null || mirrorMessage == null || chatDescriptionMessage == null) {
            return null
        }
        return Host(guild, mirrorMessage, chatDescriptionMessage)
    }
}
