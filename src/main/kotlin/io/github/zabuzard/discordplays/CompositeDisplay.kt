package io.github.zabuzard.discordplays

import eu.rekawek.coffeegb.gpu.Display

class CompositeDisplay(private val displays: List<Display>) : Display {
    override fun putDmgPixel(color: Int) {
        displays.forEach {
            it.putDmgPixel(color)
        }
    }

    override fun putColorPixel(gbcRgb: Int) {
        displays.forEach {
            it.putDmgPixel(gbcRgb)
        }
    }

    override fun requestRefresh() {
        displays.forEach(Display::requestRefresh)
    }

    override fun waitForRefresh() {
        displays.forEach(Display::waitForRefresh)
    }

    override fun enableLcd() {
        displays.forEach(Display::enableLcd)
    }

    override fun disableLcd() {
        displays.forEach(Display::disableLcd)
    }
}
