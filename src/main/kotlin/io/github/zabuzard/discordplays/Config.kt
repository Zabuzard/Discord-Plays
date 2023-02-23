package io.github.zabuzard.discordplays

import dev.kord.common.entity.Snowflake
import io.github.zabuzard.discordplays.discord.HostId
import io.github.zabuzard.discordplays.discord.stats.UserSnapshot
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data

@Serializable
data class Config(
    var romPath: String = "Pokemon Red TPP.gb",
    var gameTitle: String = "Pokemon Red",
    var owners: Set<Snowflake> = setOf(Snowflake(157994153806921728u)),
    var bannedUsers: Set<Snowflake> = setOf(),
    var hosts: Set<HostId> = setOf(),
    var autoSaveRemindAt: LocalTime = LocalTime(13, 0),
    var userToInputCount: List<Pair<UserSnapshot, Int>> = emptyList(),
    var playtimeMs: Long = 0,
    var recordingPath: String = "recording"
) : Data()
