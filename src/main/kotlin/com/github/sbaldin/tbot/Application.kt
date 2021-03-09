package com.github.sbaldin.tbot

import com.github.sbaldin.tbot.domain.BotConf
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun readBotConf(
    resourcePath: String = "application-bot.yaml"
) = Config()
    .from.yaml.resource(resourcePath).at("bot").toValue<BotConf>()

object Application {
    val log = LoggerFactory.getLogger(Application::class.java)
    val RECONNECT_PAUSE = 10_000L

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting Telegram Cyber Anny Bot.")
        val appConf: BotConf = readBotConf()

        //System.getProperties()["proxySet"] = "true";
        // System.getProperties()["socksProxyHost"] = "ip";
        // System.getProperties()["socksProxyPort"] = "port";

        botConnect(appConf)
        log.info("The bot has been shut down.")
    }

    private fun botConnect(appConf: BotConf) {
        try {
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(CyberAnnyBot(appConf.name, appConf.token))
            log.info("TelegramAPI started. Look for messages")
        } catch (e: TelegramApiRequestException) {
            log.error("Cant Connect. Pause " + (RECONNECT_PAUSE / 1000).toString() + "sec and try again. Error: " + e.message)
            log.error("Error", e)
            try {
                Thread.sleep(RECONNECT_PAUSE)
            } catch (e1: InterruptedException) {
                e1.printStackTrace()
            }
            botConnect(appConf)
        }
    }
}