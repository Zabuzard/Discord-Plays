package io.github.zabuzard.discordplays.discord.stats

import dev.kord.common.entity.Snowflake
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.annotations.Service
import me.jakejmattson.discordkt.dsl.edit
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@Service
class Statistics(private val config: Config) {
    private var consumers = emptyList<StatisticsConsumer>()
    private val statsService = Executors.newSingleThreadScheduledExecutor()

    private var gameActiveLastHeartbeat: Instant? = null
    private val userToInputCount: MutableMap<Snowflake, Int>
    private val userToName: MutableMap<Snowflake, String>
    private var totalInputCount: Int

    init {
        userToInputCount = config.userToInputCount.toMap().mapKeys { it.key.id }.toMutableMap()
        userToName =
            config.userToInputCount.associate { (user, _) -> user.id to user.name }.toMutableMap()
        totalInputCount = config.userToInputCount.sumOf { it.second }

        statsService.scheduleAtFixedRate(
            this::computeStats.logAllExceptions(),
            1,
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
        synchronized(this) {
            userInput.user.id.let {
                userToInputCount[it] = (userToInputCount[it] ?: 0) + 1
                userToName[it] = userInput.user.username
            }
        }
        totalInputCount++
    }

    private fun computeStats() {
        val uniqueUserCount = userToInputCount.size

        val userIdToInputSorted: List<Pair<Snowflake, Int>>
        synchronized(this) {
            userIdToInputSorted =
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
        }

        val topUserOverview = userIdToInputSorted.take(20).joinToString("\n") { (id, inputCount) ->
            "|* ${userToName[id]} - $inputCount"
        }

        val stats = """
            |Playtime: ${config.playtimeMs.milliseconds}
            |Received $totalInputCount inputs by $uniqueUserCount users.
            |Top players:
            $topUserOverview
        """.trimMargin()

        logger.info { "Sending statistics update" }
        consumers.forEach({ consumer: StatisticsConsumer -> consumer.acceptStatistics(stats) }.logAllExceptions())
    }
}

private val logger = KotlinLogging.logger {}
