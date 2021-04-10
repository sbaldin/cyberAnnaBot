package com.github.sbaldin.tbot.presentation.base

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.PhotoSize
import com.github.sbaldin.tbot.data.PhotoSizeModel
import com.vdurmont.emoji.EmojiParser
import java.util.ResourceBundle

interface DialogChain {

    fun chain(bot: Bot): ChainBuilder

    fun ResourceBundle.getStringWithEmoji(id: String): String = EmojiParser.parseToUnicode(getString(id))
}