package io.github.zabuzard.discordplays.discord.commands

import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.rest.builder.message.modify.embed

object CommandExtensions {
    fun UserMessageModifyBuilder.clearEmbeds() {
        embed { description = "" }
        embeds?.clear()
    }
}
