package io.github.zabuzard.discordplays

import io.ktor.utils.io.printStack

object Extensions {
    fun Runnable.logAllExceptions() = Runnable {
        try {
            this@logAllExceptions.run()
        } catch (e: Throwable) {
            e.printStack()
        }
    }

    fun (() -> Unit).logAllExceptions() = Runnable {
        try {
            invoke()
        } catch (e: Throwable) {
            e.printStack()
        }
    }

    fun <T> ((T) -> Unit).logAllExceptions(): ((T) -> Unit) = {
        try {
            this.invoke(it)
        } catch (e: Throwable) {
            e.printStack()
        }
    }
}
