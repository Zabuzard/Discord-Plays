package io.github.zabuzard.discordplays

import io.github.zabuzard.discordplays.Extensions.toInputStream
import io.ktor.utils.io.printStack
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object Extensions {
    fun Runnable.logAllExceptions() = Runnable {
        try {
            this@logAllExceptions.run()
        } catch (e: Throwable) {
            e.printStack()
        }
    }

    fun (() -> Unit).logAllExceptions() = Runnable {
        try {
            invoke()
        } catch (e: Throwable) {
            e.printStack()
        }
    }

    fun <T> ((T) -> Unit).logAllExceptions(): ((T) -> Unit) = {
        try {
            this.invoke(it)
        } catch (e: Throwable) {
            e.printStack()
        }
    }

    fun ByteArray.toInputStream() =
        ByteArrayInputStream(this)

    fun BufferedImage.toInputStream() =
        ByteArrayOutputStream().use {
            ImageIO.write(this, "png", it)
            it
        }.toByteArray().toInputStream()
}
