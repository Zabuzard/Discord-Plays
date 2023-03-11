package io.github.zabuzard.discordplays.discord.stats

import io.github.oshai.KotlinLogging
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.formatted
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class Statistics(private val config: Config) {
    private val service = Executors.newSingleThreadScheduledExecutor()
    private var consumers = emptyList<StatisticsConsumer>()

    private var gameActiveLastHeartbeat: Instant? = null
    private val userToInputCount: MutableMap<Long, Long>
    private val userToName: MutableMap<Long, String>
    private var totalInputCount: Long

    init {
        userToInputCount = config.userToInputCount.toMap().mapKeys { it.key.id }.toMutableMap()
        userToName =
            config.userToInputCount.associate { (user, _) -> user.id to user.name }.toMutableMap()
        totalInputCount = config.userToInputCount.sumOf { it.second }

        service.scheduleAtFixedRate(
            { logAllExceptions { computeStats() } },
            0,
            1,
            TimeUnit.MINUTES
        )
    }

    fun addStatisticsConsumer(consumer: StatisticsConsumer) {
        logger.debug { "Adding stats consumer ${consumer.javaClass.name}" }
        consumers += consumer
    }

    fun removeStatisticsConsumer(consumer: StatisticsConsumer) {
        logger.debug { "Removing stats consumer ${consumer.javaClass.name}" }
        consumers -= consumer
    }

    fun onGameResumed() {
        logger.debug { "Resuming game stats" }
        gameActiveLastHeartbeat = Clock.System.now()
    }

    fun onGamePaused() {
        logger.debug { "Pausing game stats" }
        gameActiveLastHeartbeat?.let {
            val playtimeSinceLastHeartbeat = Clock.System.now() - it
            config.edit {
                playtimeMs += playtimeSinceLastHeartbeat.inWholeMilliseconds
            }
        }

        gameActiveLastHeartbeat = null
    }

    fun onUserInput(userInput: UserInput) {
        userInput.user.idLong.let {
            userToInputCount[it] = (userToInputCount[it] ?: 0) + 1
            userToName[it] = userInput.user.name
        }

        totalInputCount++
    }

    fun clearStats() {
        userToInputCount.clear()
        totalInputCount = 0

        config.edit {
            playtimeMs = 0
            userToInputCount = emptyList()
        }
    }

    private fun computeStats() {
        val uniqueUserCount = userToInputCount.size

        val userIdToInputSorted =
            userToInputCount.filterNot { (id, _) -> id in config.bannedUsers }
                .toList().sortedByDescending { it.second }

        config.edit {
            userToInputCount = this@Statistics.userToInputCount.mapKeys { (id, _) ->
                UserSnapshot(id, userToName[id]!!)
            }.toList()

            gameActiveLastHeartbeat?.let {
                val now = Clock.System.now()
                val playtimeSinceLastHeartbeat = now - it
                gameActiveLastHeartbeat = now
                playtimeMs += playtimeSinceLastHeartbeat.inWholeMilliseconds
            }
        }

        if (gameActiveLastHeartbeat == null) {
            return logger.debug { "Skip sending statistics update, game paused" }
        }

        val topUserOverview =
            userIdToInputSorted.take(20).joinToString("\n") { (id, inputCount) ->
                "|* ${userToName[id]} - ${inputCount.formatted()}"
            }

        val stats = """
            |Playtime: ${config.playtimeMs.milliseconds.formatted()}
            |Received ${totalInputCount.formatted()} inputs by ${uniqueUserCount.formatted()} users from ${config.hosts.size} communities.
            |Top players:
             $topUserOverview
        """.trimMargin()

        logger.debug { "Sending statistics update" }
        consumers.forEach {
            CompletableFuture.runAsync { logAllExceptions { it.acceptStatistics(stats) } }
        }
    }
}

private val logger = KotlinLogging.logger {}

private fun Long.formatted() = when (this) {
    in 0 until 1_000 -> toString()
    in 1_000 until 10_000 -> "%,.2fk".format(Locale.ENGLISH, this / 1_000.0)
    in 10_000 until 100_000 -> "%,.1fk".format(Locale.ENGLISH, this / 1_000.0)
    in 100_000 until 1_000_000 -> "${this / 1_000}k"
    else -> "%,.2fM".format(Locale.ENGLISH, this / 1_000_000.0)
}

private fun Int.formatted() = toLong().formatted()
