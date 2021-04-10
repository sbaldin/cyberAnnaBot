package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.github.sbaldin.tbot.presentation.base.DialogChain
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BirdGuessingBot(
    private val botName: String,
    private val token: String,
    private val dialogs: List<DialogChain>
) {

    fun start() {
        log.info("Bot Initialize.")
        val bot = Bot.createPolling(botName, token) {
            limit = 50
            timeout = 30
            removeWebhookAutomatically = true
            period = 1000
        }
        addLogFilter(bot)
        buildChains(bot)
        log.info("Bot Initialization finished.")
        bot.start()
        log.info("Bot has been started.")
    }

    private fun addLogFilter(bot: Bot) {
        bot.onMessage { msg ->
            log.info("msg:$msg")
        }
    }

    private fun buildChains(bot: Bot) {
        dialogs.forEach {
            log.info("Build ${it::class.java.name} chain.")
            it.chain(bot).build()
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(BirdGuessingBot::class.java)
    }
}