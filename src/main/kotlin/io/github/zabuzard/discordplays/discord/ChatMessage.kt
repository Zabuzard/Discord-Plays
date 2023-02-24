package io.github.zabuzard.discordplays.discord

import dev.kord.core.entity.Message
import dev.kord.core.entity.User

data class ChatMessage(
    val author: User,
    val content: String
)

fun Message.toChatMessage() = ChatMessage(author!!, content)
