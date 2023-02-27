# Discord Plays ![emoji](https://i.imgur.com/Hs2Wohm.png)


[![codefactor](https://img.shields.io/codefactor/grade/github/zabuzard/discord-plays)](https://www.codefactor.io/repository/github/zabuzard/discord-plays)
![Kotlin](https://img.shields.io/badge/Kotlin-1.8.0-ff696c)
[![license](https://img.shields.io/github/license/Zabuzard/Discord-Plays)](https://github.com/Zabuzard/Discord-Plays/blob/master/LICENSE)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Zabuzard/Discord-Plays?label=release)

Discord bot to play games together - cause the crowd is just better at it! 🐟🎮

Inspired by Twitch Plays Pokémon. Feel free to contribute or tell us how you like it 🤙

![demo](https://i.imgur.com/SGTyCZC.gif)

## Features

* supports any Gameboy or Gameboy Color ROM
* cross-community play and chat
* local display without lag, direct keyboard input
* recording and video creation
* statistics
* various moderation commands
* configurable, resilient

As of today, we are not planning on hosting the bot ourselves, or making global
events available to everyone.

We encourage you to download the bot and self-host your own events! 👍

## Get started

Download the latest version from
our [release section](https://github.com/Zabuzard/Discord-Plays/releases).
Run it from console using

```shell
# Example: java -jar Discord-Plays-1.0-standalone.jar MzE2OTA4Mzk0OTQ0OTQ2NTgx.XbBkcJ.TpW6sUjEoNGv539Lxi-eqfbCl4R
java -jar <name_of_the_jar> <your_discord_bot_token>
```

On first startup, it will create a `config.json` file, which you can freely edit to your needs.
At minimum, make sure to configure `romPath` and `owners`. Latter will be the ID of your own Discord
account.

The bot should be up and running now. Start the game emulation using `/owner start`.
At this point, all participating communities can set up their stream by using `/host stream`,
which will create the message containing the stream, the interaction buttons and a thread for
displaying
statistics and chatting.

Initially, input is locked for everyone except you (the owner).
To unlock input, run `/owner lock-input lock:False`.

As owner, it can be helpful to occasionally run the game locally without lag and
control the game directly. This can be done using `/owner local-display activate:True`,
optionally even with sound. The controls are:

* <kbd>W</kbd><kbd>A</kbd><kbd>S</kbd><kbd>D</kbd>
  or <kbd>↑</kbd><kbd>←</kbd><kbd>↓</kbd><kbd>→</kbd>
* A: <kbd>Q</kbd>, <kbd>SPACE</kbd>
* B: <kbd>E</kbd>, <kbd>BACKSPACE</kbd>, <kbd>ESC</kbd>
* Start: <kbd>R</kbd>, <kbd>ENTER</kbd>
* Select: <kbd>T</kbd>, <kbd>DEL</kbd>

![get started](https://i.imgur.com/WLfyCjg.gif)

### Overview

A table of all commands can be
seen [here](https://github.com/Zabuzard/Discord-Plays/blob/master/commands.md).

The permission system is divided into 3 roles:

* Owner - responsible for the event, full access, that's you
* Host - responsible for streaming the event in their community, moderators
* User - everyone else

#### Owner

Owners need at least **Admin** permission in Discord and are configured
manually in the config, or via `/owner add-owner`. The main commands
are `/start`, `/stop` and `/lock-input`.

To notify all users, you can use `/global-message` (banner in the stream itself)
and `/chat-message` (message in the threads).
Troublemakers can be excluded from the event with `/ban` (undo by editing the config).

Configure the event to your liking using `/game-metadata` and use `/clear-stats`
when starting a new run.

##### Recording

The system automatically records the game by saving frames as pictures in the `recordingPath`
specified in the config.

![recording](https://i.imgur.com/EiEv3cB.png)

This consumes around 2 GB per 24h of footage.

The images can be turned into a video (`.mp4`) by running the `/owner create-video` command,
giving it the name of the folder, e.g. `2023-02-27`. The video is created in the same folder.

The command requires the system to have [ffmpeg](https://ffmpeg.org/) installed.

##### Auto-save

The system lacks a fully automatic save mechanism. Because of that, it is necessary to
manually save the game sometimes.

To ease that workflow, the bot automatically reminds all owners once per day
(see `autoSaveRemindAt` in the config) to save the game, and walks you through
a semi-automatic save routine specialized for Pokémon.

This routine can also be triggered manually using `/owner save`.

![auto-save](https://i.imgur.com/v8zI1V1.gif)

#### Host

Hosts are, by default, all users with **Moderator** permission in Discord.
This can be configured with Discords built-in integration settings.

![integration settings](https://i.imgur.com/3Ct0buk.png)

Hosts can create the actual stream message and thread in a channel of their community
using `/host stream`. To stop the stream for this community,
just delete the bots message or the thread. Afterwards, a stream can be started,
for example in a different channel, with the same command again.

The `/host community-message` command allows to attach a message to the stream,
readable only within this community.

![community-message](https://i.imgur.com/Kj4PwkH.png)

## Contributing

Thanks for considering improving this project - we hope you like it 🤙

Feel free to post all your ideas as [issues](https://github.com/Zabuzard/Discord-Plays/issues).
If you want, you can try to implement some of them and propose a **Pull Request**.
As of now, there are no guidelines you have to follow - just try to write readable code 🙂

### Tech Stack

The main technology being used are

* Kotlin
* Gradle
* [DiscordKt](https://github.com/DiscordKt/DiscordKt) as Discord framework
* [Coffee GB](https://github.com/trekawek/coffee-gb) as emulator

### Architecture

The code base is divided into several main packages:

* Discord
* Local
* Emulation
* Stream

![architecture](https://i.imgur.com/FPJ9Mm4.png)

The flow starts in `Main.kt`, starting the bot using the `Config.kt`
and booting up all `@Service`s via dependency injection.

Game emulation is represented and controlled by `Emulator.kt`. Almost all flows
start with invoking a command in either `OwnerCommands` or `HostCommands`, which
usually end up triggering a method in the main controller class `DiscordBot`.

The emulator pushes the graphics to `StreamRenderer`, which scales the game image,
adds overlays and more. From there on, all rendered frames are exposed mainly
to `DiscordBot`, `LocalDisplay` and `FrameRecorder`.
