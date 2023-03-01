package io.github.zabuzard.discordplays

import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.entity.Member
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.DiscordEmoji
import dev.kord.x.emoji.toReaction
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.CoroutineExceptionHandler
import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.CancellationException
import javax.imageio.ImageIO
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object Extensions {
    val logAllExceptions = CoroutineExceptionHandler { _, e ->
        if (e !is CancellationException) {
            logger.error(e) { UNKNOWN_ERROR_MESSAGE }
        }
    }

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

    suspend fun logAllExceptions(action: suspend () -> Unit) {
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

    fun UserMessageModifyBuilder.clearEmbeds() {
        embed { description = "" }
        embeds?.clear()
    }

    fun EmbedBuilder.author(member: Member) {
        author {
            name = member.displayName
            icon = member.avatar?.url ?: member.defaultAvatar.url
            url = "https://discord.com/users/${member.id.value}/"
        }
    }

    fun DiscordEmoji.toPartialEmoji() =
        DiscordPartialEmoji(name = toReaction().name)

    fun (() -> InputStream).asChannelProvider() =
        ChannelProvider { invoke().toByteReadChannel() }
}

private val logger = KotlinLogging.logger {}

private const val UNKNOWN_ERROR_MESSAGE = "Unknown error"
