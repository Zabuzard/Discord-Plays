package io.github.zabuzard.discordplays.stream

import java.awt.image.BufferedImage

interface StreamConsumer {
    fun acceptFrame(frame: BufferedImage)
    fun acceptGif(gif: ByteArray)
}
