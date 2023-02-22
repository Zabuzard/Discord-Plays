package io.github.zabuzard.discordplays.discord.stats

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class UserSnapshot(val id: Snowflake, val name: String)
