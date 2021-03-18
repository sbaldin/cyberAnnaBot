package com.github.sbaldin.tbot

import org.slf4j.LoggerFactory
import java.io.File

class BirdClassifier {
    fun getBirdClassDistribution(localFile: File): BirdClassDistribution {
        log.info("Calc bird class distribution of $localFile")

        return BirdClassDistribution(
            mapOf(
                "DOVE" to BirdClass(
                    "DOVE",
                    "ГОЛУБЬ",
                    0.78
                )
            )
        )
    }


    companion object {
        val log = LoggerFactory.getLogger(BirdClassifier::class.java)
    }
}

data class BirdClassDistribution(
    val birdById: Map<String, BirdClass>
)


data class BirdClass(
    val id: String,
    val title: String,
    val rate: Double //probability that source image belongs to this class
)
