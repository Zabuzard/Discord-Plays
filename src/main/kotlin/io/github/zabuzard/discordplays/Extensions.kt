package io.github.zabuzard.discordplays

import io.github.oshai.KotlinLogging
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import javax.imageio.ImageIO
import kotlin.time.Duration
import kotlin.time.toJavaDuration

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

    fun logAllExceptions(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            if (e !is CancellationException) {
                logger.error(e) { UNKNOWN_ERROR_MESSAGE }
            }
        }
    }

    fun ByteArray.toInputStream() =
        ByteArrayInputStream(this)

    fun BufferedImage.toByteArray(): ByteArray = ByteArrayOutputStream().use {
        ImageIO.write(this, "png", it)
        it
    }.toByteArray()

    fun Duration.formatted() = toJavaDuration().run {
        listOf(
            toDays().toInt() to "d",
            toHoursPart() to "h",
            toMinutesPart() to "m",
            toSecondsPart() to "s"
        ).filterNot { (value, _) -> value == 0 }
            .joinToString(separator = " ") { (value, label) -> "$value$label" }
    }

    fun EmbedBuilder.setAuthor(member: Member) = setAuthor(
        member.effectiveName,
        "https://discord.com/users/${member.idLong}/",
        member.avatarUrl
    )

    fun IReplyCallback.replyEphemeral(content: String) =
        reply(content).setEphemeral(true)
}

private val logger = KotlinLogging.logger {}

private const val UNKNOWN_ERROR_MESSAGE = "Unknown error"
