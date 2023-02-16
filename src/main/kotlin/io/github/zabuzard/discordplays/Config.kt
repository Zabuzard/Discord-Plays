package io.github.zabuzard.discordplays

import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data

@Serializable
data class Config(
    var romPath: String = "Pokemon Red TPP.gb"
) : Data()
