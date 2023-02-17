package io.github.zabuzard.discordplays.discord

import com.sksamuel.aedile.core.caffeineBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.rest.request.KtorRequestException
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.stream.OverlayRenderer
import io.github.zabuzard.discordplays.stream.StreamConsumer
import io.github.zabuzard.discordplays.stream.StreamRenderer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.annotations.Service
import me.jakejmattson.discordkt.extensions.DiscordRegex.user
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Service
class DiscordBot(
    private val emulator: Emulator,
    private val streamRenderer: StreamRenderer,
    private val overlayRenderer: OverlayRenderer
) : StreamConsumer {
    private val targets: MutableList<Message> =
        Collections.synchronizedList(mutableListOf<Message>())

    private val userInputCache = caffeineBuilder<Snowflake, Instant> {
        maximumSize = 1_000
        expireAfterWrite = (10).seconds
    }.build()

    init {
        streamRenderer.addStreamConsumer(this)
    }

    suspend fun startGame() {
        emulator.start()
        streamRenderer.start()
    }

    fun stopGame() {
        emulator.stop()
        streamRenderer.stop()
    }

    fun addStreamTarget(target: Message) {
        targets += target
    }

    fun removeStreamTarget(target: Message) {
        targets -= target
    }

    enum class UserInputResult {
        ACCEPTED,
        RATE_LIMITED
    }

    suspend fun onUserInput(input: UserInput): UserInputResult {
        val userId = input.user.id
        val lastInput = userInputCache.getIfPresent(userId)

        val now = Clock.System.now()
        val timeSinceLastInput = if (lastInput == null) userInputRateLimit else now - lastInput

        return when {
            timeSinceLastInput >= userInputRateLimit -> {
                overlayRenderer.recordUserInput(input)
                emulator.clickButton(input.button)

                userInputCache.put(userId, now)
                UserInputResult.ACCEPTED
            }

            else -> UserInputResult.RATE_LIMITED
        }
    }

    override fun acceptFrame(frame: BufferedImage) {
        // Only interested in gifs
    }

    override fun acceptGif(gif: ByteArray) {
        runBlocking {
            targets.forEach {
                launch {
                    try {
                        it.edit {
                            files?.clear()
                            addFile("image.gif", gif.toInputStream())
                        }
                    } catch (e: KtorRequestException) {
                        if (e.error?.code?.name == "UnknownMessage") {
                            removeStreamTarget(it)
                        } else {
                            throw e
                        }
                    }
                }
            }
        }
    }
}

private val userInputRateLimit = (1.5).seconds

private fun ByteArray.toInputStream() =
    ByteArrayInputStream(this)
