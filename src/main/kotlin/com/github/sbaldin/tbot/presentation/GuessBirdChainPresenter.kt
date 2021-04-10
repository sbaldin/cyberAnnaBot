package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.PhotoSize
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.data.PhotoSizeModel
import com.github.sbaldin.tbot.data.enum.BirdNameEnum
import com.github.sbaldin.tbot.domain.BiggestPhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassifierInteractor
import com.github.sbaldin.tbot.getStringWithEmoji
import com.github.sbaldin.tbot.toEmoji
import com.github.sbaldin.tbot.toPercentage
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

class GuessBirdChainPresenter(
    locale: Locale,
    private val token: String,
    private val photoInteractor: BiggestPhotoInteractor,
    private val birdClassifierInteractor: BirdClassifierInteractor
) : DialogChain {

    // TODO: I am not sure that kt-telegram-bot-1.3.8.jar provides thread-safe api when you work with chains, investigate how to do it right
    private val birdClassDistributionByChatId = ConcurrentHashMap<Long, BirdClassDistributionModel>()
    private val birdNamesRes: ResourceBundle = ResourceBundle.getBundle("bird_name", locale)

    private val startDialogMsg: String
    private val abortDialogMsg: String
    private val guessingInProgressMsg: String
    private val hypothesisMsg: String
    private val hypothesisShortMsg: String
    private val finishSuccessDialogMsg: String
    private val finishFailDialogMsg: String

    private val guessingSuccessKeyboard: String
    private val guessingFailKeyboard: String

    init {
        ResourceBundle.getBundle("bot_dialogs", locale).apply {
            startDialogMsg = getStringWithEmoji("find_bird_dialog_start_message")
            abortDialogMsg = getStringWithEmoji("find_bird_dialog_abort_message")
            guessingInProgressMsg = getStringWithEmoji("find_bird_dialog_in_progress_message")
            hypothesisMsg = getStringWithEmoji("find_bird_dialog_hypothesis_message")
            hypothesisShortMsg = getStringWithEmoji("find_bird_dialog_hypothesis_short_message")
            finishSuccessDialogMsg = getStringWithEmoji("find_bird_dialog_success_finish_message")
            finishFailDialogMsg = getStringWithEmoji("find_bird_dialog_fail_finish_message")

            guessingSuccessKeyboard = getStringWithEmoji("find_bird_dialog_success_message")
            guessingFailKeyboard = getStringWithEmoji("find_bird_dialog_fail_message")
        }
    }

    override fun chain(bot: Bot): ChainBuilder = bot.chain(trigger = "/guessBird") { msg ->
        bot.sendMessage(msg.chat.id, startDialogMsg)
    }.then(label = "photo_recognize_step") { msg ->
        if (msg.new_chat_photo == null && msg.photo == null) {
            abortChain(bot, msg.chat.id, abortDialogMsg)
            return@then
        }
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)

        val birdDistribution = getBirdClassDistribution(msg, bot)
        val bestBird = birdClassifierInteractor.getBirdWithHighestRate(birdDistribution)
        birdClassDistributionByChatId[msg.chat.id] = birdDistribution

        bot.sendMessage(
            chatId = msg.chat.id,
            text = MessageFormat.format(
                hypothesisMsg,
                getBirdName(bestBird),
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
    }.then { msg ->
        var lastDialogMsg = ""
        when (msg.text) {
            guessingSuccessKeyboard -> {
                lastDialogMsg = finishSuccessDialogMsg
            }
            guessingFailKeyboard -> {
                val distribution = birdClassDistributionByChatId.getValue(msg.chat.id)
                val fiveBirdsMsg = birdClassifierInteractor.getBirdsWithHighestRate(distribution).mapIndexed { index, bird ->
                    birdWithEmojiNumber(index, bird)
                }.joinToString("")
                lastDialogMsg = finishFailDialogMsg + fiveBirdsMsg
            }
        }
        abortChain(bot, msg.chat.id, lastDialogMsg)
    }

    private fun getBirdName(bestBird: BirdClassModel) =
        birdNamesRes.getString(BirdNameEnum.fromId(bestBird.id).name)

    private fun abortChain(bot: Bot, chatId: Long, message: String) {
        bot.sendMessage(chatId, message)
        bot.terminateChain(chatId)
    }

    private fun getBirdClassDistribution(
        msg: Message,
        bot: Bot
    ): BirdClassDistributionModel {
        val photos = (msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()).map { it.toPhotoSizeModel() }
        val localFile = photoInteractor.saveBiggestPhotoAsTempFile(photos) { fileId ->
            "https://api.telegram.org/file/bot$token/${bot.getFile(fileId).get().file_path}"
        }

        return birdClassifierInteractor.calcBirdClassDistribution(localFile)
    }

    private fun birdWithEmojiNumber(index: Int, bird: BirdClassModel): String {
        val birdMsg = MessageFormat.format(
            hypothesisShortMsg,
            getBirdName(bird),
            bird.rate.toPercentage()
        )
        val indexEmoji = when (index) {
            0 -> ":one:"
            1 -> ":two:"
            2 -> ":three:"
            3 -> ":four:"
            4 -> ":five:"
            else -> ":information_source:"
        }.toEmoji()

        return "$indexEmoji $birdMsg;\n"
    }
}

private fun PhotoSize.toPhotoSizeModel(): PhotoSizeModel = PhotoSizeModel(
    fileId = file_id,
    fileSize = file_size,
    width = width,
    height = height
)