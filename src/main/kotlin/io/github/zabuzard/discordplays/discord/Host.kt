package io.github.zabuzard.discordplays.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.serialization.Serializable

data class Host(val guild: Guild, val streamMessage: Message, val chatDescriptionMessage: Message) {
    fun toHostId() = HostId(guild.id, streamMessage.toId(), chatDescriptionMessage.toId())
}

@Serializable
data class HostId(
    val guildId: Snowflake,
    val streamMessageId: MessageId,
    val chatDescriptionMessageId: MessageId
) {
    suspend fun toHost(kord: Kord): Host? {
        val guild = kord.getGuildOrNull(guildId)
        val streamMessage = streamMessageId.toMessage(kord)
        val chatDescriptionMessage = chatDescriptionMessageId.toMessage(kord)

        if (guild == null || streamMessage == null || chatDescriptionMessage == null) {
            return null
        }
        return Host(guild, streamMessage, chatDescriptionMessage)
    }
}
