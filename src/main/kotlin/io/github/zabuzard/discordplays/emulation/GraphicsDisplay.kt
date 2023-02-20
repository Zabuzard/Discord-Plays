package io.github.zabuzard.discordplays.emulation

import eu.rekawek.coffeegb.gpu.Display
import eu.rekawek.coffeegb.gui.SwingDisplay.translateGbcRgb
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.emulation.Emulator.Companion.RESOLUTION_HEIGHT
import io.github.zabuzard.discordplays.emulation.Emulator.Companion.RESOLUTION_WIDTH
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class GraphicsDisplay() : Display {
    private val renderService = Executors.newSingleThreadExecutor()

    private val flatPixelRgb = IntArray(RESOLUTION_WIDTH * RESOLUTION_HEIGHT)
    private var pixelCursor = 0

    private var enabled = true

    private var renderTask: Future<*>? = null

    private val img: BufferedImage =
        BufferedImage(RESOLUTION_WIDTH, RESOLUTION_HEIGHT, BufferedImage.TYPE_INT_RGB)

    override fun putDmgPixel(color: Int) {
        flatPixelRgb[pixelCursor++] = colors[color]
        pixelCursor %= flatPixelRgb.size
    }

    override fun putColorPixel(gameboyColorRgb: Int) {
        flatPixelRgb[pixelCursor++] = translateGbcRgb(gameboyColorRgb)
    }

    override fun requestRefresh() {
        renderTask = renderService.submit(this::refresh.logAllExceptions())
    }

    override fun waitForRefresh() {
        renderTask?.get(2, TimeUnit.SECONDS)
    }

    override fun enableLcd() {
        enabled = true
    }

    override fun disableLcd() {
        enabled = false
    }

    @Synchronized
    private fun refresh() {
        img.setRGB(
            0,
            0,
            RESOLUTION_WIDTH,
            RESOLUTION_HEIGHT,
            flatPixelRgb,
            0,
            RESOLUTION_WIDTH
        )

        pixelCursor = 0
        renderTask = null
    }

    @Synchronized
    fun render(g: Graphics, scale: Double = 2.0, x: Int = 0, y: Int = 0) {
        val scaledWidth = (RESOLUTION_WIDTH * scale).toInt()
        val scaledHeight = (RESOLUTION_HEIGHT * scale).toInt()

        if (enabled) {
            g.drawImage(img, x, y, scaledWidth, scaledHeight, null)
        } else {
            g.color = blankColor
            g.drawRect(x, y, scaledWidth, scaledHeight)
        }
    }
}

private val colors = intArrayOf(0xe6f8da, 0x99c886, 0x437969, 0x051f2a)
private val blankColor = Color(colors[0])

private fun Int.toRgbColor(): Int {
    val r = this shr 0 and 0x1f
    val g = this shr 5 and 0x1f
    val b = this shr 10 and 0x1f

    return (
        (r * 8 shl 16)
            or (g * 8 shl 8)
            or (b * 8 shl 0)
        )
}
