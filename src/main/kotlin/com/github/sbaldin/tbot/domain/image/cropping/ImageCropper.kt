package com.github.sbaldin.tbot.domain.image.cropping

import com.github.sbaldin.tbot.data.DetectedObjectModel
import com.github.sbaldin.tbot.measure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Objects
import javax.imageio.ImageIO

class ImageCropper {

    fun crop(chatId: Long, userId: Int?, cropBorder: DetectedObjectModel, imageFile: File): File {
        return log.measure("Cropping photo operation... Chat: $chatId UserId:$userId.") {
            val image = ImageIO.read(imageFile)
            val cropped = image.getSubimage(
                cropBorder.topLeftX,
                cropBorder.topLeftY,
                cropBorder.width,
                cropBorder.height,
            )
            File.createTempFile("cropped", Objects.hash(chatId, userId).toString()).also { file ->
                ImageIO.write(cropped, "png", file)
            }
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(ImageCropper::class.java)
    }
}
