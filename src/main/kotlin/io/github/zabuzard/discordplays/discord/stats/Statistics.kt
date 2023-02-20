package io.github.zabuzard.discordplays.discord.stats

import dev.kord.core.entity.User
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.discord.UserInput
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jakejmattson.discordkt.annotations.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class Statistics {
    private val consumers = mutableListOf<StatisticsConsumer>()
    private val statsService = Executors.newSingleThreadScheduledExecutor()

    private var gameStartedAt: Instant? = null
    private val userToInputCount = mutableMapOf<User, Int>()
    private var totalInputCount = 0

    init {
        statsService.scheduleAtFixedRate(
            this::computeStats.logAllExceptions(),
            0,
            1,
            TimeUnit.MINUTES
        )
    }

    @Synchronized
    fun addStatisticsConsumer(consumer: StatisticsConsumer) {
        consumers += consumer
    }

    @Synchronized
    fun removeStatisticsConsumer(consumer: StatisticsConsumer) {
        consumers -= consumer
    }

    @Synchronized
    fun onGameStarted() {
        gameStartedAt = Clock.System.now()
    }

    @Synchronized
    fun onGameStopped() {
        gameStartedAt = null
        userToInputCount.clear()
        totalInputCount = 0
    }

    @Synchronized
    fun onUserInput(userInput: UserInput) {
        userToInputCount[userInput.user] = (userToInputCount[userInput.user] ?: 0) + 1
        totalInputCount++
    }

    @Synchronized
    private fun computeStats() {
        val runningSince = if (gameStartedAt != null) Clock.System.now() - gameStartedAt!! else null

        val uniqueUserCount = userToInputCount.size
        val userToInputSorted = userToInputCount.toList().sortedByDescending { it.second }

        val topUserOverview = userToInputSorted.take(20).joinToString("\n") { (user, inputCount) ->
            "* ${user.username} - $inputCount"
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
