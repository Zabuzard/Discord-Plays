package io.github.zabuzard.discordplays.stream

import io.github.oshai.KotlinLogging
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.discord.gif.Gif
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.stream.BannerRendering.Placement
import io.github.zabuzard.discordplays.stream.BannerRendering.renderBanner
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class StreamRenderer(
    private val config: Config,
    private val emulator: Emulator,
    private val overlayRenderer: OverlayRenderer
) {
    private val renderService = Executors.newSingleThreadScheduledExecutor()
    private val gifConsumerService = Executors.newSingleThreadExecutor()
    private var consumers = emptyList<StreamConsumer>()

    private var renderJob: Future<*>? = null
    private var consumeGifJob: Future<*>? = null

    private var gif: Gif = Gif(gifFrameRate)

    var globalMessage: String? = null

    fun addStreamConsumer(consumer: StreamConsumer) {
        consumers += consumer
    }

    fun removeStreamConsumer(consumer: StreamConsumer) {
        consumers -= consumer
    }

    fun start() {
        require(renderJob == null) { "Cannot start, job is already running" }

        renderJob = renderService.scheduleWithFixedDelay(
            { logAllExceptions { renderStream() } },
            0,
            150,
            TimeUnit.MILLISECONDS
        )
    }

    fun stop() {
        requireNotNull(renderJob) { "Cannot stop, no job is running" }

        renderJob!!.cancel(false)
        renderJob = null
    }

    private fun renderStream() {
        val frame = renderFrame()
        consumers.map {
            CompletableFuture.runAsync { logAllExceptions { it.acceptFrame(frame) } }
        }.forEach { it.join() }

        gif += frame
        if (gif.size >= FLUSH_GIF_AT_FRAMES) {
            // Skip GIF if consumers are too slow
            if (consumeGifJob?.isDone != false) {
                val rawGif = gif.endSequence()
                consumeGifJob = gifConsumerService.submit { sendGifToConsumers(rawGif) }
            } else {
                logger.warn { "Skipping gif, consumers are still busy with previous" }
            }

            gif = Gif(gifFrameRate)
        }
    }

    private fun renderFrame() =
        BufferedImage(
            STREAM_WIDTH,
            STREAM_HEIGHT,
            BufferedImage.TYPE_INT_RGB
        ).apply {
            val g = createGraphics()
            emulator.render(g, EMULATOR_SCALING_FACTOR)

            globalMessage?.let {
                renderBanner(
                    it,
                    g,
                    config.font,
                    SCREEN_WIDTH,
                    SCREEN_HEIGHT,
                    Placement.TOP
                )
            }

            // Right side
            g.translate(SCREEN_WIDTH, 0)
            overlayRenderer.renderOverlay(g, OVERLAY_WIDTH, OVERLAY_HEIGHT)

            g.dispose()
        }

    private fun sendGifToConsumers(rawGif: ByteArray) {
        consumers.map {
            CompletableFuture.runAsync { logAllExceptions { it.acceptGif(rawGif) } }
        }.forEach(CompletableFuture<Void>::join)
    }
}

private val logger = KotlinLogging.logger {}

// GIF plays slower to account for the loading times, that way the experience is
// smoother and does not display the last frame for a longer time
private val gifFrameRate = 220.milliseconds

private const val EMULATOR_SCALING_FACTOR = 2.5
private const val FLUSH_GIF_AT_FRAMES = 30

const val SCREEN_WIDTH = (Emulator.RESOLUTION_WIDTH * EMULATOR_SCALING_FACTOR).toInt()
const val SCREEN_HEIGHT = (Emulator.RESOLUTION_HEIGHT * EMULATOR_SCALING_FACTOR).toInt()
const val OVERLAY_WIDTH = 170
const val OVERLAY_HEIGHT = SCREEN_HEIGHT
const val STREAM_WIDTH = SCREEN_WIDTH + OVERLAY_WIDTH
const val STREAM_HEIGHT = SCREEN_HEIGHT
