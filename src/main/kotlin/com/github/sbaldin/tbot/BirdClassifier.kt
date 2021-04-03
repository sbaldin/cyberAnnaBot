package com.github.sbaldin.tbot

import com.github.sbaldin.tbot.domain.BirdNameEnum
import com.github.sbaldin.tbot.domain.CnnConf
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.util.ModelSerializer
import org.slf4j.LoggerFactory
import java.io.File
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler


import org.datavec.image.loader.NativeImageLoader
import org.datavec.image.transform.ResizeImageTransform
import java.io.InputStream

import javax.imageio.ImageIO


class BirdClassifier(conf: CnnConf) {

    val model: MultiLayerNetwork = ModelSerializer.restoreMultiLayerNetwork(loadModel(conf));

    private fun loadModel(conf: CnnConf): InputStream {
        return Thread.currentThread().contextClassLoader.getResourceAsStream(conf.model)!!
    }

    fun getBirdClassDistribution(localFile: File): BirdClassDistribution {
        log.info("Calc bird class distribution of $localFile")
        //Use the nativeImageLoader to convert to numerical matrix
        val loader = createImageLoader(localFile)
        val resizer = ResizeImageTransform(224, 224)
        val resized = resizer.transform(loader.asWritable(localFile))

        //put image into INDArray
        val image = NativeImageLoader(
            224,
            224,
            3
        ).asMatrix(resized)

        //values need to be scaled
        val scalar: ImagePreProcessingScaler = ImagePreProcessingScaler(0.0, 1.0)
        //then call that scalar on the image dataset
        scalar.transform(image)

        //pass through neural net and store it in output array
        val output = model.output(image)

        val outputDistribution = output.toDoubleVector().mapIndexed { index, rate ->
            val birdName = BirdNameEnum.fromId(index)
            BirdClass(
                birdName.id,
                birdName.title,
                rate
            )
        }.sortedByDescending { it.rate }

        log.info("output:\n" + outputDistribution.joinToString())

        return BirdClassDistribution(
            outputDistribution.take(3).associateByTo(LinkedHashMap()) { it.id }
        )
    }

    private fun createImageLoader(localFile: File): NativeImageLoader = ImageIO.createImageInputStream(localFile).use { iis ->
        val readers = ImageIO.getImageReaders(iis)
        readers.takeIf { it.hasNext() }?.let {
            val reader = it.next()
            try {
                reader.setInput(iis, true);
                NativeImageLoader(
                    reader.getWidth(0).toLong(),
                    reader.getHeight(0).toLong(),
                    3
                )
            } catch (e: Exception) {
                throw e;
            } finally {
                reader.dispose();
            }
        }!!
    }

    companion object {
        val log = LoggerFactory.getLogger(BirdClassifier::class.java)
    }
}

data class BirdClassDistribution(
    val birdById: Map<Int, BirdClass>
)


data class BirdClass(
    val id: Int,
    val title: String,
    val rate: Double //probability that source image belongs to this class
)
