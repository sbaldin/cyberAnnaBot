package com.github.sbaldin.tbot.domain.detection

import com.github.sbaldin.tbot.data.DetectedObjectModel
import com.github.sbaldin.tbot.data.ObjectDetectionFailedModel
import com.github.sbaldin.tbot.data.ObjectDetectionResultModel
import com.github.sbaldin.tbot.data.ObjectDetectionSuccessfulModel
import com.github.sbaldin.tbot.data.enums.ObjectDetectionLabelEnum
import com.github.sbaldin.tbot.measure
import com.google.inject.Inject
import org.datavec.image.loader.NativeImageLoader
import org.deeplearning4j.nn.graph.ComputationGraph
import org.deeplearning4j.nn.layers.objdetect.DetectedObject
import org.deeplearning4j.nn.layers.objdetect.Yolo2OutputLayer
import org.deeplearning4j.zoo.model.TinyYOLO
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

class ObjectDetector constructor(
    private val model: ComputationGraph,
    private val loader: NativeImageLoader,
    private val frameScaleFactor: Double,
) {

    @Inject
    constructor() : this(
      model =   TinyYOLO.builder().build().initPretrained() as ComputationGraph,
      loader =   NativeImageLoader(416, 416, 3),
      frameScaleFactor =   0.03,
    )

    private val detectionThreshold = 0.35
    private val gridWidth = 13
    private val gridHeight = 13

    fun detect(requestedLabel: ObjectDetectionLabelEnum, birdPhoto: File): ObjectDetectionResultModel =
        log.measure("Detect object with label:$requestedLabel") {
            val detectedObjects = applyModel(birdPhoto)
            if (detectedObjects.any { it.predictedClass == requestedLabel.ordinal }) {
                val requestedObjects = detectedObjects.filter { it.predictedClass == requestedLabel.ordinal }
                val img = ImageIO.read(birdPhoto)
                val mergedOverlappedObjects = requestedObjects.map {
                    val (topLeftX, topLeftY, bottomRightX, bottomRightY) = scaleCoordinates(it, img)
                    DetectedObjectModel(
                        topLeftX,
                        topLeftY,
                        bottomRightX,
                        bottomRightY,
                        bottomRightX - topLeftX,
                        bottomRightY - topLeftY,
                    )
                }.mergeOverlapping()
                ObjectDetectionSuccessfulModel(
                    label = requestedLabel,
                    initialPhoto = birdPhoto,
                    labeledPhoto = drawDetectedObjects(birdPhoto, mergedOverlappedObjects),
                    detectedObjects = mergedOverlappedObjects,
                )
            } else {
                ObjectDetectionFailedModel(
                    label = requestedLabel,
                    initialPhoto = birdPhoto,
                    reason = "Couldn't find the requested label on photo.",
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

    private fun drawDetectedObjects(photo: File, detectedObjects: List<DetectedObjectModel>): File {
        val img = ImageIO.read(photo)
        val g2d = img.createGraphics()
        g2d.color = Color.RED
        g2d.stroke = BasicStroke(8f)
        g2d.font = Font("TimesRoman", Font.PLAIN, 64)
        val textMargin = 12
        detectedObjects.forEachIndexed { index, detectedObject ->
            val (xs1, ys1, xs2, ys2) = detectedObject.coords()
            g2d.drawString("$index", xs1 + textMargin, ys2 - textMargin)
            g2d.drawRect(xs1, ys1, xs2 - xs1, ys2 - ys1)
        }
        g2d.dispose()
        val file = Path.of(photo.parent, "edited_" + photo.name).toFile()
        ImageIO.write(img, "png", file)
        return file
    }

    // TODO make it more safe and less ugly
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
        val log: Logger = LoggerFactory.getLogger(ObjectDetector::class.java)
    }
}

/**
 https://math.stackexchange.com/questions/99565/simplest-way-to-calculate-the-intersect-area-of-two-rectangles
 Algorithm organized in following steps:
 1. We assume that no intersection therefore create groups 'GR' one by one
 2. pick the first group and Iterates through all other groups
 3. if there are intersection than add both object to group and mark items as processed
 4. When iteration is finished merge all groups

 Note: On the one hand it's wise to merge overlapping areas twice:
 At the first stage we merge small areas that have direct overlapping,
 afterwards we receive larger areas that can have overlapping with each other.
 At the second stage we merge large areas if they have overlapping.

 However, it leads to cases when areas after 1 stage shows better results than after  1 and 2 together.
 I have tested such approach on the same photos and results are following:
 area after 1 stage shows ~90%
 area after 1 and 2 stage shows ~45-50%

 Conclusion: do not merge overlapping ares twice, better drop second overlapping areas.

 */
fun List<DetectedObjectModel>.mergeOverlapping(): List<DetectedObjectModel> {
    if (this.size <= 1) return this

    val groupIndexSeq = generateSequence(0) { it + 1 }.iterator()
    val intersectionGroups = this.associateBy { groupIndexSeq.next() }

    val mergeGroups = mutableListOf<MutableSet<DetectedObjectModel>>()
    val processedGroupIndex = mutableSetOf<Int>()

    // Grouping
    intersectionGroups.forEach outer@{ (index, areaToMerge) ->
        if (processedGroupIndex.contains(index)) return@outer
        val areasToMerge = mutableSetOf(areaToMerge)

        intersectionGroups.forEach inner@{ (anotherIndex, anotherArea) ->
            if (processedGroupIndex.contains(anotherIndex)) return@inner
            if (anotherArea != areaToMerge && checkOverlapping(areaToMerge, anotherArea)) {
                areasToMerge.add(anotherArea)
                processedGroupIndex.add(anotherIndex)
            }
        }
        processedGroupIndex.add(index)
        mergeGroups.add(areasToMerge)
    }

    // Merging
    // Do we need to make it by one loop through all elements?
    return mergeGroups.map { overlappedRectangles ->
        val topLeftX = overlappedRectangles.minOf { it.topLeftX }
        val topLeftY = overlappedRectangles.minOf { it.topLeftY }
        val bottomRightX = overlappedRectangles.maxOf { it.bottomRightX }
        val bottomRightY = overlappedRectangles.maxOf { it.bottomRightY }
        DetectedObjectModel(
            topLeftX,
            topLeftY,
            bottomRightX,
            bottomRightY,
            bottomRightX - topLeftX,
            bottomRightY - topLeftY,
        )
    }
}

/**
Here we are considering rectangle as an object of two-point: top left and bottom right, that are connected by lines:
To reduce calculation we calc width and height of interception area and then calc area of it

width = x12<x21 || x11>x22 ? 0 : Math.min(x12,x22) - Math.max(x11,x21),
height = y12<y21 || y11>y22 ? 0 : Math.min(y12,y22) - Math.max(y11,y21);
area = width * height
 */
fun checkOverlapping(rectToMerge: DetectedObjectModel, anotherRect: DetectedObjectModel): Boolean {
    val width = max(0, min(rectToMerge.bottomRightX, anotherRect.bottomRightX) - max(rectToMerge.topLeftX, anotherRect.topLeftX))
    val height = max(0, min(rectToMerge.bottomRightY, anotherRect.bottomRightY) - max(rectToMerge.topLeftY, anotherRect.topLeftY))
    val overlapArea = width * height
    // if interception area bigger than 70 percent of both rectangles area than group such objects.
    return overlapArea > rectToMerge.area * 0.7 && overlapArea > anotherRect.area * 0.7
}
