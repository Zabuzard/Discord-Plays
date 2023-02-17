package io.github.zabuzard.discordplays.emulation

import eu.rekawek.coffeegb.Gameboy
import eu.rekawek.coffeegb.GameboyOptions
import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.gui.SwingController
import eu.rekawek.coffeegb.gui.SwingDisplay
import eu.rekawek.coffeegb.memory.cart.Cartridge
import eu.rekawek.coffeegb.serial.SerialEndpoint
import io.github.zabuzard.discordplays.Config
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Dimension
import java.awt.Graphics
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import javax.swing.JFrame

@Service
class Emulator(
    private val config: Config
) {
    private val emulationService = Executors.newSingleThreadExecutor()

    private val clickController = ClickController()
    private val graphicsDisplay = GraphicsDisplay()

    init {
        if (config.localOnly) {
            start()
        }
    }

    private var gameboy: Gameboy? = null

    @Synchronized
    fun start() {
        require(gameboy == null) { "Cannot start emulation, it is already running" }

        val options = GameboyOptions(File(config.romPath), emptyList(), emptyList())
        val cartridge = Cartridge(options)
        val serialEndpoint = SerialEndpoint.NULL_ENDPOINT

        val display = if (config.localOnly) SwingDisplay(2) else graphicsDisplay

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

        gameboy = Gameboy(options, cartridge, display, controller, soundOutput, serialEndpoint)
            .also { emulationService.submit(it) }
    }

    @Synchronized
    fun stop() {
        requireNotNull(gameboy) { "Cannot stop emulation, none is running" }

        gameboy!!.stop()
        gameboy = null
    }

    suspend fun clickButton(button: ButtonListener.Button) =
        clickController.clickButton(button)

    fun render(g: Graphics, scale: Double = 2.0, x: Int = 0, y: Int = 0) {
        graphicsDisplay.render(g, scale, x, y)
    }

    companion object {
        const val RESOLUTION_WIDTH = 160
        const val RESOLUTION_HEIGHT = 144
    }
}
