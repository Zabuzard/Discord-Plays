package io.github.zabuzard.discordplays.discord

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.KotlinLogging
import io.github.oshai.withLoggingContext
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.setAuthor
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.internal.requests.CompletedRestAction
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DiscordBot(
    jda: JDA,
    private val config: Config,
    private val emulator: Emulator,
    private val streamRenderer: StreamRenderer,
    private val overlayRenderer: OverlayRenderer,
    private val localDisplay: LocalDisplay,
    private val statistics: Statistics,
    private val autoSaver: AutoSaver,
    private val frameRecorder: FrameRecorder
) : StreamConsumer, StatisticsConsumer {
    private val hostService = Executors.newFixedThreadPool(10)
    private var guildToHost = emptyMap<Guild, Host>()
    private val hostToFailureRetry =
        Collections.synchronizedMap(mutableMapOf<Host, HostFailureRetry>())

    private val userInputCache: Cache<Long, Instant> = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(10.seconds.toJavaDuration())
        .build()
    private val userChatCache: Cache<Long, Instant> = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(10.seconds.toJavaDuration())
        .build()

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

        loadHosts(jda)
    }

    fun startGame(jda: JDA) {
        logger.info { "Starting game" }
        userInputLockedToOwners = true
        loadHosts(jda)

        emulator.start()
        streamRenderer.start()
        statistics.onGameResumed()
        autoSaver.start(this, emulator, jda)
        frameRecorder.start()
        gameCurrentlyRunning = true
    }

    fun stopGame() {
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
        logger.info {
            "Adding host ${host.guild.id} (${host.guild.name}), mirrorMessage (${host.mirrorMessage.toId()}), " +
                "chatDescriptionMessage (${host.chatDescriptionMessage.toId()})"
        }
        guildToHost += host.guild to host
        saveHosts()
    }

    fun hasHost(guild: Guild): Boolean {
        return guildToHost.containsKey(guild)
    }

    fun removeHost(guild: Guild) {
        logger.info { "Removing host ${guild.id} (${guild.name})" }
        guildToHost -= guild
        saveHosts()
    }

    enum class UserInputResult {
        ACCEPTED,
        RATE_LIMITED,
        BLOCKED_NON_OWNER,
        USER_BANNED,
        GAME_OFFLINE
    }

    fun onUserInput(input: UserInput): UserInputResult {
        val userId = input.user.idLong
        val userName = input.user.name

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

    fun onChatMessage(message: ChatMessage) {
        val author = message.author
        val lastMessage = userChatCache.getIfPresent(author.idLong)

        val now = Clock.System.now()
        val timeSinceLastMessage = if (lastMessage == null) userChatRateLimit else now - lastMessage
        if (timeSinceLastMessage < userChatRateLimit) {
            return
        }
        userChatCache.put(author.idLong, now)

        overlayRenderer.recordChatMessage(message)

        forAllHosts {
            if (it.guild.idLong == author.guild.idLong) {
                return@forAllHosts CompletedRestAction<Void>(it.guild.jda, null)
            }

            val guild = author.guild.name
            val embed = EmbedBuilder()
                .setAuthor(author)
                .setDescription(message.content)
                .setFooter("from $guild")
                .build()

            it.chatDescriptionMessage.channel.sendMessageEmbeds(embed)
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

    fun sendChatMessage(message: String) {
        logger.info { "Sending chat message: $message" }
        forAllHosts {
            it.chatDescriptionMessage.channel.sendMessage(message)
                .flatMap(Message::pin)
        }
    }

    fun setCommunityMessage(guild: Guild, message: String?) {
        if (message != null) {
            require(message.isNotEmpty()) {
                "Cannot send an empty community message."
            }
        }

        val host = guildToHost[guild]
        requireNotNull(host) { "Could not find any stream hosted in this server." }

        logger.info { "Set community message for ${host.guild.name}: $message" }
        val embed = if (message == null) {
            emptyList()
        } else {
            listOf(
                EmbedBuilder().setDescription(message).build()
            )
        }

        host.mirrorMessage.editMessageEmbeds(embed).queue()
    }

    override fun acceptFrame(frame: BufferedImage) {
        lastFrame = frame
    }

    override fun acceptGif(gif: ByteArray) {
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

    private fun sendOfflineImage() =
        sendStreamFile("stream.png") { javaClass.getResourceAsStream(OFFLINE_COVER_RESOURCE)!! }

    private fun sendStreamFile(name: String, dataProducer: () -> InputStream) {
        forAllHosts {
            it.mirrorMessage.editMessageAttachments(
                FileUpload.fromData(dataProducer(), name)
            )
        }
    }

    override fun acceptStatistics(stats: String) {
        forAllHosts { it ->
            val embed = EmbedBuilder()
                .setTitle("Stats")
                .setDescription(stats)
                .build()

            it.chatDescriptionMessage.editMessageEmbeds(embed)
        }
    }

    private fun saveHosts() {
        config.edit { hosts = guildToHost.values.map(Host::toHostId).toSet() }
    }

    private fun loadHosts(jda: JDA) {
        guildToHost = config.hosts.mapNotNull { it.toHost(jda) }.associateBy { it.guild }
        saveHosts()
    }

    private fun forAllHosts(hostAction: (Host) -> RestAction<*>) {
        guildToHost.values.map {
            val failureRetry = hostToFailureRetry[it]
            if (failureRetry != null && failureRetry.retryWhen > Clock.System.now()) {
                return@map CompletableFuture.completedFuture(null)
            }

            hostService.submit {
                try {
                    hostAction(it).complete()

                    if (failureRetry != null) {
                        logger.info("Host (${it.guild.name}) succeeded after previously ${failureRetry.currentAttempt} failed attempts")
                        hostToFailureRetry -= it
                    }
                } catch (e: Exception) {
                    if (e is ErrorResponseException && e.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) {
                        removeHost(it.guild)
                    } else {
                        val nextFailureRetry = failureRetry?.nextRetry() ?: HostFailureRetry()
                        hostToFailureRetry[it] = nextFailureRetry

                        val delay = getDelayForAttempt(nextFailureRetry.currentAttempt)
                        logger.error(
                            "Unknown error with host ${it.guild.name}, next attempt delayed for $delay",
                            it,
                            e
                        )
                    }
                }
            }
        }.forEach { it.get() }
    }

    companion object {
        const val OFFLINE_COVER_RESOURCE = "/currently_offline.png"
        const val STARTING_SOON_COVER_RESOURCE = "/starting_soon.png"
    }
}

private val logger = KotlinLogging.logger {}

private val userInputRateLimit = 0.5.seconds
private val userChatRateLimit = 1.seconds
private const val MESSAGE_NOT_FOUND_ERROR = "UnknownMessage"

private val pauseAfterNoInputFor = 2.minutes
private const val PAUSED_MESSAGE = "Game is paused, press any key to continue"

private data class HostFailureRetry(
    val currentAttempt: Int = 0,
    val retryWhen: Instant = Clock.System.now() + getDelayForAttempt(0)
) {
    fun nextRetry(): HostFailureRetry = (currentAttempt + 1).let {
        HostFailureRetry(it, Clock.System.now() + getDelayForAttempt(it))
    }
}

private fun getDelayForAttempt(attempt: Int) = when (attempt) {
    0 -> 2.seconds
    1 -> 10.seconds
    2 -> 30.seconds
    3 -> 1.minutes
    else -> 5.minutes
}
