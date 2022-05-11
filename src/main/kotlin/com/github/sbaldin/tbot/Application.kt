package com.github.sbaldin.tbot

import com.github.sbaldin.tbot.data.BotConf
import com.github.sbaldin.tbot.data.CnnConf
import com.github.sbaldin.tbot.domain.detection.ObjectDetector
import com.github.sbaldin.tbot.presentation.BirdGuessingBot
import com.github.sbaldin.tbot.presentation.GreetingChainPresenter
import com.github.sbaldin.tbot.presentation.GuessBirdByChatMentionChainPresenter
import com.github.sbaldin.tbot.presentation.GuessBirdByChatPhotoChainPresenter
import com.github.sbaldin.tbot.presentation.GuessBirdByCmdChainPresenter
import com.github.sbaldin.tbot.presentation.base.DialogChain
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.apache.log4j.PropertyConfigurator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.Properties

val log: Logger = LoggerFactory.getLogger(Application::class.java)

class ApplicationModule : AbstractModule() {

    override fun configure() {
        bindConstant().annotatedWith(AppConfPathOnDisk::class.java).to(System.getProperty("appConfig") ?: "./application-bot.yaml")
        bindConstant().annotatedWith(AppConfPathInResources::class.java).to("application-bot.yaml")
        val multiBinder = Multibinder.newSetBinder(binder(), DialogChain::class.java)
        multiBinder.addBinding().to(GreetingChainPresenter::class.java)
        multiBinder.addBinding().to(GuessBirdByCmdChainPresenter::class.java)
        multiBinder.addBinding().to(GuessBirdByChatPhotoChainPresenter::class.java)
        multiBinder.addBinding().to(GuessBirdByChatMentionChainPresenter::class.java)
    }

    @Provides
    fun readBotConf(
        @AppConfPathInResources
        resourcePath: String = "application-bot.yaml",
        @AppConfPathOnDisk
        botConfPath: String = "",
    ): BotConf = Config()
        .from.yaml.resource(resourcePath)
        .from.yaml.file(botConfPath, optional = true)
        .at("bot").toValue()

    @Provides
    fun readCnnConf(
        @AppConfPathInResources
        resourcePath: String = "application-bot.yaml",
        @AppConfPathOnDisk
        cnnConfPath: String = "",
    ): CnnConf = Config()
        .from.yaml.resource(resourcePath)
        .from.yaml.file(cnnConfPath, optional = true)
        .at("cnn").toValue()

    @Provides
    @PhotoDestinationDirectory
    fun photoDestinationDirectory(conf: BotConf) = conf.photoDestinationDir

    @Provides
    @BotLocale
    fun botLocale(conf: BotConf): Locale = Locale(conf.locale)

    @Provides
    @BotName
    fun botName(conf: BotConf) = conf.name

    /**
     * In the ideal world BotConf should be split and private things like token should be moved into dedicated model that restrict its usage
     * @see <a href="https://github.com/google/guice/wiki/RestrictedBindingSource">RestrictedBindingSource</a>
     */
    @Provides
    @BotToken
    fun botToken(conf: BotConf) = conf.token
}

class Application @Inject constructor(
    @AppConfPathOnDisk
    val appConfPath: String,
    val botConf: BotConf,
    val bot: BirdGuessingBot,
) {

    private fun configureLogger() {
        val props = Properties()
        props.load(Application::class.java.classLoader.getResourceAsStream("log4j.properties"))
        PropertyConfigurator.configure(props)
    }

    fun run() {
        configureLogger()
        log.info("Application config path:$appConfPath")
        log.info("Bot configuration:\n$botConf")
        bot.start()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val injector = Guice.createInjector(ApplicationModule())
            log.info("Starting Telegram Cyber Anny Bot.")
            injector.getInstance(Application::class.java).run()
            log.info("Bot connected to telegram api.")
        }
    }
}
