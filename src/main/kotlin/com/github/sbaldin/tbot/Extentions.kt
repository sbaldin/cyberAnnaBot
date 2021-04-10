package com.github.sbaldin.tbot

import com.vdurmont.emoji.EmojiParser
import java.util.ResourceBundle

fun Double.toPercentage(): Double {
    return this * 100
}

fun ResourceBundle.getStringWithEmoji(id: String): String = EmojiParser.parseToUnicode(getString(id))

fun String.toEmoji(): String = EmojiParser.parseToUnicode(this)