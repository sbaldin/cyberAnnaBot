package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.*
import com.github.sbaldin.tbot.domain.BirdClassificationInteractor
import com.github.sbaldin.tbot.domain.BirdDetectionInteractor
import com.github.sbaldin.tbot.domain.ImageCropInteractor
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.hasPhoto
import com.github.sbaldin.tbot.presentation.base.BaseBirdDetectionChainPresenter
import com.github.sbaldin.tbot.presentation.base.BaseGuessBirdChainPresenter
import com.github.sbaldin.tbot.toPercentage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.MessageFormat

val log: Logger = LoggerFactory.getLogger(GuessBirdByCmdChainPresenter::class.java)

class GuessBirdByCmdChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    classificationInteractor: BirdClassificationInteractor,
    detectionInteractor: BirdDetectionInteractor,
    imageCropInteractor: ImageCropInteractor,
    ) : BaseBirdDetectionChainPresenter(
    conf.locale(),
    conf.token,
    photoInteractor,
    classificationInteractor,
    detectionInteractor,
    imageCropInteractor
) {

    override fun chainPredicateFn(msg: Message): Boolean {
        return super.chainPredicateFn(msg) && startChainPredicates.any { msg.text?.contains(it) ?: false }
    }

    override fun chain(bot: Bot): ChainBuilder = bot.chain("guess_bird_start", this::chainPredicateFn) { msg ->
        bot.sendMessage(msg.chat.id, startDialogMsg)
    }.then(label = "object_detection_step") { msg ->
        if (msg.new_chat_photo == null && msg.photo == null) {
            abortChain(bot, msg.chat.id, msg.from?.id, abortDialogMsg)
            return@then
        }
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)
        when (val objectDetectionResult = detectBirds(bot, msg)) {
            is ObjectDetectionSuccessfulModel -> {
                log.info(" ${objectDetectionResult.detectedObjects.size} Birds were detected on photo!")
                val keyboard = objectDetectionResult.detectedObjects
                    .mapIndexed { index, _ -> KeyboardButton("объект №$index") }
                bot.sendPhoto(
                    chatId = msg.chat.id,
                    photo = objectDetectionResult.labeledPhoto,
                    caption = detectedObjMsg,
                    markup = ReplyKeyboardMarkup(
                        resize_keyboard = true,
                        one_time_keyboard = true,
                        keyboard = listOf(keyboard)
                    )
                )

            }
            is ObjectDetectionFailedModel -> {
                log.info("Bird detection was failed! Reason:${objectDetectionResult.reason}.")
                bot.sendMessage(
                    chatId = msg.chat.id,
                    text = noDetectedObjMsg + " ",
                )
            }
        }
    }.then(label = "guess_bird_photo_crop_photo_step") { msg ->
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)
        val croppedImage = cropDetectedObject(msg, bot)
        val birdDistribution = getBirdClassDistribution(croppedImage)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
        val id = getUniqueId(msg.chat.id, msg.from?.id)
        log.info("Bird Classification: ${bestBird.title}")

        // we need unique id to react to wrong or correct recognizing of a bird
        // e.g. we want to move photo from message to training dataset if Net produced wrong answer
        birdClassDistributionByChatId[id] = birdDistribution

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
        log.info("Finishing step. With message: ${msg.text}")

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

        abortChain(bot, msg.chat.id, msg.from?.id, lastDialogMsg)
    }
}

class GuessBirdByChatPhotoChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    birdInteractor: BirdClassificationInteractor
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    override fun chain(bot: Bot): ChainBuilder =
        bot.chain("new_photo_in_chat_start", { m -> m.chat.type == "private" && m.hasPhoto() }) { msg ->
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

            abortChain(bot, msg.chat.id, msg.from?.id, lastDialogMsg)
        }
}

class GuessBirdByChatMentionChainPresenter(
    conf: BotConf,
    photoInteractor: PhotoInteractor,
    birdInteractor: BirdClassificationInteractor
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
        val id = getUniqueId(msg.chat.id, msg.from?.id)

        birdClassDistributionByChatId[id] = birdDistribution

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

        abortChain(bot, msg.chat.id, msg.from?.id, lastDialogMsg)
    }
}