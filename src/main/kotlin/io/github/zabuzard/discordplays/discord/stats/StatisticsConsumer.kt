package io.github.zabuzard.discordplays.discord.stats

interface StatisticsConsumer {
    fun acceptStatistics(stats: String)
}
