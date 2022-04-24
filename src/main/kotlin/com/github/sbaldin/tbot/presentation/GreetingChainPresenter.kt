package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.presentation.base.DialogChain
import com.github.sbaldin.tbot.presentation.base.message.exactCommand
import com.github.sbaldin.tbot.presentation.base.message.isSentInLast5minutes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.ResourceBundle

class GreetingChainPresenter(locale: Locale) : DialogChain {

    private val showHelpKeyboard: String
    private val cancelHelpKeyboard: String
    private val finishDialogKeyboard: String
    private val greetingWordMsg: String
    private val greetingAboutBotMsg: String
    private val longHelpStoryMsg: String

    init {
        ResourceBundle.getBundle(getInitialChainLabel(), locale).apply {
            greetingWordMsg = getStringWithEmoji("greeting_dialog_hi_message")
            greetingAboutBotMsg = getStringWithEmoji("greeting_dialog_about_message")
            longHelpStoryMsg = getStringWithEmoji("greeting_dialog_help_msg")

            showHelpKeyboard = getStringWithEmoji("greeting_dialog_show_gif_keyboard")
            cancelHelpKeyboard = getStringWithEmoji("greeting_dialog_cancel_keyboard")
            finishDialogKeyboard = getStringWithEmoji("greeting_dialog_finish_keyboard")
        }
    }

    override fun chainPredicate(msg: Message): Boolean {
        return msg.isSentInLast5minutes() && msg.exactCommand("/start")
    }

    override fun chain(bot: Bot): ChainBuilder = bot.safeChain("greetings", ::chainPredicate) { msg ->
        bot.sendMessage(
            msg.chat.id,
            createGreetingMsg(msg),
            markup = ReplyKeyboardMarkup(
                resize_keyboard = true,
                one_time_keyboard = true,
                keyboard = listOf(
                    listOf(
                        KeyboardButton(showHelpKeyboard),
                        KeyboardButton(cancelHelpKeyboard),
                    ),
                ),
            ),
        )
    }.then { msg ->
        when (msg.text) {
            showHelpKeyboard -> bot.sendMessage(msg.chat.id, longHelpStoryMsg)
            cancelHelpKeyboard -> bot.sendMessage(msg.chat.id, finishDialogKeyboard)
        }
        bot.terminateChain(msg.chat.id)
    }

    override fun logger(): Logger = log
    override fun getInitialChainLabel(): String = "bot_dialogs"

    private fun createGreetingMsg(msg: Message): String {
        val greetingWithName = msg.from?.let { "$greetingWordMsg, ${it.first_name}!" } ?: "$greetingWordMsg!"
        return "$greetingWithName\n $greetingAboutBotMsg"
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(GreetingChainPresenter::class.java)
    }
}
