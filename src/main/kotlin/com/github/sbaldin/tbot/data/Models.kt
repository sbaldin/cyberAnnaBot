package com.github.sbaldin.tbot.data

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