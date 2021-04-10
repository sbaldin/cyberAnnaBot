package com.github.sbaldin.tbot

import com.elbekD.bot.types.Message
import com.elbekD.bot.types.PhotoSize
import com.github.sbaldin.tbot.data.PhotoSizeModel
import com.vdurmont.emoji.EmojiParser
import java.util.ResourceBundle

fun Double.toPercentage(): Double {
    return this * 100
}

fun Message.hasPhoto(): Boolean {
    val hasNoPhoto =  new_chat_photo.isNullOrEmpty() && photo.isNullOrEmpty()
    return hasNoPhoto.not()
}