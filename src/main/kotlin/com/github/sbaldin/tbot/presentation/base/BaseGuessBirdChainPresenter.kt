package com.github.sbaldin.tbot.presentation.base

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.PhotoSize
import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.data.PhotoSizeModel
import com.github.sbaldin.tbot.data.enum.BirdNameEnum
import com.github.sbaldin.tbot.domain.BiggestPhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassifierInteractor
import com.github.sbaldin.tbot.toPercentage
import com.vdurmont.emoji.EmojiParser
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

abstract class BaseGuessBirdChainPresenter(
    locale: Locale,
    private val token: String,
    private val photoInteractor: BiggestPhotoInteractor,
    protected val birdInteractor: BirdClassifierInteractor
) : DialogChain {

    protected val startChainPredicates =
        listOf("/чтозаптица", "/чезаптица", "/чезапетух", "/guessBird", "/findBird", "/whatTheBird", "/bird")

    // TODO: I am not sure that kt-telegram-bot-1.3.8.jar provides thread-safe api when you work with chains, investigate how to do it right
    protected val birdClassDistributionByChatId = ConcurrentHashMap<Long, BirdClassDistributionModel>()
    protected val birdNamesRes: ResourceBundle = ResourceBundle.getBundle("bird_name", locale)

    protected val startDialogMsg: String
    protected val abortDialogMsg: String
    protected val guessingInProgressMsg: String
    protected val hypothesisMsg: String
    protected val hypothesisShortMsg: String
    protected val finishSuccessDialogMsg: String
    protected val finishFailDialogMsg: String

    protected val guessingSuccessKeyboard: String
    protected val guessingFailKeyboard: String

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

    protected fun getBirdClassDistribution(bot: Bot, msg: Message): BirdClassDistributionModel {
        val photos = (msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()).map { it.toPhotoSizeModel() }
        val localFile = photoInteractor.saveBiggestPhotoAsTempFile(photos) { fileId ->
            "https://api.telegram.org/file/bot$token/${bot.getFile(fileId).get().file_path}"
        }

        return birdInteractor.calcBirdClassDistribution(localFile)
    }

    fun getBirdName(bestBird: BirdClassModel): String = birdNamesRes.getString(BirdNameEnum.fromId(bestBird.id).name)

    fun birdWithEmojiNumber(index: Int, bird: BirdClassModel): String {
        val birdMsg = MessageFormat.format(hypothesisShortMsg, getBirdName(bird), bird.rate.toPercentage())
        val indexEmoji = when (index) {
            0 -> ":one:"
            1 -> ":two:"
            2 -> ":three:"
            3 -> ":four:"
            4 -> ":five:"
            else -> ":information_source:"
        }.let { EmojiParser.parseToUnicode(it) }

        return "$indexEmoji $birdMsg;\n"
    }

    fun abortChain(bot: Bot, chatId: Long, message: String) {
        bot.sendMessage(chatId, message)
        bot.terminateChain(chatId)
    }

    private fun PhotoSize.toPhotoSizeModel(): PhotoSizeModel = PhotoSizeModel(
        fileId = file_id,
        fileSize = file_size,
        width = width,
        height = height
    )
}