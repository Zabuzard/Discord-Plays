package io.github.zabuzard.discordplays.discord.gif

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.time.Duration

class Gif(frameRate: Duration) {
    private val rawStream = ByteArrayOutputStream()
    private val imageStream = MemoryCacheImageOutputStream(rawStream)
    private val gifSequence = GifSequenceWriter(
        imageStream,
        IMAGE_TYPE,
        frameRate.inWholeMilliseconds.toInt()
    )

    var size = 0
        private set

    operator fun plusAssign(image: BufferedImage) {
        gifSequence.writeToSequence(image)
        size++
    }

    fun endSequence(): ByteArray {
        gifSequence.close()
        imageStream.close()
        rawStream.close()

        return rawStream.toByteArray()
    }
}

private const val IMAGE_TYPE = BufferedImage.TYPE_INT_RGB
