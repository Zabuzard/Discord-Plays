package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.DmChannel
import dev.kord.rest.builder.message.EmbedBuilder.Limits.title
import dev.kord.x.emoji.Emojis
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.Extensions.toInputStream
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.stream.StreamConsumer
import io.github.zabuzard.discordplays.stream.StreamRenderer
import io.ktor.utils.io.printStack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import me.jakejmattson.discordkt.Discord
import me.jakejmattson.discordkt.annotations.Service
import me.jakejmattson.discordkt.conversations.ConversationBuilder
import me.jakejmattson.discordkt.conversations.conversation
import me.jakejmattson.discordkt.extensions.toPartialEmoji
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun autoSaveConversation(
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver,
    user: User
) = conversation {
    val dmChannel = user.getDmChannel()

    sendFrameSnapshot(dmChannel, autoSaver)
    delay((3).seconds)
    val start = reminderDialog()
    if (!start) return@conversation

    bot.userInputLockedToOwners = true
    bot.setGlobalMessage("Auto saving in progress - please wait")
    delay((1).seconds)

    sendFrameSnapshot(dmChannel, autoSaver)
    val ready = preparationDialog()
    if (!ready) {
        endSaveRoutine(bot)
        return@conversation
    }

    emulator.clickButton(Button.START)
    delay((1).seconds)
    sendFrameSnapshot(dmChannel, autoSaver)
    when (val cursorInput = menuDialog().first()) {
        CANCEL_LABEL -> {
            endSaveRoutine(bot)
            return@conversation
        }

        NO_INPUT_LABEL -> Unit
        else -> {
            val (count, buttonName) = cursorInput.split(" ", limit = 2)
            val button = Button.valueOf(buttonName)
            repeat(count.toInt()) {
                emulator.clickButton(button)
                delay((500).milliseconds)
            }
        }
    }
    emulator.clickButton(Button.A)
    delay((3).seconds)
    emulator.clickButton(Button.A)
    delay((6).seconds)

    sendFrameSnapshot(dmChannel, autoSaver)
    confirmationDialog()
    endSaveRoutine(bot)

    dmChannel.createMessage("Thanks 👍")
}

private suspend fun ConversationBuilder.reminderDialog() = promptButton {
    embed {
        title = "AutoSave Reminder"
        description = "It is time to save the game. Continue when ready."
    }

    buttons {
        button("Start", null, true, ButtonStyle.Primary)
        button("Skip", null, false, ButtonStyle.Danger)
    }
}

private suspend fun ConversationBuilder.preparationDialog() = promptButton {
    embed {
        title = "AutoSave Preparation"
        description = """
                Input has been blocked for other users.
                
                Please finish any encounter or dialog and close all menus - so that the routine can start by opening the menu.
        """.trimIndent()
    }

    buttons {
        button("Ready", null, true, ButtonStyle.Primary)
        button("Cancel", null, false, ButtonStyle.Danger)
    }
}

private suspend fun ConversationBuilder.menuDialog() = promptSelect {
    selectionCount = 1..1

    content {
        title = "AutoSave Menu"
        description = """
                Is the menu open? If not, please open it manually.
                
                Please select the input required to move the cursor on the **SAVE** option.
        """.trimIndent()
    }

    listOf(
        3 to "POKéMON",
        2 to "ITEM",
        1 to "TRAINER"
    ).forEach { (count, menuLabel) ->
        option(
            "$count - $menuLabel",
            "$count ${Button.DOWN}",
            emoji = Emojis.arrowDown.toPartialEmoji()
        )
    }

    option("0 - SAVE", NO_INPUT_LABEL, emoji = Emojis.greenCircle.toPartialEmoji())

    listOf(
        1 to "OPTION",
        2 to "EXIT"
    ).forEach { (count, menuLabel) ->
        option(
            "$count - $menuLabel",
            "$count ${Button.UP}",
            emoji = Emojis.arrowUp.toPartialEmoji()
        )
    }

    option(CANCEL_LABEL, emoji = Emojis.x.toPartialEmoji())
}

private suspend fun ConversationBuilder.confirmationDialog() = promptButton {
    embed {
        title = "AutoSave Confirmation"
        description = """
                Auto saving done. Does everything look good? If not, please save manually now.
                
                Once ready, proceed to unlock input again.
        """.trimIndent()
    }

    buttons {
        button("Done", null, Unit, ButtonStyle.Success)
    }
}

private fun ConversationBuilder.endSaveRoutine(bot: DiscordBot) {
    bot.setGlobalMessage(null)
    bot.userInputLockedToOwners = false
}

private suspend fun ConversationBuilder.sendFrameSnapshot(
    dmChannel: DmChannel,
    autoSaver: AutoSaver
) {
    dmChannel.createMessage {
        addFile("snapshot.png", autoSaver.lastFrame.toInputStream())
    }
}

@Service
class AutoSaver(
    private val config: Config,
    private val streamRenderer: StreamRenderer
) : StreamConsumer {
    private val routineService = Executors.newSingleThreadExecutor()
    private var routineJob: Job? = null
    internal lateinit var lastFrame: BufferedImage

    init {
        streamRenderer.addStreamConsumer(this)
    }

    fun start(bot: DiscordBot, emulator: Emulator, discord: Discord) {
        routineService.submit(
            {
                runBlocking {
                    routineJob = launch { runRoutine(bot, emulator, discord) }
                }
            }.logAllExceptions()
        )
    }

    fun stop() {
        routineJob?.cancel()
        routineJob = null
    }

    private suspend fun runRoutine(bot: DiscordBot, emulator: Emulator, discord: Discord) {
        while (coroutineContext.isActive) {
            try {
                delay(Clock.System.now().untilNext(config.autoSaveRemindAt) + (1).minutes)

                config.owners.mapNotNull { discord.kord.getUser(it) }.forEach {
                    autoSaveConversation(bot, emulator, this, it).startPrivately(discord, it)
                }
            } catch (e: Exception) {
                e.printStack()
            }
        }
    }

    override fun acceptFrame(frame: BufferedImage) {
        lastFrame = frame
    }

    override fun acceptGif(gif: ByteArray) {
        // Only interested in frames
    }
}

private const val NO_INPUT_LABEL = "no input"
private const val CANCEL_LABEL = "cancel"

private val timeZone = TimeZone.currentSystemDefault()

private fun Instant.untilNext(untilTime: LocalTime): Duration {
    val current = toLocalDateTime(timeZone)

    val untilDate = current.date.let {
        if (current.time >= untilTime) it + DatePeriod(days = 1) else it
    }
    val until = untilTime.toJavaLocalTime()
        .atDate(untilDate.toJavaLocalDate()).toKotlinLocalDateTime()
        .toInstant(timeZone)

    return until - this
}
