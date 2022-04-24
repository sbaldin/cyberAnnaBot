package com.github.sbaldin.tbot.presentation.base

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.ObjectDetectionFailedModel
import com.github.sbaldin.tbot.data.ObjectDetectionResultModel
import com.github.sbaldin.tbot.data.ObjectDetectionSuccessfulModel
import com.github.sbaldin.tbot.domain.BirdClassificationInteractor
import com.github.sbaldin.tbot.domain.BirdDetectionInteractor
import com.github.sbaldin.tbot.domain.ImageCropInteractor
import com.github.sbaldin.tbot.domain.PhotoInteractor
import com.github.sbaldin.tbot.domain.asDetectedObjects
import com.github.sbaldin.tbot.toPhotoSizeModel
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

abstract class BaseBirdDetectionChainPresenter(
    locale: Locale,
    private val token: String,
    private val photoInteractor: PhotoInteractor,
    classificationInteractor: BirdClassificationInteractor,
    private val detectionInteractor: BirdDetectionInteractor,
    private val imageCropInteractor: ImageCropInteractor,
) : BaseGuessBirdChainPresenter(locale, token, photoInteractor, classificationInteractor) {

    private val detectedObjectByChatId = ConcurrentHashMap<Int, ObjectDetectionResultModel>()

    protected fun detectBirds(bot: Bot, msg: Message): ObjectDetectionResultModel {
        val photos = (msg.new_chat_photo.orEmpty() + msg.photo.orEmpty()).map { it.toPhotoSizeModel() }
        val localFile = photoInteractor.savePhotoToStorage(getUniqueId(msg.chat.id, msg.from?.id), photos) { fileId ->
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
                logger().info("Cropping step. Selected object: ${msg.text}")
                // drop sub string "Птица №"
                val selectedObj = msg.text!!.substring(7).toInt()
                detectionResult.detectedObjects[selectedObj]
            }
            is ObjectDetectionFailedModel -> {
                logger().info("Cropping step has been skipped, due to failed object detection step. Initial image will be used.")
                detectionResult.initialPhoto.asDetectedObjects()
            }
        }

        val file = imageCropInteractor.crop(msg.chat.id, msg.from?.id, detectionResult.initialPhoto, detectedObj)
        logger().info("Cropping was finished.")
        /*
          Here  we output cropped images to be sure that detection and cropping steps were correct
          Uncomment in cose you need to test cropping
          bot.sendPhoto(
                    chatId = msg.chat.id,
                    photo = file,
                    caption = "debug message",
                )
        */
        return file
    }
}
