package io.github.zabuzard.discordplays

import io.github.zabuzard.discordplays.discord.HostId
import io.github.zabuzard.discordplays.discord.stats.UserSnapshot
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

@Serializable
data class Config(
    var romPath: String = "Pokemon Red TPP.gb",
    var gameTitle: String = "Pokemon Red",
    var owners: Set<Long> = setOf(157994153806921728),
    var bannedUsers: Set<Long> = setOf(),
    var hosts: Set<HostId> = setOf(),
    var autoSaveRemindAt: LocalTime = LocalTime(13, 0),
    var userToInputCount: List<Pair<UserSnapshot, Long>> = emptyList(),
    var playtimeMs: Long = 0,
    var recordingPath: String = "recording",
    var font: String = "Sans Serif"
) {

    fun edit(edits: Config.() -> Unit) {
        edits(this)
        save()
    }

    private fun save() {
        path.toAbsolutePath().parent?.createDirectories()
        Files.writeString(path, serializer.encodeToString(this))
    }

    companion object {
        fun loadOrDefault(): Config = if (Files.exists(path)) {
            serializer.decodeFromString(Files.readString(path))
        } else {
            Config()
        }.also { it.save() }
    }
}

private val path: Path = Path.of("config.json")

private val serializer: Json
    get() = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
