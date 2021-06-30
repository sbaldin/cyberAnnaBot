package com.github.sbaldin.tbot.domain.image.cropping

import org.datavec.image.loader.NativeImageLoader
import java.io.File
import javax.imageio.ImageIO

class ImageLoaderProvider {

    fun get(localFile: File): NativeImageLoader = ImageIO.createImageInputStream(localFile).use { iis ->
        val readers = ImageIO.getImageReaders(iis)
        readers.takeIf { it.hasNext() }?.let {
            val reader = it.next()
            try {
                reader.setInput(iis, true)
                NativeImageLoader(
                    reader.getWidth(0).toLong(),
                    reader.getHeight(0).toLong(),
                    3,
                )
            } catch (e: Exception) {
                throw e
            } finally {
                reader.dispose()
            }
        }!!
    }
}
