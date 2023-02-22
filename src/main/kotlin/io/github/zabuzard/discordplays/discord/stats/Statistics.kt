package io.github.zabuzard.discordplays.discord.stats

import dev.kord.common.entity.Snowflake
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.annotations.Service
import me.jakejmattson.discordkt.dsl.edit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class Statistics(private val config: Config) {
    private var consumers = emptyList<StatisticsConsumer>()
    private val statsService = Executors.newSingleThreadScheduledExecutor()

    private var gameStartedAt: Instant? = null
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
            0,
            1,
            TimeUnit.MINUTES
        )
    }

    fun addStatisticsConsumer(consumer: StatisticsConsumer) {
        consumers += consumer
    }

    fun removeStatisticsConsumer(consumer: StatisticsConsumer) {
        consumers -= consumer
    }

    fun onGameStarted() {
        gameStartedAt = Clock.System.now()
    }

    fun onGameStopped() {
        gameStartedAt = null
    }

    fun onUserInput(userInput: UserInput) {
        synchronized(userToInputCount) {
            userInput.user.id.let {
                userToInputCount[it] = (userToInputCount[it] ?: 0) + 1
                userToName[it] = userInput.user.username
            }
        }
        totalInputCount++
    }

    private fun computeStats() {
        val runningSince = if (gameStartedAt != null) Clock.System.now() - gameStartedAt!! else null

        val uniqueUserCount = userToInputCount.size

        val userIdToInputSorted: List<Pair<Snowflake, Int>>
        synchronized(userToInputCount) {
            userIdToInputSorted =
                userToInputCount.filterNot { (id, _) -> id in config.bannedUsers }
                    .toList().sortedByDescending { it.second }

            config.edit {
                userToInputCount = this@Statistics.userToInputCount.mapKeys { (id, _) ->
                    UserSnapshot(id, userToName[id]!!)
                }.toList()
            }
        }

        val topUserOverview = userIdToInputSorted.take(20).joinToString("\n") { (id, inputCount) ->
            "* ${userToName[id]} - $inputCount"
        }

        val stats = """
            Running since $runningSince.
            Received $totalInputCount inputs by $uniqueUserCount users.
            Top players:
            $topUserOverview
        """.trimIndent()

        consumers.forEach({ consumer: StatisticsConsumer -> consumer.acceptStatistics(stats) }.logAllExceptions())
    }
}
