package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BotConf
import com.github.sbaldin.tbot.domain.BirdClassificationInteractor
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.presentation.base.BaseGuessBirdChainPresenter
import com.github.sbaldin.tbot.toPercentage
import java.text.MessageFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GuessBirdByChatMentionChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    birdInteractor: BirdClassificationInteractor,
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    private val botName = conf.name

    override fun chainPredicate(msg: Message): Boolean {
        return super.chainPredicate(msg) &&
            msg.chat.type == "private" &&
            msg.text?.contains(botName) ?: false &&
            startChainPredicates.any { msg.text?.contains(it) ?: false }
    }

    override fun chain(bot: Bot): ChainBuilder = bot.safeChain("mention_in_chat", ::chainPredicate) { msg ->
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
    }.then(label = "guess_bird_photo_finish_step") { msg ->
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

    companion object {
        val log: Logger = LoggerFactory.getLogger(GuessBirdByChatMentionChainPresenter::class.java)
    }
}
