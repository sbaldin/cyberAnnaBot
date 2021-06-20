package com.github.sbaldin.tbot.domain

import com.github.sbaldin.tbot.data.PhotoSizeModel
import kotlin.jvm.Throws
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class PhotoInteractor(private val photoStorageDir: String) {

    @Throws(DownloadPhotoException::class)
    fun savePhotoToStorage(messageId: Int, photos: List<PhotoSizeModel>, fileUrlProvider: (String) -> String): File {
        val photoFolder = Path.of(photoStorageDir)

        if (!Files.exists(photoFolder)) {
            Files.createDirectories(photoFolder)
        }
        val photoIndex = "$messageId.jpg"
        log.info("Saving photo ${photoIndex}")
        val localFile = Files.createFile(Path.of(photoStorageDir, photoIndex)).toFile()

        val fileId = getBiggestPhoto(photos)
        val fileUrl = fileUrlProvider(fileId)
        try {
            val stream = URL(fileUrl).openStream()
            FileUtils.copyInputStreamToFile(stream, localFile)
        } catch (e: IOException) {
            throw DownloadPhotoException("Can't download photo and save to local file!\n" + e.message)
        }
        return localFile
    }

    fun getBiggestPhoto(photos: List<PhotoSizeModel>) = photos.maxByOrNull { it.fileSize }!!.let { biggestPhoto ->
        log.info("The biggest photo from message is ${biggestPhoto.fileId}")
        biggestPhoto.fileId
    }

    fun removePhotoFromStorage(messageId: Int) {
        val photoIndex = "$messageId.jpg"
        val pathToPhoto = Path.of(photoStorageDir, photoIndex)
        if (Files.exists(pathToPhoto)) {
            Files.delete(pathToPhoto)
        }
    }

    fun putPhotoToTrainingStorage(messageId: Int): Path {
        val photoIndex = "$messageId.jpg"
        val pathToPhoto = Path.of(photoStorageDir, photoIndex)
        val trainingPath = Path.of(photoStorageDir, TRAINING_DATA_SET_DIR)
        if (!Files.exists(trainingPath)) {
            Files.createDirectory(trainingPath)
        }

        val pathToTrainingStorage = Path.of(photoStorageDir, TRAINING_DATA_SET_DIR, photoIndex)
        return Files.copy(pathToPhoto, pathToTrainingStorage, StandardCopyOption.REPLACE_EXISTING).also {
            Files.delete(pathToPhoto)
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(PhotoInteractor::class.java)
        const val TRAINING_DATA_SET_DIR = "training"
    }
}

class DownloadPhotoException(override val message: String) : Exception(message)