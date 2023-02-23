package io.github.zabuzard.discordplays

import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import javax.imageio.ImageIO

object Extensions {
    fun Runnable.logAllExceptions() = Runnable {
        try {
            this@logAllExceptions.run()
        } catch (e: Throwable) {
            if (e !is CancellationException) {
                logger.error(e) { UNKNOWN_ERROR_MESSAGE }
            }
        }
    }

    fun (() -> Unit).logAllExceptions() = Runnable {
        try {
            invoke()
        } catch (e: Throwable) {
            if (e !is CancellationException) {
                logger.error(e) { UNKNOWN_ERROR_MESSAGE }
            }
        }
    }

    fun <T> ((T) -> Unit).logAllExceptions(): ((T) -> Unit) = {
        try {
            this.invoke(it)
        } catch (e: Throwable) {
            if (e !is CancellationException) {
                logger.error(e) { UNKNOWN_ERROR_MESSAGE }
            }
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

private val logger = KotlinLogging.logger {}

private const val UNKNOWN_ERROR_MESSAGE = "Unknown error"
