package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.jumpToAndFire
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.data.enum.BirdNameEnum
import com.github.sbaldin.tbot.domain.BiggestPhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassifierInteractor
import com.github.sbaldin.tbot.hasPhoto
import com.github.sbaldin.tbot.presentation.base.BaseGuessBirdChainPresenter
import com.github.sbaldin.tbot.toPercentage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import java.util.concurrent.ConcurrentHashMap

class GuessBirdByCmdChainPresenter(
    locale: Locale,
    token: String,
    photoInteractor: BiggestPhotoInteractor,
    birdInteractor: BirdClassifierInteractor
) : BaseGuessBirdChainPresenter(locale, token, photoInteractor, birdInteractor) {

    private val startChainPredicates = listOf("/чтозаптица", "/чезаптица", "/чезапетух", "/guessBird")

    override fun chain(bot: Bot): ChainBuilder = bot.chain(label = "guess_bird_start", { msg -> msg.text in startChainPredicates }) { msg ->
        bot.sendMessage(msg.chat.id, startDialogMsg)
    }.then(label = "guess_bird_photo_guess_step") { msg ->
        if (msg.new_chat_photo == null && msg.photo == null) {
            abortChain(bot, msg.chat.id, abortDialogMsg)
            return@then
        }
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)

        val birdDistribution = getBirdClassDistribution(bot, msg)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
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
    }.then(label = "guess_bird_photo_finish_step") { msg ->
        var lastDialogMsg = ""
        when (msg.text) {
            guessingSuccessKeyboard -> {
                lastDialogMsg = finishSuccessDialogMsg
            }
            guessingFailKeyboard -> {
                val distribution = birdClassDistributionByChatId.getValue(msg.chat.id)
                val fiveBirdsMsg = birdInteractor.getBirdsWithHighestRate(distribution).mapIndexed { index, bird ->
                    birdWithEmojiNumber(index, bird)
                }.joinToString("")
                lastDialogMsg = finishFailDialogMsg + fiveBirdsMsg
            }
        }
        abortChain(bot, msg.chat.id, lastDialogMsg)
    }


}


class GuessBirdByChatPhotoChainPresenter(
    locale: Locale,
    token: String,
    photoInteractor: BiggestPhotoInteractor,
    birdInteractor: BirdClassifierInteractor
) : BaseGuessBirdChainPresenter(locale, token, photoInteractor, birdInteractor) {

    override fun chain(bot: Bot): ChainBuilder = bot.chain("new_photo_in_chat", { m -> m.hasPhoto() }) { msg ->
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)
        val birdDistribution = getBirdClassDistribution(bot, msg)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
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
    }.then(label = "guess_bird_photo_finish_step") { msg ->
        var lastDialogMsg = ""
        when (msg.text) {
            guessingSuccessKeyboard -> {
                lastDialogMsg = finishSuccessDialogMsg
            }
            guessingFailKeyboard -> {
                val distribution = birdClassDistributionByChatId.getValue(msg.chat.id)
                val fiveBirdsMsg = birdInteractor.getBirdsWithHighestRate(distribution).mapIndexed { index, bird ->
                    birdWithEmojiNumber(index, bird)
                }.joinToString("")
                lastDialogMsg = finishFailDialogMsg + fiveBirdsMsg
            }
        }
        abortChain(bot, msg.chat.id, lastDialogMsg)
    }

}