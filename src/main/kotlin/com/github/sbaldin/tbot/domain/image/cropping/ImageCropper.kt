package com.github.sbaldin.tbot.domain.image.cropping

import com.github.sbaldin.tbot.data.DetectedObjectModel
import com.github.sbaldin.tbot.measure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.File
import java.util.Objects
import javax.imageio.ImageIO

class ImageCropper {

    fun crop(chatId: Long, userId: Int?, cropBorder: DetectedObjectModel, imageFile: File): File {
        return log.measure("Cropping photo operation... Chat: $chatId UserId:$userId.") {
            val image = ImageIO.read(imageFile)
            val safeCropBorder = image.boundByImageSize(cropBorder)
            val cropped = image.getSubimage(
                safeCropBorder.topLeftX,
                safeCropBorder.topLeftY,
                safeCropBorder.width,
                safeCropBorder.height,
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

private fun BufferedImage.boundByImageSize(cropBorder: DetectedObjectModel): DetectedObjectModel {
    val imageBounds = this.raster.bounds
    return DetectedObjectModel(
        topLeftX = if (cropBorder.topLeftX > imageBounds.minX) cropBorder.topLeftX else imageBounds.minX.toInt(),
        topLeftY = if (cropBorder.topLeftY > imageBounds.minY) cropBorder.topLeftY else imageBounds.minY.toInt(),
        bottomRightX = if (cropBorder.bottomRightX < imageBounds.maxX) cropBorder.bottomRightX else imageBounds.maxX.toInt(),
        bottomRightY = if (cropBorder.bottomRightY < imageBounds.maxY) cropBorder.bottomRightY else imageBounds.maxY.toInt(),
    )
}
