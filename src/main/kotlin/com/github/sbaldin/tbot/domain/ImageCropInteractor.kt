package com.github.sbaldin.tbot.domain

import com.github.sbaldin.tbot.data.DetectedObjectModel
import com.github.sbaldin.tbot.domain.image.cropping.ImageCropper
import com.google.inject.Inject
import java.io.File
import javax.imageio.ImageIO

class ImageCropInteractor @Inject constructor(
    private val imageCropper: ImageCropper,
) {
    fun crop(chatId: Long, userId: Int?, image: File, detectedObject: DetectedObjectModel): File {
        val img = ImageIO.read(image)
        return if (detectedObject.height < img.height || detectedObject.width < img.width) {
            imageCropper.crop(chatId, userId, detectedObject, image)
        } else {
            image
        }
    }
}
