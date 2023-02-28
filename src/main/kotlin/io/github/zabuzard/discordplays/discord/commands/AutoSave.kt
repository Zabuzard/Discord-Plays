package io.github.zabuzard.discordplays.discord.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.DmChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.toReaction
import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.Extensions.toByteArray
import io.github.zabuzard.discordplays.Extensions.toInputStream
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.stream.StreamConsumer
import io.github.zabuzard.discordplays.stream.StreamRenderer
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.util.cio.toByteReadChannel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Kord.onAutoSaveConversation(
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver
) {
    on<ButtonInteractionCreateEvent> {
        with(interaction) {
            if (!componentId.startsWith(AUTO_SAVE_ID_PREFIX)) {
                return@on
            }

            val dmChannel = interaction.channel.asChannelOfOrNull<DmChannel>() ?: return@on

            interaction.deferPublicMessageUpdate()
            val (_, phase, value, _) = componentId.split("-", limit = 4)
            when (phase) {
                "1" -> onReminderResponse(value, dmChannel, bot, autoSaver)
                "2" -> onPreparationResponse(value, dmChannel, bot, emulator, autoSaver)
                "4" -> onConfirmationResponse(dmChannel, bot)
            }
        }
    }

    on<SelectMenuInteractionCreateEvent> {
        with(interaction) {
            if (!componentId.startsWith(AUTO_SAVE_ID_PREFIX)) {
                return@on
            }

            val dmChannel = interaction.channel.asChannelOfOrNull<DmChannel>() ?: return@on

            interaction.deferPublicMessageUpdate()
            when (componentId.split("-", limit = 4)[1]) {
                "3" -> onMenuResponse(values, dmChannel, bot, emulator, autoSaver)
            }
        }
    }
}

suspend fun autoSaveConversation(
    autoSaver: AutoSaver,
    dmChannel: DmChannel
) {
    sendFrameSnapshot(dmChannel, autoSaver)
    delay(3.seconds)

    dmChannel.createMessage {
        embed {
            title = "AutoSave Reminder"
            description = "It is time to save the game. Continue when ready."
        }
        actionRow {
            interactionButton(
                ButtonStyle.Primary,
                componentId(REMINDER_PHASE_ID, true.toString())
            ) { label = "Start" }
            interactionButton(
                ButtonStyle.Danger,
                componentId(REMINDER_PHASE_ID, false.toString())
            ) { label = "Skip" }
        }
    }
}

private suspend fun onReminderResponse(
    response: String,
    dmChannel: DmChannel,
    bot: DiscordBot,
    autoSaver: AutoSaver
) {
    val start = response.toBoolean()
    if (!start) return

    bot.userInputLockedToOwners = true
    bot.setGlobalMessage("Auto saving in progress - please wait")
    delay(1.seconds)

    sendFrameSnapshot(dmChannel, autoSaver)

    dmChannel.createMessage {
        embed {
            title = "AutoSave Preparation"
            description = """
                |Input has been blocked for other users.
                |
                |Please finish any encounter or dialog and close all menus - so that the routine can start by opening the menu.
            """.trimMargin()
        }
        actionRow {
            interactionButton(
                ButtonStyle.Primary,
                componentId(PREPARATION_PHASE_ID, true.toString())
            ) { label = "Ready" }
            interactionButton(
                ButtonStyle.Danger,
                componentId(PREPARATION_PHASE_ID, false.toString())
            ) { label = "Cancel" }
        }
    }
}

private suspend fun onPreparationResponse(
    response: String,
    dmChannel: DmChannel,
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver
) {
    val ready = response.toBoolean()
    if (!ready) {
        return endSaveRoutine(bot)
    }

    emulator.clickButton(Button.START)
    delay(1.seconds)
    sendFrameSnapshot(dmChannel, autoSaver)

    dmChannel.createMessage {
        embed {
            title = "AutoSave Menu"
            description = """
                |Is the menu open? If not, please open it manually.
                |
                |Please select the input required to move the cursor on the **SAVE** option.
            """.trimMargin()
        }
        actionRow {
            selectMenu(componentId(MENU_PHASE_ID, "menu")) {
                allowedValues = 1..1

                listOf(
                    3 to "POKéMON",
                    2 to "ITEM",
                    1 to "TRAINER"
                ).forEach { (count, menuLabel) ->
                    option("$count - $menuLabel", "$count ${Button.DOWN}") {
                        emoji = DiscordPartialEmoji(name = Emojis.arrowDown.toReaction().name)
                    }
                }

                option("0 - SAVE", NO_INPUT_LABEL) {
                    emoji = DiscordPartialEmoji(name = Emojis.greenCircle.toReaction().name)
                }

                listOf(
                    1 to "OPTION",
                    2 to "EXIT"
                ).forEach { (count, menuLabel) ->
                    option("$count - $menuLabel", "$count ${Button.UP}") {
                        emoji = DiscordPartialEmoji(name = Emojis.arrowUp.toReaction().name)
                    }
                }

                option(CANCEL_LABEL, CANCEL_LABEL) {
                    emoji = DiscordPartialEmoji(name = Emojis.x.toReaction().name)
                }
            }
        }
    }
}

private suspend fun onMenuResponse(
    selection: List<String>,
    dmChannel: DmChannel,
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver
) {
    if (selection.isEmpty()) return

    when (val cursorInput = selection.first()) {
        CANCEL_LABEL -> return endSaveRoutine(bot)
        NO_INPUT_LABEL -> Unit
        else -> {
            val (count, buttonName) = cursorInput.split(" ", limit = 2)
            val button = Button.valueOf(buttonName)
            repeat(count.toInt()) {
                emulator.clickButton(button)
                delay(500.milliseconds)
            }
        }
    }
    emulator.clickButton(Button.A)
    delay(3.seconds)
    emulator.clickButton(Button.A)
    delay(6.seconds)

    sendFrameSnapshot(dmChannel, autoSaver)

    dmChannel.createMessage {
        embed {
            title = "AutoSave Confirmation"
            description = """
                |Auto saving done. Does everything look good? If not, please save manually now.
                |
                |Once ready, proceed to unlock input again.
            """.trimMargin()
        }
        actionRow {
            interactionButton(
                ButtonStyle.Success,
                componentId(CONFIRMATION_PHASE_ID, true.toString())
            ) { label = "Done" }
        }
    }
}

private suspend fun onConfirmationResponse(
    dmChannel: DmChannel,
    bot: DiscordBot
) {
    endSaveRoutine(bot)

    dmChannel.createMessage("Thanks 👍")

    val user = dmChannel.recipients.first()
    logger.info { "${user.username} saved the game" }
}

private fun endSaveRoutine(bot: DiscordBot) {
    bot.setGlobalMessage(null)
    bot.userInputLockedToOwners = false
}

private suspend fun sendFrameSnapshot(
    dmChannel: DmChannel,
    autoSaver: AutoSaver
) {
    dmChannel.createMessage {
        addFile(
            "snapshot.png",
            ChannelProvider {
                autoSaver.lastFrame.toByteArray().toInputStream().toByteReadChannel()
            }
        )
    }
}

class AutoSaver(
    private val config: Config,
    streamRenderer: StreamRenderer
) : StreamConsumer {
    private var routineJob: Job? = null
    internal lateinit var lastFrame: BufferedImage

    init {
        streamRenderer.addStreamConsumer(this)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun start(bot: DiscordBot, emulator: Emulator, kord: Kord) {
        routineJob = GlobalScope.launch(logAllExceptions) { runRoutine(bot, emulator, kord) }
    }

    fun stop() {
        routineJob?.cancel()
        routineJob = null
    }

    private suspend fun runRoutine(bot: DiscordBot, emulator: Emulator, kord: Kord) {
        val autoSaver = this
        while (coroutineContext.isActive) {
            coroutineScope {
                logAllExceptions {
                    val remindIn = Clock.System.now().untilNext(config.autoSaveRemindAt) + 1.minutes
                    logger.info { "Reminding to save in: $remindIn" }
                    delay(remindIn)

                    logger.info { "Reminding to save" }
                    config.owners.mapNotNull { kord.getUser(it) }
                        .mapNotNull { it.getDmChannelOrNull() }
                        .forEach {
                            launch(logAllExceptions) {
                                autoSaveConversation(autoSaver, it)
                            }
                        }
                }
            }
        }
    }

    override suspend fun acceptFrame(frame: BufferedImage) {
        lastFrame = frame
    }

    override suspend fun acceptGif(gif: ByteArray) {
        // Only interested in frames
    }
}

private val logger = KotlinLogging.logger {}

private const val AUTO_SAVE_ID_PREFIX = "discord_plays_auto_save"

private const val REMINDER_PHASE_ID = "1"
private const val PREPARATION_PHASE_ID = "2"
private const val MENU_PHASE_ID = "3"
private const val CONFIRMATION_PHASE_ID = "4"

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

private fun componentId(phase: String, value: String) =
    "$AUTO_SAVE_ID_PREFIX-$phase-$value-${UUID.randomUUID()}"
