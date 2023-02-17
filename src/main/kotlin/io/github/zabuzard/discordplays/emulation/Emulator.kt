package io.github.zabuzard.discordplays.emulation

import eu.rekawek.coffeegb.Gameboy
import eu.rekawek.coffeegb.GameboyOptions
import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.gui.SwingController
import eu.rekawek.coffeegb.gui.SwingDisplay
import eu.rekawek.coffeegb.memory.cart.Cartridge
import eu.rekawek.coffeegb.serial.SerialEndpoint
import io.github.zabuzard.discordplays.ClickController
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.ImageDisplay
import io.github.zabuzard.discordplays.VolumeControlSoundOutput
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Dimension
import java.awt.Graphics
import java.io.File
import java.util.*
import javax.swing.JFrame

@Service
class Emulator(
    private val config: Config,
    private val clickController: ClickController,
    private val imageDisplay: ImageDisplay
) {
    init {
        if (config.localOnly) {
            start()
        }
    }

    private lateinit var gameboy: Gameboy

    lateinit var title: String

    fun start() {
        val romPath = config.romPath

        val options = GameboyOptions(File(romPath), listOf(), listOf())
        val cartridge = Cartridge(options)
        val serialEndpoint = SerialEndpoint.NULL_ENDPOINT

        val display = if (config.localOnly) SwingDisplay(2) else imageDisplay

        val controller = if (config.localOnly) SwingController(Properties()) else clickController

        val soundOutput = VolumeControlSoundOutput()
        soundOutput.mute()

        if (config.localOnly) {
            val swingDisplay = display as SwingDisplay
            val swingController = controller as SwingController

            swingDisplay.preferredSize = Dimension(160 * 2, 144 * 2)
            val mainWindow = JFrame(cartridge.title)
            mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            mainWindow.setLocationRelativeTo(null)
            mainWindow.contentPane = swingDisplay
            mainWindow.isResizable = false
            mainWindow.isVisible = true
            mainWindow.pack()
            mainWindow.addKeyListener(swingController)
            Thread(swingDisplay).start()
        }

        title = cartridge.title
        gameboy = Gameboy(options, cartridge, display, controller, soundOutput, serialEndpoint)

        Thread(gameboy).start()
    }

    fun stop() {
        gameboy.stop()
    }

    suspend fun clickButton(button: ButtonListener.Button) =
        clickController.clickButton(button)

    fun render(g: Graphics, scale: Double = 2.0, x: Int = 0, y: Int = 0) {
        imageDisplay.render(g, scale, x, y)
    }
}
