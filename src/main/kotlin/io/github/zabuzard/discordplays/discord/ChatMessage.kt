package io.github.zabuzard.discordplays.discord

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message

data class ChatMessage(
    val author: Member,
    val content: String
)

fun Message.toChatMessage() = ChatMessage(member!!, contentDisplay)
