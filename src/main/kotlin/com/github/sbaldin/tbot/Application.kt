package com.github.sbaldin.tbot

import com.github.sbaldin.tbot.domain.BirdClassifier
import com.github.sbaldin.tbot.domain.BotConf
import com.github.sbaldin.tbot.domain.CnnConf
import com.github.sbaldin.tbot.presentation.GreetingChainPresenter
import com.github.sbaldin.tbot.presentation.GuessBirdChainPresenter
import com.github.sbaldin.tbot.domain.BirdGuessingBot
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.slf4j.LoggerFactory
import org.apache.log4j.PropertyConfigurator
import org.slf4j.Logger

import java.util.Properties

fun readBotConf(
    resourcePath: String = "application-bot.yaml",
    botConfPath: String = ""
) = Config()
    .from.yaml.resource(resourcePath).from.yaml.file(botConfPath, optional = true).at("bot").toValue<BotConf>()

fun readCnnConf(
    resourcePath: String = "application-bot.yaml",
    cnnConfPath: String = ""
) = Config()
    .from.yaml.resource(resourcePath).from.yaml.file(cnnConfPath, optional = true).at("cnn").toValue<CnnConf>()

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        val props = Properties()
        props.load(Application::class.java.classLoader.getResourceAsStream("log4j.properties"))
        PropertyConfigurator.configure(props)
        log.info("Starting Telegram Cyber Anny Bot.")

        val appConfPath: String = System.getProperty("appConfig") ?: "./application-bot.yaml"
        log.info("Application config path:$appConfPath")
        val appConf: BotConf = readBotConf(botConfPath = appConfPath)

        val locale = appConf.locale()
        BirdGuessingBot(
            appConf.name,
            appConf.token,
            listOf(
                GreetingChainPresenter(locale),
                GuessBirdChainPresenter(
                    BirdClassifier(readCnnConf(cnnConfPath = appConfPath)),
                    appConf.token,
                    locale
                )
            )
        ).init()
        log.info("The bot connected to telegram api.")
    }
}

val log: Logger = LoggerFactory.getLogger(Application::class.java)
const val RECONNECT_PAUSE = 10_000L
