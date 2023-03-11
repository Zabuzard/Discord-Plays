package io.github.zabuzard.discordplays.discord.stats

import kotlinx.serialization.Serializable

@Serializable
data class UserSnapshot(val id: Long, val name: String)
