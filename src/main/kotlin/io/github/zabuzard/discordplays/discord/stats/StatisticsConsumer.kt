package io.github.zabuzard.discordplays.discord.stats

interface StatisticsConsumer {
    suspend fun acceptStatistics(stats: String)
}
