package com.github.sbaldin.tbot.domain


//cnn config related models
class CnnInputLayerSizeModel(
    val width: Long = 224,
    val height: Long = 224,
    val channels: Long = 3
)

//bird classifier models
data class BirdClassModel(
    val id: Int,
    val title: String,
    val rate: Double //probability that input image belongs to this class
)

data class BirdClassDistributionModel(
    val birdById: Map<Int, BirdClassModel>
)
