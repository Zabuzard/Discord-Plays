package io.github.zabuzard.discordplays.discord.stats

import dev.kord.common.entity.Snowflake
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.formatted
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(DelicateCoroutinesApi::class)
class Statistics(private val config: Config) {
    private var consumers = emptyList<StatisticsConsumer>()

    private var gameActiveLastHeartbeat: Instant? = null
    private val userToInputCount: MutableMap<Snowflake, Int>
    private val userToName: MutableMap<Snowflake, String>
    private var totalInputCount: Int

    init {
        userToInputCount = config.userToInputCount.toMap().mapKeys { it.key.id }.toMutableMap()
        userToName =
            config.userToInputCount.associate { (user, _) -> user.id to user.name }.toMutableMap()
        totalInputCount = config.userToInputCount.sumOf { it.second }

        GlobalScope.launch(Dispatchers.IO) { computeStatsRoutine() }
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
        userInput.user.id.let {
            userToInputCount[it] = (userToInputCount[it] ?: 0) + 1
            userToName[it] = userInput.user.username
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

    private suspend fun computeStatsRoutine() {
        while (coroutineContext.isActive) {
            logAllExceptions {
                computeStats()

                delay(1.minutes)
            }
        }
    }

    private suspend fun computeStats() {
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
                "|* ${userToName[id]} - $inputCount"
            }

        val stats = """
                    |Playtime: ${config.playtimeMs.milliseconds.formatted()}
                    |Received $totalInputCount inputs by $uniqueUserCount users.
                    |Top players:
                    $topUserOverview
                    """.trimMargin()

        logger.debug { "Sending statistics update" }
        coroutineScope {
            consumers.forEach {
                launch(logAllExceptions) { it.acceptStatistics(stats) }
            }
        }
    }
}

private val logger = KotlinLogging.logger {}
