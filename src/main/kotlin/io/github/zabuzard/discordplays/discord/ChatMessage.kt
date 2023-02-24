package io.github.zabuzard.discordplays.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message

data class ChatMessage(
    val author: Member,
    val content: String
)

suspend fun Message.toChatMessage(guildId: Snowflake) = ChatMessage(author!!.asMember(guildId), content)
