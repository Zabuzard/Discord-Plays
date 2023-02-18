package io.github.zabuzard.discordplays.emulation

import eu.rekawek.coffeegb.Gameboy
import eu.rekawek.coffeegb.GameboyOptions
import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.memory.cart.Cartridge
import eu.rekawek.coffeegb.serial.SerialEndpoint
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.discord.util.Extensions.logAllExceptions
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Graphics
import java.io.File
import java.util.concurrent.Executors

@Service
class Emulator(
    private val config: Config
) {
    private val emulationService = Executors.newSingleThreadExecutor()

    private val display = GraphicsDisplay()
    private val controller = ClickController()
    private val soundOutput = VolumeControlSoundOutput()

    private var gameboy: Gameboy? = null

    @Synchronized
    fun start() {
        require(gameboy == null) { "Cannot start emulation, it is already running" }

        val options = GameboyOptions(File(config.romPath), emptyList(), emptyList())
        val cartridge = Cartridge(options)
        val serialEndpoint = SerialEndpoint.NULL_ENDPOINT

        muteSound()

        gameboy = Gameboy(options, cartridge, display, controller, soundOutput, serialEndpoint)
            .also { emulationService.submit(it.logAllExceptions()) }
    }

    @Synchronized
    fun stop() {
        requireNotNull(gameboy) { "Cannot stop emulation, none is running" }

        gameboy!!.stop()
        gameboy = null
    }

    suspend fun clickButton(button: ButtonListener.Button) =
        controller.clickButton(button)

    fun pressButton(button: ButtonListener.Button) = controller.pressButton(button)
    fun releaseButton(button: ButtonListener.Button) = controller.releaseButton(button)

    fun muteSound() = soundOutput.mute()
    fun activateSound() = soundOutput.fullVolume()

    fun render(g: Graphics, scale: Double = 2.0, x: Int = 0, y: Int = 0) {
        display.render(g, scale, x, y)
    }

    companion object {
        const val RESOLUTION_WIDTH = 160
        const val RESOLUTION_HEIGHT = 144
    }
}
