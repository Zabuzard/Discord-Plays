package io.github.zabuzard.discordplays.discord.commands

import eu.rekawek.coffeegb.controller.ButtonListener.Button
import io.github.oshai.KotlinLogging
import io.github.zabuzard.discordplays.Config
import io.github.zabuzard.discordplays.Extensions.logAllExceptions
import io.github.zabuzard.discordplays.Extensions.toByteArray
import io.github.zabuzard.discordplays.discord.DiscordBot
import io.github.zabuzard.discordplays.emulation.Emulator
import io.github.zabuzard.discordplays.stream.StreamConsumer
import io.github.zabuzard.discordplays.stream.StreamRenderer
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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button.danger
import net.dv8tion.jda.api.interactions.components.buttons.Button.primary
import net.dv8tion.jda.api.interactions.components.buttons.Button.success
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun JDA.onAutoSaveConversation(
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver
) {
    addEventListener(object : ListenerAdapter() {
        override fun onButtonInteraction(event: ButtonInteractionEvent) {
            with(event) {
                val buttonId = button.id ?: ""
                if (!buttonId.startsWith(AUTO_SAVE_ID_PREFIX)) {
                    return
                }

                if (channel.type != ChannelType.PRIVATE) return
                val dmChannel = channel.asPrivateChannel()

                deferEdit().queue()
                val (_, phase, value, _) = componentId.split("-", limit = 4)
                dialogService.submit {
                    when (phase) {
                        REMINDER_PHASE_ID -> onReminderResponse(value, dmChannel, bot, autoSaver)
                        PREPARATION_PHASE_ID -> onPreparationResponse(
                            value,
                            dmChannel,
                            bot,
                            emulator,
                            autoSaver
                        )

                        CONFIRMATION_PHASE_ID -> onConfirmationResponse(dmChannel, bot)
                    }
                }
            }
        }

        override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
            with(event) {
                if (!componentId.startsWith(AUTO_SAVE_ID_PREFIX)) {
                    return
                }

                if (channel.type != ChannelType.PRIVATE) return
                val dmChannel = channel.asPrivateChannel()

                deferEdit().queue()
                dialogService.submit {
                    when (componentId.split("-", limit = 4)[1]) {
                        MENU_PHASE_ID -> onMenuResponse(values, dmChannel, bot, emulator, autoSaver)
                    }
                }
            }
        }
    })
}

fun autoSaveConversation(
    autoSaver: AutoSaver,
    dmChannel: PrivateChannel
) {
    sendFrameSnapshot(dmChannel, autoSaver)
    TimeUnit.SECONDS.sleep(3)

    val message = MessageCreateBuilder().addEmbeds(
        EmbedBuilder()
            .setTitle("AutoSave Reminder")
            .setDescription("It is time to save the game. Continue when ready.")
            .build()
    ).addActionRow(
        primary(componentId(REMINDER_PHASE_ID, true.toString()), "Start"),
        danger(componentId(REMINDER_PHASE_ID, false.toString()), "Skip")
    ).build()

    dmChannel.sendMessage(message).queue()
}

private fun onReminderResponse(
    response: String,
    dmChannel: PrivateChannel,
    bot: DiscordBot,
    autoSaver: AutoSaver
) {
    val start = response.toBoolean()
    if (!start) return

    bot.userInputLockedToOwners = true
    bot.setGlobalMessage("Auto saving in progress - please wait")
    TimeUnit.SECONDS.sleep(1)

    sendFrameSnapshot(dmChannel, autoSaver).flatMap {
        val message = MessageCreateBuilder().addEmbeds(
            EmbedBuilder()
                .setTitle("AutoSave Preparation")
                .setDescription(
                    """
                |Input has been blocked for other users.
                |
                |Please finish any encounter or dialog and close all menus - so that the routine can start by opening the menu.
                    """.trimMargin()
                ).build()
        )
            .addActionRow(
                primary(componentId(PREPARATION_PHASE_ID, true.toString()), "Ready"),
                danger(componentId(PREPARATION_PHASE_ID, false.toString()), "Cancel")
            )
            .build()

        dmChannel.sendMessage(message)
    }.queue()
}

private fun onPreparationResponse(
    response: String,
    dmChannel: PrivateChannel,
    bot: DiscordBot,
    emulator: Emulator,
    autoSaver: AutoSaver
) {
    val ready = response.toBoolean()
    if (!ready) {
        return endSaveRoutine(bot)
    }

    emulator.clickButton(Button.START).get()
    TimeUnit.SECONDS.sleep(1)
    sendFrameSnapshot(dmChannel, autoSaver).flatMap {
        val embed = EmbedBuilder()
            .setTitle("AutoSave Menu")
            .setDescription(
                """
                |Is the menu open? If not, please open it manually.
                |
                |Please select the input required to move the cursor on the **SAVE** option.
                """.trimMargin()
            )
            .build()

        val selectMenu = StringSelectMenu.create(componentId(MENU_PHASE_ID, "menu"))
        listOf(
            4 to "POKéDEX",
            3 to "POKéMON",
            2 to "ITEM",
            1 to "TRAINER"
        ).forEach { (count, menuLabel) ->
            selectMenu.addOption(
                "$count - $menuLabel",
                "$count ${Button.DOWN}",
                Emoji.fromUnicode("⬇")
            )
        }

        selectMenu.addOption("0 - SAVE", NO_INPUT_LABEL, Emoji.fromUnicode("🟢"))

        listOf(
            1 to "OPTION",
            2 to "EXIT"
        ).forEach { (count, menuLabel) ->
            selectMenu.addOption(
                "$count - $menuLabel",
                "$count ${Button.UP}",
                Emoji.fromUnicode("⬆")
            )
        }

        selectMenu.addOption(CANCEL_LABEL, CANCEL_LABEL, Emoji.fromUnicode("❌"))

        val message = MessageCreateBuilder()
            .setEmbeds(embed)
            .addActionRow(selectMenu.build())
            .build()

        dmChannel.sendMessage(message)
    }.queue()
}

private fun onMenuResponse(
    selection: List<String>,
    dmChannel: PrivateChannel,
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
                emulator.clickButton(button).get()
                TimeUnit.MILLISECONDS.sleep(500)
            }
        }
    }
    emulator.clickButton(Button.A).get()
    TimeUnit.SECONDS.sleep(3)
    emulator.clickButton(Button.A).get()
    TimeUnit.SECONDS.sleep(6)

    sendFrameSnapshot(dmChannel, autoSaver).flatMap {
        val message = MessageCreateBuilder().addEmbeds(
            EmbedBuilder()
                .setTitle("AutoSave Confirmation")
                .setDescription(
                    """
                |Auto saving done. Does everything look good? If not, please save manually now.
                |
                |Once ready, proceed to unlock input again.
                    """.trimMargin()
                ).build()
        )
            .addActionRow(success(componentId(CONFIRMATION_PHASE_ID, true.toString()), "Done"))
            .build()

        dmChannel.sendMessage(message)
    }.queue()
}

private fun onConfirmationResponse(
    dmChannel: PrivateChannel,
    bot: DiscordBot
) {
    endSaveRoutine(bot)

    dmChannel.sendMessage("Thanks 👍")
        .flatMap { dmChannel.retrieveUser() }
        .onSuccess { logger.info { "${it.name} saved the game" } }
        .queue()
}

private fun endSaveRoutine(bot: DiscordBot) {
    bot.setGlobalMessage(null)
    bot.userInputLockedToOwners = false
}

private fun sendFrameSnapshot(
    dmChannel: PrivateChannel,
    autoSaver: AutoSaver
) = dmChannel.sendFiles(
    FileUpload.fromData(
        autoSaver.lastFrame.toByteArray(),
        "snapshot.png"
    )
)

class AutoSaver(
    private val config: Config,
    streamRenderer: StreamRenderer
) : StreamConsumer {
    private val service = Executors.newSingleThreadScheduledExecutor()
    private var routineJob: ScheduledFuture<*>? = null
    internal lateinit var lastFrame: BufferedImage

    init {
        streamRenderer.addStreamConsumer(this)
    }

    fun start(bot: DiscordBot, emulator: Emulator, jda: JDA) = scheduleReminder(jda)

    fun stop() {
        routineJob?.cancel(false)
        routineJob = null
    }

    private fun scheduleReminder(jda: JDA) {
        val remindIn = Clock.System.now().untilNext(config.autoSaveRemindAt) + 1.minutes
        logger.info { "Reminding to save in: $remindIn" }

        routineJob = service.schedule(
            { logAllExceptions { runRoutine(jda) } },
            remindIn.inWholeSeconds,
            TimeUnit.SECONDS
        )
    }

    private fun runRoutine(jda: JDA) {
        logger.info { "Reminding to save" }
        config.owners.first()
            .let { jda.getUserById(it) }
            ?.openPrivateChannel()
            ?.onSuccess { autoSaveConversation(this, it) }
            ?.queue()

        if (routineJob?.isCancelled == false) {
            scheduleReminder(jda)
        }
    }

    override fun acceptFrame(frame: BufferedImage) {
        lastFrame = frame
    }

    override fun acceptGif(gif: ByteArray) {
        // Only interested in frames
    }
}

private val logger = KotlinLogging.logger {}

private val dialogService = Executors.newCachedThreadPool()

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
