package com.github.sbaldin.tbot.presentation.base.message

import com.elbekD.bot.types.Message
import java.time.Duration
import java.time.Instant

/*
 * File contains predicate functions that are used in chain presenters to determinate
 * either chain should handle the incoming message or should ignore it.
 */

fun Message.isSentInLast5minutes(): Boolean {
    val now = Instant.now()
    val msgDate = Instant.ofEpochMilli(date * 1000L)

    val diff = Duration.between(msgDate, now).toSeconds()
    val fiveMinuteInterval = 300

    return diff < fiveMinuteInterval
}

fun Message.exactCommand(cmd: String): Boolean = this.text == cmd
