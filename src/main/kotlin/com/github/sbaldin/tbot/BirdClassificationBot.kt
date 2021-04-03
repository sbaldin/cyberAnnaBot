package com.github.sbaldin.tbot

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.PhotoSize
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.domain.BirdNameEnum
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.text.MessageFormat
import java.util.ResourceBundle

import java.util.Locale


class BirdClassificationBot(
    private val botName: String,
    private val token: String,
    private val birdClassifier: BirdClassifier,
    locale: Locale = Locale("ru", "Ru")
) {

    val birdNamesRes = ResourceBundle.getBundle("bird_name", locale)
    val botDialogRes = ResourceBundle.getBundle("bot_dialogs", locale)


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
        val showHelpKeyboard = botDialogRes.getString("greetings_dialog_show_gif_keyboard")
        val finishDialogKeyboard = botDialogRes.getString("greetings_dialog_finish_keyboard")
        bot.chain("/start") { msg ->
            bot.sendMessage(
                msg.chat.id,
                createGreetingMsg(msg),
                markup = ReplyKeyboardMarkup(
                    resize_keyboard = true,
                    one_time_keyboard = true,
                    keyboard = listOf(
                        listOf(
                            KeyboardButton(showHelpKeyboard + "\uD83D\uDC66\uD83C\uDFFF"),
                            KeyboardButton(finishDialogKeyboard)
                        )
                    )
                )
            )

        }.then { msg ->
            when(msg.text){
                showHelpKeyboard -> bot.sendMessage(msg.chat.id,"Gif will be here")
                finishDialogKeyboard -> bot.sendMessage(msg.chat.id, "Answer something")
            }
            log.info(msg.text)
            bot.terminateChain(msg.chat.id)
        }.build()
    }

    private fun findBirdChain(bot: Bot) {
        bot.chain(trigger = "/findBird") { msg ->
            bot.sendMessage(msg.chat.id, botDialogRes.getString("find_bird_dialog_start_message"))
        }.then(label = "photo_recognize_step") { msg ->
            if (msg.new_chat_photo == null && msg.photo == null) {
                bot.sendMessage(msg.chat.id, botDialogRes.getString("find_bird_dialog_abort_message"))
                bot.terminateChain(msg.chat.id)
                return@then
            }
            bot.sendMessage(msg.chat.id, botDialogRes.getString("find_bird_dialog_in_progress_message"))

            val photos = msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()
            val localFile = photos.run { getBiggestPhotoAndSaveLocally(bot) }
            val bestBird = birdClassifier.getBirdClassDistribution(localFile).birdById.values.maxByOrNull { it.rate }!!

            val hypothesis_msg = botDialogRes.getString("find_bird_dialog_hypothesis_message")
            val birdName = birdNamesRes.getString(BirdNameEnum.fromId(bestBird.id).name)

            bot.sendMessage(
                msg.chat.id,
                MessageFormat.format(
                    hypothesis_msg,
                    birdName,
                    bestBird.rate.toPercentage()
                ),
                markup = ReplyKeyboardMarkup(
                    resize_keyboard = true,
                    one_time_keyboard = true,
                    keyboard = listOf(
                        listOf(
                            KeyboardButton(botDialogRes.getString("find_bird_dialog_success_message")),
                            KeyboardButton(botDialogRes.getString("find_bird_dialog_fail_message"))
                        )
                    )
                )
            )

        }.then(isTerminal = true) { msg ->
            bot.sendMessage(msg.chat.id, botDialogRes.getString("find_bird_dialog_finish_message"))
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
        return localFile
    }

    private fun createGreetingMsg(msg: Message): String {
        val greetings = botDialogRes.getString("greetings_dialog_hi_message")
        val greetingsWithName = msg.from?.let { "$greetings, ${it.first_name}!" } ?: "$greetings!"
        return "$greetingsWithName ${botDialogRes.getString("greetings_dialog_about_message")}"
    }

    companion object {
        val log = LoggerFactory.getLogger(BirdClassificationBot::class.java)
    }
}


