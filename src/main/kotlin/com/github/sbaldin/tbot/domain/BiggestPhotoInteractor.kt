package com.github.sbaldin.tbot.domain

import com.github.sbaldin.tbot.data.PhotoSizeModel
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL

class BiggestPhotoInteractor {

    fun saveBiggestPhotoAsTempFile(photos: List<PhotoSizeModel>, fileUrlProvider: (String) -> String): File {
        log.info("Saving Biggest photo from list ${photos.joinToString()}")
        val fileId = getBiggestPhoto(photos)

        val fileUrl = fileUrlProvider(fileId)
        val stream = URL(fileUrl).openStream()
        val localFile = File.createTempFile(
            "telegram",
            "jpg"
        )
        localFile.deleteOnExit()
        FileUtils.copyInputStreamToFile(stream, localFile)
        return localFile
    }

    private fun getBiggestPhoto(photos: List<PhotoSizeModel>) = photos.maxByOrNull { it.fileSize }!!.let { biggestPhoto ->
        log.info("The biggest photo from message is ${biggestPhoto.fileId}")
        biggestPhoto.fileId
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(BiggestPhotoInteractor::class.java)
    }
}