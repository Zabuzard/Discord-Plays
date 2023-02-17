package io.github.zabuzard.discordplays

import io.github.zabuzard.discordplays.emulation.Emulator
import me.jakejmattson.discordkt.annotations.Service
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.WindowConstants

@Service
class ImageTestRenderer(private val emulator: Emulator) {
    private val renderService = Executors.newSingleThreadScheduledExecutor()

    init {
        // NOTE Comment in to have a test render
        renderService.scheduleAtFixedRate(this::render, 1000, 100, TimeUnit.MILLISECONDS)
    }

    private fun render() {
        val image = BufferedImage(
            (Emulator.RESOLUTION_WIDTH * SCALE).toInt(),
            (Emulator.RESOLUTION_HEIGHT * SCALE).toInt(),
            BufferedImage.TYPE_INT_RGB
        )

        val g = image.createGraphics()
        emulator.render(g, SCALE)
        g.dispose()

        display(image)
    }

    // Quick and dirty
    private var frame: JFrame? = null
    private var label: JLabel? = null
    private fun display(image: BufferedImage) {
        if (frame == null) {
            frame = JFrame()
            frame!!.title = "Test render"
            frame!!.setSize(image.width, image.height)
            frame!!.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

            label = JLabel()
            label!!.icon = ImageIcon(image)
            frame!!.contentPane.add(label!!, BorderLayout.CENTER)
            frame!!.setLocationRelativeTo(null)
            frame!!.pack()
            frame!!.isVisible = true
        } else {
            label!!.icon = ImageIcon(image)
        }
    }
}

private const val SCALE = 4.0
