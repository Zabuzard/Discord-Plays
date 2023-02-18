package io.github.zabuzard.discordplays.stream

import io.github.zabuzard.discordplays.discord.util.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.discord.util.toGif
import io.github.zabuzard.discordplays.emulation.Emulator
import io.ktor.utils.io.printStack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.jakejmattson.discordkt.annotations.Service
import java.awt.image.BufferedImage
import java.lang.Exception
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

@Service
class StreamRenderer(
    private val emulator: Emulator,
    private val overlayRenderer: OverlayRenderer
) {
    private val consumers = mutableListOf<StreamConsumer>()

    private val renderService = Executors.newSingleThreadScheduledExecutor()
    private var renderJob: Job? = null

    private val gifBuffer = mutableListOf<BufferedImage>()

    @Synchronized
    fun addStreamConsumer(consumer: StreamConsumer) {
        consumers += consumer
    }

    @Synchronized
    fun removeStreamConsumer(consumer: StreamConsumer) {
        consumers -= consumer
    }

    suspend fun start() {
        require(renderJob == null) { "Cannot start, job is already running" }
        renderService.submit(
            {
                runBlocking {
                    renderJob = launch { renderStream() }
                }
            }.logAllExceptions()
        )
    }

    fun stop() {
        requireNotNull(renderJob) { "Cannot stop, no job is running" }

        renderJob!!.cancel()
        renderJob = null
    }

    private suspend fun renderStream() {
        while (coroutineContext.isActive) {
            try {
                val frame = renderFrame()
                synchronized(this) {
                    consumers.forEach({ consumer: StreamConsumer -> consumer.acceptFrame(frame) }.logAllExceptions())
                }

                gifBuffer += frame
                if (gifBuffer.size >= FLUSH_GIF_AT_FRAMES) {
                    val gif = gifBuffer.toGif(gifFrameRate)
                    synchronized(this) {
                        consumers.forEach({ consumer: StreamConsumer -> consumer.acceptGif(gif) }.logAllExceptions())
                    }

                    gifBuffer.clear()
                }

                delay(renderFrameRate)
            } catch (e: Exception) {
                e.printStack()
            }
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

            g.translate(SCREEN_WIDTH, 0)
            overlayRenderer.renderOverlay(g, OVERLAY_WIDTH, OVERLAY_HEIGHT)

            g.dispose()
        }
}

private val renderFrameRate = (150).milliseconds

// GIF plays slower to account for the loading times, that way the experience is
// smoother and does not display the last frame for a longer time
private val gifFrameRate = (220).milliseconds

private const val EMULATOR_SCALING_FACTOR = 4.0
private const val FLUSH_GIF_AT_FRAMES = 30

private const val SCREEN_WIDTH = (Emulator.RESOLUTION_WIDTH * EMULATOR_SCALING_FACTOR).toInt()
private const val SCREEN_HEIGHT = (Emulator.RESOLUTION_HEIGHT * EMULATOR_SCALING_FACTOR).toInt()
private const val OVERLAY_WIDTH = 170
private const val OVERLAY_HEIGHT = SCREEN_HEIGHT
const val STREAM_WIDTH = SCREEN_WIDTH + OVERLAY_WIDTH
const val STREAM_HEIGHT = SCREEN_HEIGHT
