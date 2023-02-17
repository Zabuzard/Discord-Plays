package io.github.zabuzard.discordplays.discord

import dev.kord.core.entity.User
import eu.rekawek.coffeegb.controller.ButtonListener
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class UserInput(
    val user: User,
    val button: ButtonListener.Button,
    val sendAt: Instant = Clock.System.now()
)
