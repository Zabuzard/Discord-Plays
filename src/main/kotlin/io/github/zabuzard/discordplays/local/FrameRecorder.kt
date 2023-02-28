package io.github.zabuzard.discordplays.local

import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.stream.StreamConsumer
import io.github.zabuzard.discordplays.stream.StreamRenderer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.jakejmattson.discordkt.annotations.Service
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.name

@Service
class FrameRecorder(
    private val config: Config,
    private val streamRenderer: StreamRenderer
) : StreamConsumer {
    private var skipFrameCount = AtomicInteger(SKIP_FRAMES)
    private var nextFrameId = AtomicLong(0)
    private var previousFrameDate: LocalDate? = null

    fun start() {
        streamRenderer.addStreamConsumer(this)
    }

    fun stop() {
        streamRenderer.removeStreamConsumer(this)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun acceptFrame(frame: BufferedImage) {
        if (skipFrameCount.getAndDecrement() == 0) {
            GlobalScope.launch(logAllExceptions) { recordFrame(frame) }

            skipFrameCount.set(SKIP_FRAMES)
        }
    }

    override suspend fun acceptGif(gif: ByteArray) {
        // Only interested in frames
    }

    private fun recordFrame(frame: BufferedImage) {
        val date = LocalDate.now()!!
        val frameFolder = Path.of(config.recordingPath, date.toString()).createDirectories()

        val frameId = when (date) {
            previousFrameDate -> nextFrameId.getAndIncrement()
            else -> nextFrameIdInFolder(frameFolder).also { nextFrameId.set(it + 1) }
        }
        previousFrameDate = date

        val framePath = Path.of(config.recordingPath, date.toString(), "$frameId$FRAME_SUFFIX")
        Files.createDirectories(framePath)
        ImageIO.write(frame, "png", framePath.toFile())
    }

    private fun nextFrameIdInFolder(frameFolder: Path) =
        Files.list(frameFolder).map(Path::name).filter { it.endsWith("png") }
            .mapToLong { it.split(" ", limit = 2)[0].toLong() }
            .max().orElse(-1) + 1

    companion object {
        const val FRAME_SUFFIX = " frame.png"
    }
}

private const val SKIP_FRAMES = 1
