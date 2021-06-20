package com.github.sbaldin.tbot.presentation.base

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.data.MessageBirdDistributionModel
import com.github.sbaldin.tbot.data.enums.BirdNameEnum
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassificationInteractor
import com.github.sbaldin.tbot.domain.GuessingStateHandler
import com.github.sbaldin.tbot.toPercentage
import com.github.sbaldin.tbot.toPhotoSizeModel
import com.vdurmont.emoji.EmojiParser
import java.text.MessageFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class BaseGuessBirdChainPresenter(
    locale: Locale,
    private val token: String,
    private val photoInteractor: PhotoInteractor,
    protected val birdInteractor: BirdClassificationInteractor
) : DialogChain {

    protected val startChainPredicates = listOf("/guessBird", "/findBird", "/whatsBird", "/bird")

    // TODO: I am not sure that kt-telegram-bot-1.3.8.jar provides thread-safe api when you work with chains, investigate how to do it correctly
    protected val birdClassDistributionByChatId = ConcurrentHashMap<Int, MessageBirdDistributionModel>()
    protected val birdNamesRes: ResourceBundle = ResourceBundle.getBundle("bird_name", locale)

    protected val stateHandler = GuessingStateHandler(photoInteractor)

    protected val startDialogMsg: String
    protected val abortDialogMsg: String
    protected val guessingInProgressMsg: String

    protected val detectedObjMsg: String
    protected val noDetectedObjMsg: String

    protected val detectionSuccessfulKeyboard: String
    protected val detectionFailKeyboard: String

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
            detectedObjMsg = getStringWithEmoji("find_bird_dialog_detected_object_message")
            noDetectedObjMsg = getStringWithEmoji("find_bird_dialog_no_detected_object_message")
            hypothesisShortMsg = getStringWithEmoji("find_bird_dialog_hypothesis_short_message")
            finishSuccessDialogMsg = getStringWithEmoji("find_bird_dialog_success_finish_keyboard")
            finishFailDialogMsg = getStringWithEmoji("find_bird_dialog_fail_finish_keyboard")

            detectionSuccessfulKeyboard = getStringWithEmoji("find_bird_dialog_obj_detection_successful_message")
            detectionFailKeyboard = getStringWithEmoji("find_bird_dialog_obj_detection_failed_message")

            guessingSuccessKeyboard = getStringWithEmoji("find_bird_dialog_success_message")
            guessingFailKeyboard = getStringWithEmoji("find_bird_dialog_fail_message")
        }
    }
    protected open fun chainPredicateFn(msg: Message): Boolean = isMessageWasSendInLast5minutes(msg)

    protected open fun clearState(chatId: Long, userId: Int?) {
        birdClassDistributionByChatId.remove(getUniqueId(chatId, userId))
    }

    protected fun getUniqueId(chatId: Long, userId: Int?) = Objects.hash(chatId, userId)

    protected open fun getBirdClassDistribution(bot: Bot, msg: Message): BirdClassDistributionModel {
        val photos = (msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()).map { it.toPhotoSizeModel() }
        val localFile = photoInteractor.savePhotoToStorage(msg.message_id, photos) { fileId ->
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

    fun handleGuessingResults(msg: Message, onSuccess: (BirdClassDistributionModel) -> Unit, onFail: (BirdClassDistributionModel) -> Unit) {
        val (msgId, birdDistribution) = birdClassDistributionByChatId.getValue(getUniqueId(msg.chat.id, msg.from?.id))
        when (msg.text) {
            guessingSuccessKeyboard -> {
                onSuccess(birdDistribution)
                stateHandler.onSuccessGuessing(msgId)
            }
            guessingFailKeyboard -> {
                onFail(birdDistribution)
                stateHandler.onFailedGuessing(msgId)
            }
        }
    }

    fun abortChain(bot: Bot, chatId: Long, userId: Int?, message: String) {
        bot.sendMessage(
            chatId, message, markup = ReplyKeyboardMarkup(
                resize_keyboard = true,
                one_time_keyboard = true,
                keyboard = listOf(
                    listOf()
                )
            )
        )
        clearState(chatId, userId)
        bot.terminateChain(chatId)
    }

}

private fun isMessageWasSendInLast5minutes(msg: Message): Boolean {
    val now = Instant.now()
    val msgDate = Instant.ofEpochMilli(msg.date * 1000L)

    val diff = Duration.between(msgDate, now).toSeconds()
    val fiveMinuteInterval = 300

    return diff < fiveMinuteInterval
}