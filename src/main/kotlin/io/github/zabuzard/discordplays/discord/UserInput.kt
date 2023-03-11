package io.github.zabuzard.discordplays.discord

import eu.rekawek.coffeegb.controller.ButtonListener
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.dv8tion.jda.api.entities.User

data class UserInput(
    val user: User,
    val button: ButtonListener.Button,
    val sendAt: Instant = Clock.System.now()
)
