package com.github.sbaldin.tbot.domain.classification

import com.github.sbaldin.tbot.data.BirdClassDistributionModel
import com.github.sbaldin.tbot.data.BirdClassModel
import com.github.sbaldin.tbot.data.CnnConf
import com.github.sbaldin.tbot.data.enums.BirdNameEnum
import com.github.sbaldin.tbot.domain.image.cropping.ImageLoaderProvider
import org.deeplearning4j.util.ModelSerializer
import org.slf4j.LoggerFactory
import java.io.File
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler
import org.datavec.image.loader.NativeImageLoader
import org.datavec.image.transform.ResizeImageTransform
import java.io.InputStream
import javax.imageio.ImageIO

class BirdClassifier(conf: CnnConf) {

    private val model = ModelSerializer.restoreComputationGraph(loadModel(conf))
    private val loader = ImageLoaderProvider()


    private fun loadModel(conf: CnnConf): InputStream {
        return Thread.currentThread().contextClassLoader.getResourceAsStream(conf.modelFileName)!!
    }

    fun getBirdClassDistribution(localFile: File): BirdClassDistributionModel {
        log.info("Calc bird class distribution of $localFile")
        // use the nativeImageLoader to convert to numerical matrix
        val loader = loader.get(localFile)
        val resizer = ResizeImageTransform(224, 224)
        val resized = resizer.transform(loader.asWritable(localFile))

        // put image into INDArray
        val image = NativeImageLoader(
            224,
            224,
            3
        ).asMatrix(resized)

        // values need to be scaled
        val scalar = ImagePreProcessingScaler(0.0, 1.0)
        // then call that scalar on the image dataset
        scalar.transform(image)

        // pass through neural net and store it in output array
        val output = model.output(image)

        val outputDistribution = output[0].toDoubleVector().mapIndexed { index, rate ->
            val birdName = BirdNameEnum.fromId(index)
            BirdClassModel(
                birdName.id,
                birdName.title,
                rate
            )
        }.sortedByDescending { it.rate }

        log.info("output:\n" + outputDistribution.first())

        return BirdClassDistributionModel(
            outputDistribution.take(10).associateByTo(LinkedHashMap()) { it.id }
        )
    }

    companion object {
        val log = LoggerFactory.getLogger(BirdClassifier::class.java)
    }
}