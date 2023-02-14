package io.github.zabuzard.discordplays

import eu.rekawek.coffeegb.gpu.Display
import eu.rekawek.coffeegb.gui.SwingDisplay.translateGbcRgb
import kotlinx.atomicfu.locks.withLock
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JPanel

class ImageDisplay(scale: Int = 2) : JPanel(), Display, Runnable {
    private val scaledWidth = RESOLUTION_WIDTH * scale
    private val scaledHeight = RESOLUTION_HEIGHT * scale

    private val flatPixelRgb = IntArray(RESOLUTION_WIDTH * RESOLUTION_HEIGHT)
    private var pixelCursor = 0

    private var enabled = true
    private var doStop = false
    private var doRefresh = false

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    val img: BufferedImage =
        BufferedImage(RESOLUTION_WIDTH, RESOLUTION_HEIGHT, BufferedImage.TYPE_INT_RGB)

    override fun putDmgPixel(color: Int) {
        flatPixelRgb[pixelCursor++] = colors[color]
        pixelCursor %= flatPixelRgb.size
    }

    override fun putColorPixel(gbcRgb: Int) {
        flatPixelRgb[pixelCursor++] = translateGbcRgb(gbcRgb)
    }

    override fun requestRefresh() {
        doRefresh = true
        condition.signalAll()
    }

    override fun waitForRefresh() {
        while (doRefresh) {
            condition.await(1, TimeUnit.MILLISECONDS)
        }
    }

    override fun enableLcd() {
        enabled = true
    }

    override fun disableLcd() {
        enabled = false
    }

    override fun run() {
        while (!doStop) {
            lock.withLock {
                condition.await(1, TimeUnit.MILLISECONDS)
            }

            if (doRefresh) {
                img.setRGB(
                    0,
                    0,
                    RESOLUTION_WIDTH,
                    RESOLUTION_HEIGHT,
                    flatPixelRgb,
                    0,
                    RESOLUTION_WIDTH
                )
                validate()
                repaint()

                lock.withLock {
                    pixelCursor = 0
                    doRefresh = false
                    condition.signalAll()
                }
            }
        }
    }

    fun stop() {
        doStop = true
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2d = g.create() as Graphics2D
        if (enabled) {
            g2d.drawImage(img, 0, 0, scaledWidth, scaledHeight, null)
        } else {
            g2d.color = blankColor
            g2d.drawRect(0, 0, scaledWidth, scaledHeight)
        }
        g2d.dispose()
    }
}

private const val RESOLUTION_WIDTH = 160
private const val RESOLUTION_HEIGHT = 144
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
