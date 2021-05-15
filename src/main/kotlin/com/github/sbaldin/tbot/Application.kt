package com.github.sbaldin.tbot

import com.github.sbaldin.tbot.domain.BirdClassifier
import com.github.sbaldin.tbot.data.BotConf
import com.github.sbaldin.tbot.data.CnnConf
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassifierInteractor
import com.github.sbaldin.tbot.presentation.GreetingChainPresenter
import com.github.sbaldin.tbot.presentation.GuessBirdByCmdChainPresenter
import com.github.sbaldin.tbot.presentation.BirdGuessingBot
import com.github.sbaldin.tbot.presentation.GuessBirdByChatMentionChainPresenter
import com.github.sbaldin.tbot.presentation.GuessBirdByChatPhotoChainPresenter
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.slf4j.LoggerFactory
import org.apache.log4j.PropertyConfigurator
import org.slf4j.Logger

import java.util.Properties

val log: Logger = LoggerFactory.getLogger(Application::class.java)

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
    private val appConfPath: String = System.getProperty("appConfig") ?: "./application-bot.yaml"
    private val appConf: BotConf = readBotConf(botConfPath = appConfPath)
    private val cnnConf: CnnConf = readCnnConf(cnnConfPath = appConfPath)

    fun configureLogger() {
        val props = Properties()
        props.load(Application::class.java.classLoader.getResourceAsStream("log4j.properties"))
        PropertyConfigurator.configure(props)
    }

    fun run() {
        log.info("Application config path:$appConfPath")
        val locale = appConf.locale()
        log.info("Application locale path:$locale")

        val classifier = BirdClassifier(cnnConf)
        val photoInteractor = PhotoInteractor(appConf.photoDestinationDir)
        val birdInteractor = BirdClassifierInteractor(classifier)

        val dialogs = listOf(
            GreetingChainPresenter(locale),
            GuessBirdByCmdChainPresenter(
                conf = appConf,
                photoInteractor = photoInteractor,
                birdInteractor = birdInteractor
            ),
            GuessBirdByChatPhotoChainPresenter(
                conf = appConf,
                photoInteractor = photoInteractor,
                birdInteractor = birdInteractor
            ),
            GuessBirdByChatMentionChainPresenter(
                conf = appConf,
                photoInteractor = photoInteractor,
                birdInteractor = birdInteractor
            )
        )

        BirdGuessingBot(
            appConf.name,
            appConf.token,
            dialogs
        ).start()
    }
}

fun main() {
    Application.configureLogger()
    log.info("Starting Telegram Cyber Anny Bot.")
    Application.run()
    log.info("The bot connected to telegram api.")
}
