package io.github.zabuzard.discordplays.emulation

import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import eu.rekawek.coffeegb.controller.Controller
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal class ClickController : Controller {
    private lateinit var buttonListener: ButtonListener

    override fun setButtonListener(listener: ButtonListener) {
        buttonListener = listener
    }

    suspend fun clickButton(button: Button) {
        buttonListener.onButtonPress(button)
        delay(clickDuration)
        buttonListener.onButtonRelease(button)
    }

    fun pressButton(button: Button) = buttonListener.onButtonPress(button)
    fun releaseButton(button: Button) = buttonListener.onButtonRelease(button)
}

private val clickDuration = 250.milliseconds
