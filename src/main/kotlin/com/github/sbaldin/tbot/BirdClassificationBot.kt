package com.github.sbaldin.tbot

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.jumpTo
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.PhotoSize
import com.elbekD.bot.types.ReplyKeyboardMarkup
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.GetFile
import java.io.File
import java.net.URL

class BirdClassificationBot(
    private val botName: String,
    private val token: String,
    private val birdClassifier: BirdClassifier
) {

    fun start() {
        val bot = Bot.createPolling(botName, token) {
            limit = 50
            timeout = 30
            removeWebhookAutomatically = true
            period = 1000
        }
        addLogFilter(bot)
        buildChains(bot)
        bot.start()
    }
    private fun addLogFilter(bot: Bot) {
        bot.onMessage { msg ->
            log.info("msg:$msg")
        }
    }

    private fun buildChains(bot: Bot) {
        greetingsChain(bot)
        findBirdChain(bot)
    }

    private fun greetingsChain(bot: Bot) {
        bot.chain("/start") { msg ->
            bot.sendMessage(msg.chat.id, createGreetingMsg(msg))
            bot.terminateChain(msg.chat.id)
        }.build()
    }

    private fun findBirdChain(bot: Bot) {
        bot.chain(trigger = "/findBird") { msg ->
            bot.sendMessage(msg.chat.id, "Окей, я попробую отгадать имя птицы, пришли мне фото...")
        }.then(label = "photo_recognize_step") { msg ->
            if (msg.new_chat_photo == null && msg.photo == null) {
                bot.sendMessage(msg.chat.id, "Я не нашла фото в последнем сообщении, процедура поиска птиц прекращена!")
                bot.terminateChain(msg.chat.id)
                return@then
            }
            bot.sendMessage(msg.chat.id, "Запускаю поиск птиц...")
            val photos = msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()
            val localFile = photos.run { getBiggestPhotoAndSaveLocally(bot) }
            val bestBird = birdClassifier.getBirdClassDistribution(localFile).birdById.values.maxByOrNull { it.rate }!!
            bot.sendMessage(
                msg.chat.id,
                "Это ${bestBird.title} с вероятностю ${bestBird.rate.toPercentage()}...\nЯ права?",
                markup = ReplyKeyboardMarkup(
                    resize_keyboard = true,
                    one_time_keyboard = true,
                    keyboard = listOf(listOf(KeyboardButton("Да, ты молодец!"), KeyboardButton("Хм, ну не знаю...")))
                )
            )

        }.then(isTerminal = true) { msg ->
            bot.sendMessage(msg.chat.id, "Ну вот и ладушки!")
            bot.terminateChain(msg.chat.id)

        }.build()
    }

    private fun List<PhotoSize>.getBiggestPhotoAndSaveLocally(bot: Bot): File {
        val biggestPhoto = maxByOrNull { it.file_size }!!
        val fileId = biggestPhoto.file_id

        log.info("Step:photo_recognize_step:File id is $fileId")

        val fileUrl = "https://api.telegram.org/file/bot$token/${bot.getFile(fileId).get().file_path}"
        val stream = URL(fileUrl).openStream()
        val localFile = File.createTempFile(
            "telegram",
            "jpg"
        )
        FileUtils.copyInputStreamToFile(stream, localFile)
        return localFile;
    }

    private fun createGreetingMsg(msg: Message): String {
        val greetings = msg.from?.let { "Привет, ${it.first_name}!" } ?: "Привет!"
        return "$greetings Меня зовут Кибер-Аня, и я отгадываю птиц!\nПошли мне фото или ссылку на птицу, а я назову тебе ее имя!"
    }

    companion object {
        val log = LoggerFactory.getLogger(BirdClassificationBot::class.java)
    }
}


