package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BotConf
import com.github.sbaldin.tbot.data.MessageBirdDistributionModel
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassifierInteractor
import com.github.sbaldin.tbot.domain.GuessingStateHandler
import com.github.sbaldin.tbot.hasPhoto
import com.github.sbaldin.tbot.presentation.base.BaseGuessBirdChainPresenter
import com.github.sbaldin.tbot.toPercentage
import java.text.MessageFormat

class GuessBirdByCmdChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    birdInteractor: BirdClassifierInteractor,
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    override fun chainPredicateFn(msg: Message): Boolean {
        return super.chainPredicateFn(msg) && startChainPredicates.any { msg.text?.contains(it) ?: false }
    }

    override fun chain(bot: Bot): ChainBuilder = bot.chain("guess_bird_start", this::chainPredicateFn) { msg ->
        bot.sendMessage(msg.chat.id, startDialogMsg)
    }.then(label = "guess_bird_photo_guess_step") { msg ->
        if (msg.new_chat_photo == null && msg.photo == null) {
            abortChain(bot, msg.chat.id, abortDialogMsg)
            return@then
        }
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)

        val birdDistribution = getBirdClassDistribution(bot, msg)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)

        // we need messageId to react to wrong or correct recognizing of a bird
        // e.g. we want to move photo from message to training dataset if Net produced wrong answer
        birdClassDistributionByChatId[msg.chat.id] = MessageBirdDistributionModel(msg.message_id, birdDistribution)

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
            }
        )

        abortChain(bot, msg.chat.id, lastDialogMsg)
    }
}

class GuessBirdByChatPhotoChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    birdInteractor: BirdClassifierInteractor
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    override fun chain(bot: Bot): ChainBuilder =
        bot.chain("new_photo_in_chat_start", { m -> m.chat.type == "private" && m.hasPhoto() }) { msg ->
            bot.sendMessage(msg.chat.id, guessingInProgressMsg)
            val birdDistribution = getBirdClassDistribution(bot, msg)
            val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
            birdClassDistributionByChatId[msg.chat.id] = MessageBirdDistributionModel(msg.message_id, birdDistribution)

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
                }
            )

            abortChain(bot, msg.chat.id, lastDialogMsg)
        }
}

class GuessBirdByChatMentionChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    birdInteractor: BirdClassifierInteractor
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    private val botName = conf.name

    override fun chainPredicateFn(msg: Message): Boolean {
        return super.chainPredicateFn(msg)
                && msg.chat.type == "private"
                && msg.text?.contains(botName) ?: false
                && startChainPredicates.any { msg.text?.contains(it) ?: false }
    }

    override fun chain(bot: Bot): ChainBuilder = bot.chain("mention_in_chat", this::chainPredicateFn) { msg ->
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)
        val birdDistribution = getBirdClassDistribution(bot, msg)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
        birdClassDistributionByChatId[msg.chat.id] = MessageBirdDistributionModel(msg.message_id, birdDistribution)

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
            }
        )

        abortChain(bot, msg.chat.id, lastDialogMsg)
    }
}