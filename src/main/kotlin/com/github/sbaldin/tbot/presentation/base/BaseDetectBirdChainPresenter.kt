package com.github.sbaldin.tbot.presentation.base

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import com.github.sbaldin.tbot.data.*
import com.github.sbaldin.tbot.domain.*
import com.github.sbaldin.tbot.presentation.log
import com.github.sbaldin.tbot.toPhotoSizeModel
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class BaseBirdDetectionChainPresenter(
    locale: Locale,
    private val token: String,
    private val photoInteractor: PhotoInteractor,
    private val classificationInteractor: BirdClassificationInteractor,
    private val detectionInteractor: BirdDetectionInteractor,
    private val imageCropInteractor: ImageCropInteractor,
) : BaseGuessBirdChainPresenter(locale, token, photoInteractor, classificationInteractor) {
    // TODO: don't use chat id as key for holding long-term data,
    // TODO: because chat id is not unique for case when two users uses bot simultaneously
    private val detectedObjectByChatId = ConcurrentHashMap<Int, ObjectDetectionResultModel>()

    protected fun detectBirds(bot: Bot, msg: Message): ObjectDetectionResultModel {
        val photos = (msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()).map { it.toPhotoSizeModel() }
        val localFile = photoInteractor.savePhotoToStorage(msg.message_id, photos) { fileId ->
            "https://api.telegram.org/file/bot$token/${bot.getFile(fileId).get().file_path}"
        }

        return detectionInteractor.detect(localFile).also {
            detectedObjectByChatId[getUniqueId(msg.chat.id, msg.from?.id)] = it
        }
    }

    protected fun getDetectionResult(chatId: Long, userId: Int?): ObjectDetectionResultModel {
        return detectedObjectByChatId.getValue(getUniqueId(chatId, userId))
    }

    protected open fun getBirdClassDistribution(file: File): BirdClassDistributionModel {
        return birdInteractor.calcBirdClassDistribution(file)
    }

    override fun clearState(chatId: Long, userId: Int?) {
        super.clearState(chatId, userId)
        detectedObjectByChatId.remove(getUniqueId(chatId, userId))
    }

    fun cropDetectedObject(
        msg: Message,
        bot: Bot,
    ): File {
        val detectionResult = getDetectionResult(msg.chat.id, msg.from?.id)
        val detectedObj = when (detectionResult) {
            is ObjectDetectionSuccessfulModel -> {
                log.info("Cropping step. Selected object: ${msg.text}")
                val selectedObj = msg.text!!.substring(8).toInt()
                detectionResult.detectedObjects[selectedObj]
            }
            is ObjectDetectionFailedModel -> {
                detectionResult.initialPhoto.asDetectedObjects()
            }
        }

        val file = imageCropInteractor.crop(msg.chat.id, msg.from?.id, detectionResult.initialPhoto, detectedObj)
        log.info("Cropping was finished.")
        bot.sendPhoto(
            chatId = msg.chat.id,
            photo = file,
            caption = "debug message"
        )
        return file
    }
}