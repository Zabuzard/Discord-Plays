package io.github.zabuzard.discordplays

import eu.rekawek.coffeegb.Gameboy
import eu.rekawek.coffeegb.GameboyOptions
import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.memory.cart.Cartridge
import eu.rekawek.coffeegb.serial.SerialEndpoint
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Graphics
import java.io.File

@Service
class GameService(
    private val clickController: ClickController,
    private val imageDisplay: ImageDisplay
) {
    init {
        // TODO Remove after debugging
        // start()
    }

    //var to hold gameboyInstance
    lateinit var gameboy: Gameboy


    fun start() {
        val romPath = "C:\\Users\\Zabuza\\Desktop\\Pokemon - Blue Version (UE)[!].zip"

        val options = GameboyOptions(File(romPath), listOf(), listOf())
        val cartridge = Cartridge(options)
        val serialEndpoint = SerialEndpoint.NULL_ENDPOINT

        //val swingDisplay = SwingDisplay(2)
        val display = CompositeDisplay(listOf(imageDisplay))

        val soundOutput = VolumeControlSoundOutput()
        soundOutput.mute()

        /*
        swingDisplay.preferredSize = Dimension(160 * 2, 144 * 2)
        val mainWindow = JFrame(cartridge.title)
        mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        mainWindow.setLocationRelativeTo(null)
        mainWindow.contentPane = swingDisplay
        mainWindow.isResizable = false
        mainWindow.isVisible = true
        mainWindow.pack()
        mainWindow.addKeyListener(controller)
        Thread(swingDisplay).start()
         */
        gameboy = Gameboy(options, cartridge, display, clickController, soundOutput, serialEndpoint)

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
