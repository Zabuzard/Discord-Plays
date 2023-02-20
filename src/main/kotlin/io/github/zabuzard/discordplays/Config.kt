package io.github.zabuzard.discordplays

import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data

@Serializable
data class Config(
    var romPath: String = "Pokemon Red TPP.gb",
    var gameTitle: String = "Pokemon Red",
    var owners: Set<ULong> = setOf(157994153806921728u),
    var bannedUsers: Set<ULong> = setOf()
) : Data()
