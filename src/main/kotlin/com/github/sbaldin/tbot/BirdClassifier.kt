package com.github.sbaldin.tbot

import com.github.sbaldin.tbot.domain.CnnConf
import javax.imageio.ImageReader
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.slf4j.LoggerFactory
import java.io.File
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler

import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization


import org.datavec.image.loader.NativeImageLoader
import org.datavec.image.transform.ResizeImageTransform
import java.io.InputStream
import jdk.nashorn.internal.objects.NativeRegExp.source

import javax.imageio.ImageIO

import javax.imageio.stream.ImageInputStream

import org.deeplearning4j.util.NetworkUtils.output
import org.deeplearning4j.util.NetworkUtils.output








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
        val image =  NativeImageLoader(
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
        log.info("output:"+ output.toStringFull())
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

    //helper class to return the largest value in the output array
    fun arrayMaximum(arr: DoubleArray): Double {
        var max = Double.NEGATIVE_INFINITY
        for (cur in arr) max = Math.max(max, cur)
        return max
    }

    // helper class to find the index (and therefore numerical value) of the largest confidence score
    fun getIndexOfLargestValue(array: DoubleArray?): Int {
        if (array == null || array.isEmpty()) return -1
        var largest = 0
        for (i in 1 until array.size) {
            if (array[i] > array[largest]) largest = i
        }
        return largest
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
