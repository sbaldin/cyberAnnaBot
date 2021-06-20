package com.github.sbaldin.tbot.data

import com.github.sbaldin.tbot.data.enums.ObjectDetectionLabelEnum
import java.io.File

// cnn config models
class CnnInputLayerSizeModel(
    val width: Long = 224,
    val height: Long = 224,
    val channels: Long = 3
)

// bird classifier models
data class BirdClassModel(
    val id: Int,
    val title: String,
    val rate: Double // probability that input image belongs to this class
)

data class BirdClassDistributionModel(
    val birdById: Map<Int, BirdClassModel>
)

data class MessageBirdDistributionModel(
    // we need messageId to react to wrong or correct recognizing of a bird
    // e.g. we want to move photo from message to training dataset if Net produced wrong answer
    val messageId: Int,
    val birds: BirdClassDistributionModel
)

// photo interactor models
data class PhotoSizeModel(
    val fileId: String,
    val width: Int,
    val height: Int,
    val fileSize: Int
)

// object detector models
data class DetectedObjectModel(
    val topLeftX: Int = 0,
    val topLeftY: Int = 0,
    val bottomRightX: Int = 0,
    val bottomRightY: Int = 0,
    val width: Int = 0,
    val height: Int = 0
)

sealed class ObjectDetectionResultModel(
    open val label: ObjectDetectionLabelEnum,
    open val initialPhoto: File
)

class ObjectDetectionSuccessfulModel(
    override val label: ObjectDetectionLabelEnum,
    override val initialPhoto: File,
    val labeledPhoto: File,
    val detectedObjects: List<DetectedObjectModel>,
) : ObjectDetectionResultModel(label, initialPhoto)

class ObjectDetectionFailedModel(
    override val label: ObjectDetectionLabelEnum,
    override val initialPhoto: File,
    val reason: String,
) : ObjectDetectionResultModel(label, initialPhoto)