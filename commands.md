# Commands

## Key 
| Symbol      | Meaning                        |
|-------------|--------------------------------|
| [Argument]  | Argument is not required.      |
| /Category   | This is a subcommand group.    |

## /host
| Commands          | Arguments | Description                                                            |
|-------------------|-----------|------------------------------------------------------------------------|
| community-message | [message] | Attaches a community-wide message to the stream hosted in this channel |
| stream            |           | Starts your game stream in this channel                                |

## /owner
| Commands       | Arguments         | Description                                                       |
|----------------|-------------------|-------------------------------------------------------------------|
| add-owner      | user              | Give another user owner-permission                                |
| ban            | user              | Bans an user from the event, their input will be blocked          |
| chat-message   | message           | Sends a message to the chats of all hosts                         |
| clear-stats    |                   | Clears all statistics, use when starting a new run                |
| create-video   | date              | Creates a video out of the recorded frames                        |
| game-metadata  | entity, value     | Change the metadata of the game played                            |
| global-message | [message]         | Attaches a global message to the stream                           |
| local-display  | activate, [sound] | Activates a local display on the bots machine for manual control. |
| lock-input     | lock              | Only allows user input from owners, blocks any other input        |
| log-level      | level             | Changes the log level                                             |
| save           |                   | Starts the auto-save dialog out of its automatic schedule         |
| start          |                   | Starts the game emulation                                         |
| stop           |                   | Stops the game emulation                                          |

## Utility
| Commands | Arguments | Description                |
|----------|-----------|----------------------------|
| Help     | [Command] | Display a help menu.       |
| info     |           | Bot info for Discord Plays |

