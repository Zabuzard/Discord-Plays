package io.github.zabuzard.discordplays

import eu.rekawek.coffeegb.Gameboy
import eu.rekawek.coffeegb.GameboyOptions
import eu.rekawek.coffeegb.controller.ButtonListener
import eu.rekawek.coffeegb.gui.AudioSystemSoundOutput
import eu.rekawek.coffeegb.gui.SwingDisplay
import eu.rekawek.coffeegb.memory.cart.Cartridge
import eu.rekawek.coffeegb.serial.SerialEndpoint
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Dimension
import java.io.File
import javax.swing.JFrame

@Service
class GameService(private val clickController: ClickController, private val imageDisplay: ImageDisplay) {
    init {
        // TODO Remove after debugging
        start()
    }

    fun start() {
        val romPath = "C:\\Users\\Zabuza\\Desktop\\Pokemon - Blue Version (UE)[!].zip"

        val options = GameboyOptions(File(romPath), listOf(), listOf())
        val cartridge = Cartridge(options)
        val serialEndpoint = SerialEndpoint.NULL_ENDPOINT

        val swingDisplay = SwingDisplay(2)
        val display = CompositeDisplay(listOf(swingDisplay, imageDisplay))

        val soundOutput = AudioSystemSoundOutput()

        val gameboy = Gameboy(options, cartridge, display, clickController, soundOutput, serialEndpoint)

        swingDisplay.preferredSize = Dimension(160 * 2, 144 * 2)
        val mainWindow = JFrame(cartridge.title)
        mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        mainWindow.setLocationRelativeTo(null)
        mainWindow.contentPane = swingDisplay
        mainWindow.isResizable = false
        mainWindow.isVisible = true
        mainWindow.pack()
        // mainWindow.addKeyListener(controller)
        Thread(swingDisplay).start()

        Thread(gameboy).start()
    }

    suspend fun clickButton(button: ButtonListener.Button) =
        clickController.clickButton(button)
}
