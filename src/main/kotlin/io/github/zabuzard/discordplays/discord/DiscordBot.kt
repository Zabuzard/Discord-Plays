package io.github.zabuzard.discordplays.discord

import com.sksamuel.aedile.core.caffeineBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Guild
import dev.kord.rest.builder.message.modify.embed
import dev.kord.rest.request.KtorRequestException
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.asChannelProvider
import io.github.zabuzard.discordplays.Extensions.author
import io.github.zabuzard.discordplays.Extensions.clearEmbeds
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.Extensions.toByteArray
import io.github.zabuzard.discordplays.Extensions.toInputStream
import io.github.zabuzard.discordplays.discord.commands.AutoSaver
import io.github.zabuzard.discordplays.discord.stats.Statistics
import io.github.zabuzard.discordplays.discord.stats.StatisticsConsumer
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.local.FrameRecorder
import io.github.zabuzard.discordplays.local.LocalDisplay
import io.github.zabuzard.discordplays.stream.BannerRendering
import io.github.zabuzard.discordplays.stream.BannerRendering.Placement
import io.github.zabuzard.discordplays.stream.OverlayRenderer
import io.github.zabuzard.discordplays.stream.SCREEN_HEIGHT
import io.github.zabuzard.discordplays.stream.SCREEN_WIDTH
import io.github.zabuzard.discordplays.stream.StreamConsumer
import io.github.zabuzard.discordplays.stream.StreamRenderer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging
import mu.withLoggingContext
import java.awt.image.BufferedImage
import java.io.InputStream
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DiscordBot(
    kord: Kord,
    private val config: Config,
    private val emulator: Emulator,
    private val streamRenderer: StreamRenderer,
    private val overlayRenderer: OverlayRenderer,
    private val localDisplay: LocalDisplay,
    private val statistics: Statistics,
    private val autoSaver: AutoSaver,
    private val frameRecorder: FrameRecorder
) : StreamConsumer, StatisticsConsumer {
    private var guildToHost = emptyMap<Guild, Host>()

    private val userInputCache = caffeineBuilder<Snowflake, Instant> {
        maximumSize = 1_000
        expireAfterWrite = 10.seconds
    }.build()
    private val userChatCache = caffeineBuilder<Snowflake, Instant> {
        maximumSize = 1_000
        expireAfterWrite = 10.seconds
    }.build()

    var userInputLockedToOwners = false
        set(value) {
            logger.info { "User input locked to owners: $value" }
            field = value

            setGlobalMessage(if (value) "User input is currently locked" else null)
        }
    var gameCurrentlyRunning = false
        private set

    private var lastUserInputAt: Instant = Clock.System.now()
    private var isPaused: Boolean = false
    private lateinit var lastFrame: BufferedImage

    init {
        streamRenderer.addStreamConsumer(this)
        statistics.addStatisticsConsumer(this)

        runBlocking {
            loadHosts(kord)
        }
    }

    suspend fun startGame(kord: Kord) {
        logger.info { "Starting game" }
        userInputLockedToOwners = true
        loadHosts(kord)

        emulator.start()
        streamRenderer.start()
        statistics.onGameResumed()
        autoSaver.start(this, emulator, kord)
        frameRecorder.start()
        gameCurrentlyRunning = true
    }

    suspend fun stopGame() {
        logger.info { "Stopping game" }
        emulator.stop()
        streamRenderer.stop()
        statistics.onGamePaused()
        gameCurrentlyRunning = false
        autoSaver.stop()
        frameRecorder.stop()

        sendOfflineImage()
    }

    fun addHost(host: Host) {
        require(guildToHost[host.guild] == null) { "Only one host per guild allowed, first delete the existing host" }

        logger.info { "Adding host (${host.guild.name})" }
        guildToHost += host.guild to host
        saveHosts()
    }

    private fun removeHost(host: Host) {
        logger.info { "Removing host (${host.guild.name})" }
        guildToHost -= host.guild
        saveHosts()
    }

    enum class UserInputResult {
        ACCEPTED,
        RATE_LIMITED,
        BLOCKED_NON_OWNER,
        USER_BANNED,
        GAME_OFFLINE
    }

    suspend fun onUserInput(input: UserInput): UserInputResult {
        val userId = input.user.id
        val userName = input.user.username

        if (!gameCurrentlyRunning) {
            logger.debug { withLoggingContext("user" to userName) { "Blocked user input ($userName), game offline" } }
            return UserInputResult.GAME_OFFLINE
        }

        if (userInputLockedToOwners && userId !in config.owners) {
            logger.debug { withLoggingContext("user" to userName) { "Blocked user input ($userName), locked to owner" } }
            return UserInputResult.BLOCKED_NON_OWNER
        }

        if (userId in config.bannedUsers) {
            logger.debug { withLoggingContext("user" to userName) { "Blocked user input ($userName), banned user" } }
            return UserInputResult.USER_BANNED
        }

        val lastInput = userInputCache.getIfPresent(userId)

        val now = Clock.System.now()
        val timeSinceLastInput = if (lastInput == null) userInputRateLimit else now - lastInput

        return when {
            timeSinceLastInput >= userInputRateLimit -> {
                logger.debug { withLoggingContext("user" to userName) { "$userName pressed ${input.button}" } }
                overlayRenderer.recordUserInput(input)
                emulator.clickButton(input.button)

                userInputCache.put(userId, now)
                statistics.onUserInput(input)

                if (isPaused) {
                    logger.info { "Resuming paused game after inactivity" }
                    isPaused = false
                    statistics.onGameResumed()
                    frameRecorder.start()
                }
                lastUserInputAt = now
                UserInputResult.ACCEPTED
            }

            else -> UserInputResult.RATE_LIMITED.also {
                logger.debug { withLoggingContext("user" to userName) { "Blocked user input ($userName), rate limited" } }
            }
        }
    }

    suspend fun onChatMessage(message: ChatMessage) {
        val author = message.author
        val lastMessage = userChatCache.getIfPresent(author.id)

        val now = Clock.System.now()
        val timeSinceLastMessage = if (lastMessage == null) userChatRateLimit else now - lastMessage
        if (timeSinceLastMessage < userChatRateLimit) {
            return
        }
        userChatCache.put(author.id, now)

        overlayRenderer.recordChatMessage(message)

        forAllHosts {
            if (it.guild.id == author.guildId) {
                return@forAllHosts
            }

            val guild = author.getGuild().name
            it.chatDescriptionMessage.getChannel().createEmbed {
                author(author)
                description = message.content
                footer { text = "from $guild" }
            }
        }
    }

    fun activateLocalDisplay(sound: Boolean) {
        logger.info { "Activating local display, sound: $sound" }
        localDisplay.activate(sound)
    }

    fun deactivateLocalDisplay() {
        logger.info { "Deactivating local display" }
        localDisplay.deactivate()
        emulator.muteSound()
    }

    fun setGlobalMessage(message: String?) {
        if (message == null) {
            logger.info { "Clearing global message" }
            streamRenderer.globalMessage = null
        } else {
            require(message.isNotEmpty()) {
                "Cannot send an empty global message."
            }
            logger.info { "Set global message: $message" }
            streamRenderer.globalMessage = message
        }
    }

    suspend fun sendChatMessage(message: String) {
        logger.info { "Sending chat message: $message" }
        forAllHosts {
            it.chatDescriptionMessage.channel.createMessage(message).pin()
        }
    }

    suspend fun setCommunityMessage(guild: Guild, message: String?) {
        if (message != null) {
            require(message.isNotEmpty()) {
                "Cannot send an empty community message."
            }
        }

        val host = guildToHost[guild]
        requireNotNull(host) { "Could not find any stream hosted in this server." }

        logger.info { "Set community message for ${host.guild.name}: $message" }
        host.mirrorMessage.edit {
            clearEmbeds()

            if (message != null) {
                embed { description = message }
            }
        }
    }

    override suspend fun acceptFrame(frame: BufferedImage) {
        lastFrame = frame
    }

    override suspend fun acceptGif(gif: ByteArray) {
        if (Clock.System.now() - lastUserInputAt > pauseAfterNoInputFor) {
            if (!isPaused) {
                logger.info { "Pausing game due to inactivity" }
                isPaused = true
                statistics.onGamePaused()
                frameRecorder.stop()

                lastFrame.apply {
                    val g = createGraphics()
                    BannerRendering.renderBanner(
                        PAUSED_MESSAGE,
                        g,
                        config.font,
                        SCREEN_WIDTH,
                        SCREEN_HEIGHT,
                        Placement.CENTER
                    )
                }.toByteArray().let {
                    sendStreamFile("stream.png") { it.toInputStream() }
                }
            }
            return
        }

        sendStreamFile("image.gif") { gif.toInputStream() }
    }

    private suspend fun sendOfflineImage() =
        sendStreamFile("stream.png") { javaClass.getResourceAsStream(OFFLINE_COVER_RESOURCE)!! }

    private suspend fun sendStreamFile(name: String, dataProducer: () -> InputStream) {
        forAllHosts {
            it.mirrorMessage.edit {
                files?.clear()
                addFile(name, dataProducer.asChannelProvider())
            }
        }
    }

    override suspend fun acceptStatistics(stats: String) {
        forAllHosts {
            it.chatDescriptionMessage.edit {
                embeds?.clear()
                embed {
                    title = "Stats"
                    description = stats
                }
            }
        }
    }

    private fun saveHosts() {
        config.edit { hosts = guildToHost.values.map(Host::toHostId).toSet() }
    }

    private suspend fun loadHosts(kord: Kord) {
        guildToHost = config.hosts.mapNotNull { it.toHost(kord) }.associateBy { it.guild }
        saveHosts()
    }

    private suspend fun forAllHosts(consumer: suspend (Host) -> Unit) {
        coroutineScope {
            guildToHost.values.forEach {
                launch(logAllExceptions) {
                    try {
                        consumer(it)
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

    companion object {
        const val OFFLINE_COVER_RESOURCE = "/currently_offline.png"
        const val STARTING_SOON_COVER_RESOURCE = "/starting_soon.png"
    }
}

private val logger = KotlinLogging.logger {}

private val userInputRateLimit = 1.5.seconds
private val userChatRateLimit = 1.5.seconds
private const val MESSAGE_NOT_FOUND_ERROR = "UnknownMessage"

private val pauseAfterNoInputFor = 2.minutes
private const val PAUSED_MESSAGE = "Game is paused, press any key to continue"
