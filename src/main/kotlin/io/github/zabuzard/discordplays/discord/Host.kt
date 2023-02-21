package io.github.zabuzard.discordplays.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.Discord

data class Host(val guild: Guild, val streamMessage: Message, val chatDescriptionMessage: Message) {
    fun toHostId() = HostId(guild.id, streamMessage.toId(), chatDescriptionMessage.toId())
}

@Serializable
data class HostId(
    val guildId: Snowflake,
    val streamMessageId: MessageId,
    val chatDescriptionMessageId: MessageId
) {
    suspend fun toHost(discord: Discord): Host? {
        val guild = discord.kord.getGuild(guildId)
        val streamMessage = streamMessageId.toMessage(discord)
        val chatDescriptionMessage = chatDescriptionMessageId.toMessage(discord)

        if (guild == null || streamMessage == null || chatDescriptionMessage == null) {
            return null
        }
        return Host(guild, streamMessage, chatDescriptionMessage)
    }
}
