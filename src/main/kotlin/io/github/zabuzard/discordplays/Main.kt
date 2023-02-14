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
import me.jakejmattson.discordkt.extensions.createMenu
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

            val displayMessage = channel.createMessage("display...")
            channel.createMenu {
                page { description = "controls" }

                buttons {
                    editButton(null, Emojis.arrowUp) {
                        displayMessage.edit { content = "you pressed up" }
                    }
                    editButton(null, Emojis.arrowLeft) {
                        displayMessage.edit { content = "you pressed left" }
                    }
                    editButton(null, Emojis.arrowRight) {
                        displayMessage.edit { content = "you pressed right" }
                    }
                    editButton(null, Emojis.arrowDown) {
                        displayMessage.edit { content = "you pressed down" }
                    }
                }
                buttons {
                    editButton(null, Emojis.a) {
                        displayMessage.edit { content = "you pressed a" }
                    }
                    editButton(null, Emojis.b) {
                        displayMessage.edit { content = "you pressed b" }
                    }
                    editButton("start", null) {
                        displayMessage.edit { content = "you pressed start" }
                    }
                    editButton("select", null) {
                        displayMessage.edit { content = "you pressed select" }
                    }
                }
            }
        }
    }
}
