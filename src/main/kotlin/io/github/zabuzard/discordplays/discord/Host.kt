package io.github.zabuzard.discordplays.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.serialization.Serializable

data class Host(val guild: Guild, val mirrorMessage: Message, val chatDescriptionMessage: Message) {
    fun toHostId() = HostId(guild.id, mirrorMessage.toId(), chatDescriptionMessage.toId())
}

@Serializable
data class HostId(
    val guildId: Snowflake,
    val mirrorMessageId: MessageId,
    val chatDescriptionMessageId: MessageId
) {
    suspend fun toHost(kord: Kord): Host? {
        val guild = kord.getGuildOrNull(guildId)
        val mirrorMessage = mirrorMessageId.toMessage(kord)
        val chatDescriptionMessage = chatDescriptionMessageId.toMessage(kord)

        if (guild == null || mirrorMessage == null || chatDescriptionMessage == null) {
            return null
        }
        return Host(guild, mirrorMessage, chatDescriptionMessage)
    }
}
