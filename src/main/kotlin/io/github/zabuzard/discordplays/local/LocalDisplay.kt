package io.github.zabuzard.discordplays.local

import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.stream.STREAM_HEIGHT
import io.github.zabuzard.discordplays.stream.STREAM_WIDTH
import io.github.zabuzard.discordplays.stream.StreamConsumer
import me.jakejmattson.discordkt.annotations.Service
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.WindowConstants

@Service
class LocalDisplay(
    private val emulator: Emulator
) : StreamConsumer {
    private var frame: JFrame? = null
    private var label: JLabel? = null

    @Synchronized
    fun activate() {
        require(frame == null) { "Cannot activate local display, is already active" }

        frame = JFrame()
        frame!!.title = "Local Display - Discord Plays"
        frame!!.setSize(STREAM_WIDTH, STREAM_HEIGHT)
        frame!!.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        label = JLabel()
        label!!.icon =
            ImageIcon(BufferedImage(STREAM_WIDTH, STREAM_HEIGHT, BufferedImage.TYPE_INT_RGB))
        frame!!.contentPane.add(label!!, BorderLayout.CENTER)
        frame!!.setLocationRelativeTo(null)
        frame!!.pack()
        frame!!.isVisible = true

        frame!!.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = deactivate()
        })
        frame!!.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                e.toButton()?.let { emulator.pressButton(it) }
            }

            override fun keyReleased(e: KeyEvent) {
                e.toButton()?.let { emulator.releaseButton(it) }
            }
        })
    }

    @Synchronized
    fun deactivate() {
        require(frame != null) { "Cannot deactivate local display, is not active" }
        frame?.dispose()
        frame = null
        label = null
    }

    override fun acceptFrame(frame: BufferedImage) {
        label?.icon = ImageIcon(frame)
    }

    override fun acceptGif(gif: ByteArray) {
        // Only interested in frames
    }
}

private fun KeyEvent.toButton() =
    when (keyCode) {
        KeyEvent.VK_UP, KeyEvent.VK_W -> Button.UP
        KeyEvent.VK_LEFT, KeyEvent.VK_A -> Button.LEFT
        KeyEvent.VK_RIGHT, KeyEvent.VK_D -> Button.RIGHT
        KeyEvent.VK_DOWN, KeyEvent.VK_S -> Button.DOWN
        KeyEvent.VK_SPACE, KeyEvent.VK_Q -> Button.A
        KeyEvent.VK_BACK_SPACE, KeyEvent.VK_E, KeyEvent.VK_ESCAPE -> Button.B
        KeyEvent.VK_ENTER, KeyEvent.VK_R -> Button.START
        KeyEvent.VK_DELETE, KeyEvent.VK_T -> Button.SELECT
        else -> null
    }
