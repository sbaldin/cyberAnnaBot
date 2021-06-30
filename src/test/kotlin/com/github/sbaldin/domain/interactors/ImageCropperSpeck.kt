package com.github.sbaldin.domain.interactors

import com.github.sbaldin.tbot.data.DetectedObjectModel
import com.github.sbaldin.tbot.domain.image.cropping.ImageCropper
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.DirectColorModel
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class ImageCropperSpeck : Spek({
    given("Image Cropper") {
        val cropper = ImageCropper()
        val objectToCropping = DetectedObjectModel(100, 200, 200, 300, 100, 158)
        val imageToCropping = createImageWithCroppingArea(
            objectToCropping.topLeftX,
            objectToCropping.topLeftY,
            objectToCropping.width,
            objectToCropping.height,
        )

        on("cropping 100x200 image") {
            val croppedImage = cropper.crop(1, 1, objectToCropping, imageToCropping)

            it("Top left pixel is red") {
                val image = ImageIO.read(croppedImage)
                val rgb = image.getRGB(75, 79)
                val r = DirectColorModel.getRGBdefault().getRed(rgb)
                val g = DirectColorModel.getRGBdefault().getGreen(rgb)
                val b = DirectColorModel.getRGBdefault().getBlue(rgb)
                assertEquals(r, 255)
                assertEquals(g, 0)
                assertEquals(b, 0)
            }
            it("Bottom right pixel is red") {
                val image = ImageIO.read(croppedImage)
                val rgb = image.getRGB(99, 157)
                val r = DirectColorModel.getRGBdefault().getRed(rgb)
                val g = DirectColorModel.getRGBdefault().getGreen(rgb)
                val b = DirectColorModel.getRGBdefault().getBlue(rgb)
                assertEquals(r, 255)
                assertEquals(g, 0)
                assertEquals(b, 0)
            }
            it("Image was correctly cropped") {
                val image = ImageIO.read(croppedImage)
                assertEquals(image.width, 100)
                assertEquals(image.height, 158)
            }
        }
    }
})

private fun createImageWithCroppingArea(x: Int, y: Int, width: Int, height: Int): File {
    val file = Files.createTempFile("test", "png").toFile()
    val image = BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB)
    val g2d = image.createGraphics()
    g2d.color = Color.WHITE
    g2d.fillRect(0, 0, 1000, 700)
    g2d.color = Color.RED
    g2d.fillRect(x, y, width, height)
    g2d.dispose()
    ImageIO.write(image, "png", file)
    return file
}
