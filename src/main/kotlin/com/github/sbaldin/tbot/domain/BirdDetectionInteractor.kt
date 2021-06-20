package com.github.sbaldin.tbot.domain

import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.data.DetectedObjectModel
import com.github.sbaldin.tbot.data.ObjectDetectionResultModel
import com.github.sbaldin.tbot.data.enums.ObjectDetectionLabelEnum
import com.github.sbaldin.tbot.domain.detection.ObjectDetector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.imageio.ImageIO

class BirdDetectionInteractor(
    private val objectDetector: ObjectDetector,
) {
    fun detect(savedPhoto: File): ObjectDetectionResultModel =
        objectDetector.detect(ObjectDetectionLabelEnum.BIRD, savedPhoto)

}

fun File.asDetectedObjects(): DetectedObjectModel {
    val img = ImageIO.read(this)
    return DetectedObjectModel(
        topLeftX = 0,
        topLeftY = 0,
        bottomRightX = img.width - 1,
        bottomRightY = img.height - 1,
        width = img.width,
        height = img.height,
    )
}
