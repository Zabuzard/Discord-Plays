package io.github.zabuzard.discordplays

import eu.rekawek.coffeegb.gpu.Display
import eu.rekawek.coffeegb.gui.SwingDisplay.translateGbcRgb
import me.jakejmattson.discordkt.annotations.Service
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Service
class ImageDisplay() : Display {
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

    override fun putColorPixel(gbcRgb: Int) {
        flatPixelRgb[pixelCursor++] = translateGbcRgb(gbcRgb)
    }

    override fun requestRefresh() {
        renderTask = renderService.submit(this::refresh)
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

    companion object {
        const val RESOLUTION_WIDTH = 160
        const val RESOLUTION_HEIGHT = 144
    }
}

private val colors = intArrayOf(0xe6f8da, 0x99c886, 0x437969, 0x051f2a)
private val blankColor = Color(colors[0])

fun translateGbcRgb(gbcRgb: Int): Int {
    val r = gbcRgb shr 0 and 0x1f
    val g = gbcRgb shr 5 and 0x1f
    val b = gbcRgb shr 10 and 0x1f

    var result = r * 8 shl 16
    result = result or (g * 8 shl 8)
    result = result or (b * 8 shl 0)

    return result
}
