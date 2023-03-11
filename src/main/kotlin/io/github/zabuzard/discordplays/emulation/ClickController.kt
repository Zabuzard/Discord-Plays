package io.github.zabuzard.discordplays.emulation

import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import eu.rekawek.coffeegb.controller.Controller
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class ClickController : Controller {
    private lateinit var buttonListener: ButtonListener
    private val service = Executors.newSingleThreadScheduledExecutor()

    override fun setButtonListener(listener: ButtonListener) {
        buttonListener = listener
    }

    fun clickButton(button: Button): ScheduledFuture<*> {
        buttonListener.onButtonPress(button)
        return service.schedule(
            { buttonListener.onButtonRelease(button) },
            250,
            TimeUnit.MILLISECONDS
        )
    }

    fun pressButton(button: Button) = buttonListener.onButtonPress(button)
    fun releaseButton(button: Button) = buttonListener.onButtonRelease(button)
}
