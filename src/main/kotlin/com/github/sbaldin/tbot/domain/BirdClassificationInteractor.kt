package com.github.sbaldin.tbot.domain

import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.domain.classification.BirdClassifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class BirdClassificationInteractor(
    private val birdClassifier: BirdClassifier
) {

    fun calcBirdClassDistribution(savedPhoto: File) = birdClassifier.getBirdClassDistribution(savedPhoto)

    fun getBirdWithHighestRate(birdDistribution: BirdClassDistributionModel): BirdClassModel {
        return birdDistribution.birdById.values.maxByOrNull { it.rate }!!
    }

    fun getBirdsWithHighestRate(birdDistribution: BirdClassDistributionModel, limit: Int = 5): List<BirdClassModel> {
        return birdDistribution.birdById.values.asSequence().sortedByDescending { it.rate }.take(limit).toList()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(BirdClassificationInteractor::class.java)
    }
}