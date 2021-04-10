package com.github.sbaldin.tbot

import com.elbekD.bot.types.Message

fun Double.toPercentage(): Double {
    return this * 100
}

fun Message.hasPhoto(): Boolean {
    val hasNoPhoto = new_chat_photo.isNullOrEmpty() && photo.isNullOrEmpty()
    return hasNoPhoto.not()
}