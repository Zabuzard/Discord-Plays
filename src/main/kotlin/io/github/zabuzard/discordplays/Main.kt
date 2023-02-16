package io.github.zabuzard.discordplays

import me.jakejmattson.discordkt.dsl.bot

fun main(args: Array<String>) {
    require(args.size == 1) {
        "Incorrect number of arguments. The first argument must be the bot token. " +
                "Supplied arguments: ${args.contentToString()}"
    }

   val token = args.first()


    bot(token) {
    }
}
