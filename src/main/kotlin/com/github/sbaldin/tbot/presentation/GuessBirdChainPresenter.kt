package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.PhotoSize
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.domain.BirdClassifier
import com.github.sbaldin.tbot.domain.classifier.toPercentage
import com.github.sbaldin.tbot.domain.enum.BirdNameEnum
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

class GuessBirdChainPresenter(
    private val birdClassifier: BirdClassifier,
    private val token: String,
    locale: Locale
) : DialogChain {

    val birdNamesRes = ResourceBundle.getBundle("bird_name", locale)

    private val startDialogMsg: String
    private val abortDialogMsg: String
    private val guessingInProgressMsg: String
    private val hypothesisMsg: String
    private val finishDialogMsg: String

    private val guessingSuccessKeyboard: String
    private val guessingFailKeyboard: String

    init {
        ResourceBundle.getBundle("bot_dialogs", locale).apply {
            startDialogMsg = getString("find_bird_dialog_start_message")
            abortDialogMsg = getString("find_bird_dialog_abort_message")
            guessingInProgressMsg = getString("find_bird_dialog_in_progress_message")
            hypothesisMsg = getString("find_bird_dialog_hypothesis_message")
            finishDialogMsg = getString("find_bird_dialog_finish_message")

            guessingSuccessKeyboard = getString("find_bird_dialog_success_message")
            guessingFailKeyboard = getString("find_bird_dialog_fail_message")
        }
    }


    override fun chain(bot: Bot): ChainBuilder = bot.chain(trigger = "/guessBird") { msg ->
        bot.sendMessage(msg.chat.id, startDialogMsg)
    }.then(label = "photo_recognize_step") { msg ->
        if (msg.new_chat_photo == null && msg.photo == null) {
            bot.sendMessage(msg.chat.id, abortDialogMsg)
            bot.terminateChain(msg.chat.id)
            return@then
        }
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)

        val photos = msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()
        val localFile = photos.run { getBiggestPhotoAndSaveLocally(bot) }
        val bestBird = birdClassifier.getBirdClassDistribution(localFile).birdById.values.maxByOrNull { it.rate }!!

        val birdName = birdNamesRes.getString(BirdNameEnum.fromId(bestBird.id).name)

        bot.sendMessage(
            msg.chat.id,
            MessageFormat.format(
                hypothesisMsg,
                birdName,
                bestBird.rate.toPercentage()
            ),
            markup = ReplyKeyboardMarkup(
                resize_keyboard = true,
                one_time_keyboard = true,
                keyboard = listOf(
                    listOf(
                        KeyboardButton(guessingSuccessKeyboard),
                        KeyboardButton(guessingFailKeyboard)
                    )
                )
            )
        )

    }.then(isTerminal = true) { msg ->
        bot.sendMessage(msg.chat.id, finishDialogMsg)
        bot.terminateChain(msg.chat.id)
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

    companion object {
        val log: Logger = LoggerFactory.getLogger(GuessBirdChainPresenter::class.java)
    }
}