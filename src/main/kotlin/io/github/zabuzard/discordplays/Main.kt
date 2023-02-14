package io.github.zabuzard.discordplays

import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.Emojis.id
import eu.rekawek.coffeegb.Gameboy
import eu.rekawek.coffeegb.GameboyOptions
import eu.rekawek.coffeegb.controller.Controller
import eu.rekawek.coffeegb.gpu.Display
import eu.rekawek.coffeegb.gui.AudioSystemSoundOutput
import eu.rekawek.coffeegb.gui.Main
import eu.rekawek.coffeegb.gui.SwingController
import eu.rekawek.coffeegb.gui.SwingDisplay
import eu.rekawek.coffeegb.memory.cart.Cartridge
import eu.rekawek.coffeegb.serial.SerialEndpoint
import eu.rekawek.coffeegb.sound.SoundOutput
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.arguments.IntegerArg
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.extensions.button
import java.awt.Dimension
import java.io.File
import java.util.*
import javax.swing.JFrame

@OptIn(KordPreview::class)
fun main(args: Array<String>) {
    require(args.size == 1) {
        "Incorrect number of arguments. The first argument must be the bot token. " +
                "Supplied arguments: ${args.contentToString()}"
    }

    val token = args.first()

    val romPath = "C:\\Users\\Zabuza\\Desktop\\Pokemon - Blue Version (UE)[!].zip"


    val options = GameboyOptions(File(romPath), listOf(), listOf())
    val cartridge = Cartridge(options)
    val display = SwingDisplay(2)
    val controller = SwingController(Properties())
    val soundOutput = AudioSystemSoundOutput()
    val serialEndpoint = SerialEndpoint.NULL_ENDPOINT

    val gameboy = Gameboy(options, cartridge, display, controller, soundOutput, serialEndpoint)

    display.preferredSize = Dimension(160 * 2, 144 * 2)
    val mainWindow = JFrame(cartridge.title)
    mainWindow.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    mainWindow.setLocationRelativeTo(null)
    mainWindow.contentPane = display
    mainWindow.isResizable = false
    mainWindow.isVisible = true
    mainWindow.pack()
    mainWindow.addKeyListener(controller)

    //Thread(display).start()
    //Thread(gameboy).start()

    bot(token) {}
}

fun commands() = commands("game") {
    slash("game-start", "Start a new game") {
        execute {
            respond("Starting a game")
            channel.createMessage {
                content = "this is a game..."
                actionRow {
                    button(null, Emojis.arrowUp) {
                    }
                    button(null, Emojis.arrowLeft) {
                    }
                    button(null, Emojis.arrowRight) {
                    }
                    button(null, Emojis.arrowDown) {
                    }
                }
                actionRow {
                    button(null, Emojis.a) {
                    }
                    button(null, Emojis.b) {
                    }
                    button("start", null) {
                    }
                    button("select", null) {
                    }
                }
            }
        }
    }
}
