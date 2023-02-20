package io.github.zabuzard.discordplays.discord

import com.sksamuel.aedile.core.caffeineBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.message.modify.embed
import dev.kord.rest.request.KtorRequestException
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.commands.CommandExtensions.clearEmbeds
import io.github.zabuzard.discordplays.discord.stats.Statistics
import io.github.zabuzard.discordplays.discord.stats.StatisticsConsumer
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.local.LocalDisplay
import io.github.zabuzard.discordplays.stream.OverlayRenderer
import io.github.zabuzard.discordplays.stream.StreamConsumer
import io.github.zabuzard.discordplays.stream.StreamRenderer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.annotations.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Service
class DiscordBot(
    private val config: Config,
    private val emulator: Emulator,
    private val streamRenderer: StreamRenderer,
    private val overlayRenderer: OverlayRenderer,
    private val localDisplay: LocalDisplay,
    private val statistics: Statistics
) : StreamConsumer, StatisticsConsumer {
    private val hosts: MutableList<Host> =
        Collections.synchronizedList(mutableListOf<Host>())

    private val userInputCache = caffeineBuilder<Snowflake, Instant> {
        maximumSize = 1_000
        expireAfterWrite = (10).seconds
    }.build()

    var userInputLockedToOwners = false

    init {
        streamRenderer.addStreamConsumer(this)
        statistics.addStatisticsConsumer(this)
    }

    suspend fun startGame() {
        emulator.start()
        streamRenderer.start()
        statistics.onGameStarted()
    }

    fun stopGame() {
        emulator.stop()
        streamRenderer.stop()
        statistics.onGameStopped()
    }

    fun addHost(host: Host) {
        hosts += host
    }

    private fun removeHost(host: Host) {
        hosts -= host
    }

    enum class UserInputResult {
        ACCEPTED,
        RATE_LIMITED,
        BLOCKED_NON_OWNER
    }

    suspend fun onUserInput(input: UserInput): UserInputResult {
        val userId = input.user.id

        if (userInputLockedToOwners && userId.value !in config.owners) {
            return UserInputResult.BLOCKED_NON_OWNER
        }

        val lastInput = userInputCache.getIfPresent(userId)

        val now = Clock.System.now()
        val timeSinceLastInput = if (lastInput == null) userInputRateLimit else now - lastInput

        return when {
            timeSinceLastInput >= userInputRateLimit -> {
                overlayRenderer.recordUserInput(input)
                emulator.clickButton(input.button)

                userInputCache.put(userId, now)
                statistics.onUserInput(input)
                UserInputResult.ACCEPTED
            }

            else -> UserInputResult.RATE_LIMITED
        }
    }

    fun activateLocalDisplay(sound: Boolean) {
        localDisplay.activate(sound)
        streamRenderer.addStreamConsumer(localDisplay)
    }

    fun deactivateLocalDisplay() {
        streamRenderer.removeStreamConsumer(localDisplay)
        localDisplay.deactivate()
        emulator.muteSound()
    }

    fun setGlobalMessage(message: String?) {
        if (message == null) {
            streamRenderer.globalMessage = null
        } else {
            require(message.isNotEmpty()) {
                "Cannot send an empty global message."
            }
            streamRenderer.globalMessage = message
        }
    }

    suspend fun setCommunityMessage(channelId: Snowflake, message: String?) {
        if (message != null) {
            require(message.isNotEmpty()) {
                "Cannot send an empty community message."
            }
        }

        val host = hosts.find { it.streamMessage.channelId == channelId }
        requireNotNull(host) { "Could not find any stream hosted in this channel." }

        host.streamMessage.edit {
            clearEmbeds()

            if (message != null) {
                embed { description = message }
            }
        }
    }

    override fun acceptFrame(frame: BufferedImage) {
        // Only interested in gifs
    }

    override fun acceptGif(gif: ByteArray) {
        runBlocking {
            synchronized(hosts) {
                hosts.forEach {
                    launch {
                        try {
                            it.streamMessage.edit {
                                files?.clear()
                                addFile("image.gif", gif.toInputStream())
                            }
                        } catch (e: KtorRequestException) {
                            if (e.error?.code?.name == MESSAGE_NOT_FOUND_ERROR) {
                                removeHost(it)
                            } else {
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }

    override fun acceptStatistics(stats: String) {
        runBlocking {
            synchronized(hosts) {
                hosts.forEach {
                    launch {
                        try {
                            it.chatDescriptionMessage.edit {
                                embeds?.clear()
                                embed {
                                    title = "Stats"
                                    description = stats
                                }
                            }
                        } catch (e: KtorRequestException) {
                            if (e.error?.code?.name == MESSAGE_NOT_FOUND_ERROR) {
                                removeHost(it)
                            } else {
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }
}

private val userInputRateLimit = (1.5).seconds
private const val MESSAGE_NOT_FOUND_ERROR = "UnknownMessage"

private fun ByteArray.toInputStream() =
    ByteArrayInputStream(this)
