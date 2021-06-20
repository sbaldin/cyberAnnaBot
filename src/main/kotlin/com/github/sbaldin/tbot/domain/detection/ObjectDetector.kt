package com.github.sbaldin.tbot.domain.detection

import com.github.sbaldin.tbot.data.DetectedObjectModel
import com.github.sbaldin.tbot.data.ObjectDetectionFailedModel
import com.github.sbaldin.tbot.data.ObjectDetectionResultModel
import com.github.sbaldin.tbot.data.ObjectDetectionSuccessfulModel
import com.github.sbaldin.tbot.data.enums.ObjectDetectionLabelEnum
import com.github.sbaldin.tbot.measure
import org.datavec.image.loader.NativeImageLoader
import org.deeplearning4j.nn.graph.ComputationGraph
import org.deeplearning4j.nn.layers.objdetect.DetectedObject
import org.deeplearning4j.nn.layers.objdetect.Yolo2OutputLayer
import org.deeplearning4j.zoo.model.TinyYOLO
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler
import org.slf4j.LoggerFactory
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO


class ObjectDetector(
    private val model: ComputationGraph = TinyYOLO.builder().build().initPretrained() as ComputationGraph,
    private val loader: NativeImageLoader = NativeImageLoader(416, 416, 3),
    private val frameScaleFactor: Double = 0.09,
) {
    private val labels = arrayOf(
        "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair", "cow",
        "diningtable", "dog", "horse", "motorbike", "person", "pottedplant", "sheep", "sofa", "train", "tvmonitor"
    )
    private val detectionThreshold = 0.4
    private val gridWidth = 13
    private val gridHeight = 13

/*    private fun detectWithMaxConfidence(birdPhoto: File):ObjectDetectionResultModel = log.measure("Detect object with max confedence") {
        val detectedObjects = applyModel(birdPhoto)
        val detectedObjectWithMaxConfidence = detectedObjects.maxByOrNull { it.confidence }
        if (detectedObjectWithMaxConfidence != null) {
            val labelId = detectedObjectWithMaxConfidence.classPredictions.getInt(0, 0)
            ObjectDetectionSuccessfulModel(
                label = ObjectDetectionLabelEnum.values()[labelId],
                labeledPhoto = drawDetectedObjects(birdPhoto, detectedObjects),
                detectedObjects = listOf(detectedObjectWithMaxConfidence).map {
                    DetectedObjectModel(
                        it.centerX,
                        it.centerY,
                        it.width,
                        it.height,
                    )
                }
            )
        } else {
            ObjectDetectionFailedModel(
                label = requestedLabel,
                reason = "Couldn't find the requested label on photo."
            )
        }
    }*/


    fun detect(requestedLabel: ObjectDetectionLabelEnum, birdPhoto: File): ObjectDetectionResultModel =
        log.measure("Detect object with label:$requestedLabel") {
            val detectedObjects = applyModel(birdPhoto)
            if (detectedObjects.any { it.predictedClass == requestedLabel.ordinal }) {
                val requestedObjects = detectedObjects.filter { it.predictedClass == requestedLabel.ordinal }
                val img = ImageIO.read(birdPhoto)
                ObjectDetectionSuccessfulModel(
                    label = requestedLabel,
                    initialPhoto = birdPhoto,
                    labeledPhoto = drawDetectedObjects(birdPhoto, requestedObjects),
                    detectedObjects = requestedObjects.map {
                        val (topLeftX, topLeftY, bottomRightX, bottomRightY) = scaleCoordinates(it, img)
                        DetectedObjectModel(
                            topLeftX,
                            topLeftY,
                            bottomRightX,
                            bottomRightY,
                            bottomRightX - topLeftX,
                            bottomRightY - topLeftY,
                        )
                    }
                )
            } else {
                ObjectDetectionFailedModel(
                    label = requestedLabel,
                    initialPhoto = birdPhoto,
                    reason = "Couldn't find the requested label on photo."
                )
            }
        }


    private fun applyModel(birdPhoto: File): List<DetectedObject> {
        val imagePreProcessingScaler = ImagePreProcessingScaler(0.0, 1.0)
        val outputLayer = model.getOutputLayer(0) as Yolo2OutputLayer
        val indArray = loader.asMatrix(birdPhoto)
        imagePreProcessingScaler.transform(indArray)
        val results = model.outputSingle(indArray)
        return outputLayer.getPredictedObjects(results, detectionThreshold)
    }

    private fun drawDetectedObjects(photo: File, detectedObjects: List<DetectedObject>): File {
        val img = ImageIO.read(photo)
        val g2d = img.createGraphics()
        g2d.color = Color.RED
        g2d.stroke = BasicStroke(8f)
        g2d.font = Font("TimesRoman", Font.PLAIN, 64)
        val textMargin = 12
        detectedObjects.forEachIndexed { index, detectedObject ->
            val (xs1, ys1, xs2, ys2) = scaleCoordinates(detectedObject, img)
            g2d.drawString("$index" + labels[detectedObject.predictedClass], xs1 + textMargin, ys2 - textMargin)
            g2d.drawRect(xs1, ys1, xs2 - xs1, ys2 - ys1)
        }
        g2d.dispose()
        val file = Path.of(photo.parent, "edited_" + photo.name).toFile()
        ImageIO.write(img, "png", file)
        return file
    }

    //TODO make it more safe and less ugly
    private fun scaleCoordinates(
        obj: DetectedObject,
        img: BufferedImage,
    ): List<Int> {
        val x1 = obj.topLeftXY[0]
        val y1 = obj.topLeftXY[1]
        val x2 = obj.bottomRightXY[0]
        val y2 = obj.bottomRightXY[1]
        val xs1 = (transformWidth(x1, img) * (1.0 - frameScaleFactor)).toInt()
        val ys1 = (transformHeight(y1, img) * (1.0 - frameScaleFactor)).toInt()
        val xs2 = (transformWidth(x2, img) * (1.0 + frameScaleFactor)).toInt()
        val ys2 = (transformHeight(y2, img) * (1.0 + frameScaleFactor)).toInt()
        return listOf(xs1, ys1, xs2, ys2)
    }

    private fun transformWidth(x: Double, img: BufferedImage) =
        x / gridWidth * img.width.toDouble()

    private fun transformHeight(y: Double, img: BufferedImage) =
        y / gridHeight * img.height.toDouble()

    companion object {
        val log = LoggerFactory.getLogger(ObjectDetector::class.java)
    }
}
