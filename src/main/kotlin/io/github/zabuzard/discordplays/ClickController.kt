package io.github.zabuzard.discordplays

import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import eu.rekawek.coffeegb.controller.Controller
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.annotations.Service

@Service
class ClickController: Controller {
    private lateinit var buttonListener: ButtonListener

    override fun setButtonListener(listener: ButtonListener) {
        buttonListener = listener
    }

    suspend fun clickButton(button: Button) {
        buttonListener.onButtonPress(button)
        delay(250)
        buttonListener.onButtonRelease(button)
    }
}
