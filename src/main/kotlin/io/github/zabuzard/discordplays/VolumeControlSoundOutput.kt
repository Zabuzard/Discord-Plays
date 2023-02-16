package io.github.zabuzard.discordplays

import eu.rekawek.coffeegb.gui.AudioSystemSoundOutput
import eu.rekawek.coffeegb.sound.SoundOutput

class VolumeControlSoundOutput : SoundOutput {
    private val delegate = AudioSystemSoundOutput()

    private var fullVolume = false

    override fun start() {
        delegate.start()
    }

    override fun stop() {
        delegate.stop()
    }

    override fun play(left: Int, right: Int) {
        if (fullVolume) {
            delegate.play(left, right)
        } else {
            delegate.play(0, 0)
        }
    }

    fun mute() {
        fullVolume = false
    }

    fun fullVolume() {
        fullVolume = true
    }
}
