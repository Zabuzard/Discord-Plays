package io.github.zabuzard.discordplays.stream

import java.awt.image.BufferedImage

interface StreamConsumer {
    suspend fun acceptFrame(frame: BufferedImage)
    suspend fun acceptGif(gif: ByteArray)
}
