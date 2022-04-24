package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BotConf
import com.github.sbaldin.tbot.domain.BirdClassificationInteractor
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.hasPhoto
import com.github.sbaldin.tbot.presentation.base.BaseGuessBirdChainPresenter
import com.github.sbaldin.tbot.toPercentage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.MessageFormat

class GuessBirdByChatPhotoChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    birdInteractor: BirdClassificationInteractor,
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    override fun chain(bot: Bot): ChainBuilder =
        bot.safeChain("new_photo_in_chat_start", { m -> m.chat.type == "private" && m.hasPhoto() }) { msg ->
            bot.sendMessage(msg.chat.id, guessingInProgressMsg)
            val birdDistribution = getBirdClassDistribution(bot, msg)
            val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)

            val id = getUniqueId(msg.chat.id, msg.from?.id)
            birdClassDistributionByChatId[id] = birdDistribution

            bot.sendMessage(
                chatId = msg.chat.id,
                text = MessageFormat.format(
                    hypothesisMsg,
                    getBirdName(bestBird),
                    bestBird.rate.toPercentage(),
                ),
                markup = ReplyKeyboardMarkup(
                    resize_keyboard = true,
                    one_time_keyboard = true,
                    keyboard = listOf(
                        listOf(
                            KeyboardButton(guessingSuccessKeyboard),
                            KeyboardButton(guessingFailKeyboard),
                        ),
                    ),
                ),
            )
        }.safeThen(label = "guess_bird_photo_finish_step", bot = bot) { msg ->
            var lastDialogMsg = ""
            handleGuessingResults(
                msg = msg,
                onSuccess = {
                    lastDialogMsg = finishSuccessDialogMsg
                },
                onFail = { birds ->
                    val fiveBirdsMsg = birdInteractor.getBirdsWithHighestRate(birds).mapIndexed { index, bird ->
                        birdWithEmojiNumber(index, bird)
                    }.joinToString("")
                    lastDialogMsg = finishFailDialogMsg + fiveBirdsMsg
                },
            )

            abortChain(bot, msg.chat.id, msg.from?.id, lastDialogMsg)
        }

    override fun logger(): Logger = log

    override fun getInitialChainLabel(): String = "new_photo_in_chat_start"

    companion object {
        val log: Logger = LoggerFactory.getLogger(GuessBirdByChatMentionChainPresenter::class.java)
    }
}
