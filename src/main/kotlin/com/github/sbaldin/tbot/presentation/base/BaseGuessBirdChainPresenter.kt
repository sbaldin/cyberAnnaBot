package com.github.sbaldin.tbot.presentation.base

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.data.enums.BirdNameEnum
import com.github.sbaldin.tbot.domain.BirdClassificationInteractor
import com.github.sbaldin.tbot.domain.GuessingStateHandler
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.presentation.base.message.isSentInLast5minutes
import com.github.sbaldin.tbot.toPercentage
import com.github.sbaldin.tbot.toPhotoSizeModel
import com.vdurmont.emoji.EmojiParser
import java.text.MessageFormat
import java.util.Locale
import java.util.Objects
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

abstract class BaseGuessBirdChainPresenter(
    locale: Locale,
    private val token: String,
    private val photoInteractor: PhotoInteractor,
    protected val birdInteractor: BirdClassificationInteractor,
) : DialogChain {

    protected val startChainPredicates = listOf("/guessBird", "/findBird", "/whatsBird", "/bird")

    // TODO: I am not sure that kt-telegram-bot-1.3.8.jar provides thread-safe api when you work with chains, investigate how to do it correctly
    protected val birdClassDistributionByChatId = ConcurrentHashMap<Int, BirdClassDistributionModel>()
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

    protected val catchExceptionMessage: String

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
            catchExceptionMessage = getStringWithEmoji("find_bird_dialog_catch_exception_message")
        }
    }

    /**
     * Determines whether to run the chain or not
     */
    override fun chainPredicate(msg: Message): Boolean = msg.isSentInLast5minutes()

    protected open fun clearState(chatId: Long, userId: Int?) {
        birdClassDistributionByChatId.remove(getUniqueId(chatId, userId))
    }

    protected fun getUniqueId(chatId: Long, userId: Int?) = Objects.hash(chatId, userId).unaryPlus()

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

    fun handleGuessingResults(
        msg: Message,
        onSuccess: (BirdClassDistributionModel) -> Unit,
        onFail: (BirdClassDistributionModel) -> Unit,
    ) {
        val id = getUniqueId(msg.chat.id, msg.from?.id)
        val birdDistribution = birdClassDistributionByChatId.getValue(id)
        when (msg.text) {
            guessingSuccessKeyboard -> {
                onSuccess(birdDistribution)
                stateHandler.onSuccessGuessing(id)
            }
            guessingFailKeyboard -> {
                onFail(birdDistribution)
                stateHandler.onFailedGuessing(id)
            }
        }
    }

    /**
     * Adds the next step for the chain. Steps are executed in the
     * order you add them.
     * Fallback allow you to send the predicate were used
     * to initiate chain, predicate will terminate current chain state and launched it again(currently supports by cmd like /bird)
     * @param label a label of the step. If null then label is
     *              generated by builder. Default is `null`
     * @param isTerminal if true then chain terminates on the current step.
     *                   Default is `false`
     * @param action an action for the current step
     * @return [ChainBuilder]
     */
    public fun ChainBuilder.safeThen(
        label: String,
        isTerminal: Boolean = false,
        bot: Bot,
        action: (Message) -> Unit,
    ): ChainBuilder = apply {
        val fallbackWrapper = { msg: Message ->
            try {
                action(msg)
            } catch (e: Exception) {
                logger().error("Error during chain processing", e)
                abortChain(bot, msg.chat.id, msg.from?.id, catchExceptionMessage)
            }
        }
        this.then(label, isTerminal, fallbackWrapper)
    }

    fun abortChain(bot: Bot, chatId: Long, userId: Int?, message: String) {
        bot.sendMessage(
            chatId,
            message,
            markup = ReplyKeyboardMarkup(
                resize_keyboard = true,
                one_time_keyboard = true,
                keyboard = listOf(
                    listOf(),
                ),
            ),
        )
        clearState(chatId, userId)
        bot.terminateChain(chatId)
    }
}
