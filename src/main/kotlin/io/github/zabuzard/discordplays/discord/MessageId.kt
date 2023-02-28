package io.github.zabuzard.discordplays.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.serialization.Serializable

@Serializable
data class MessageId(val channelId: Snowflake, val messageId: Snowflake) {
    suspend fun toMessage(kord: Kord) =
        kord.getChannelOf<MessageChannel>(channelId, strategy = EntitySupplyStrategy.rest)
            ?.getMessageOrNull(messageId)
}

fun Message.toId() = MessageId(channelId, id)
