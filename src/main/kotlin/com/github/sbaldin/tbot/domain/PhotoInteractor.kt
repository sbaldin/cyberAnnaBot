package com.github.sbaldin.tbot.domain

import com.github.sbaldin.tbot.PhotoDestinationDirectory
import com.github.sbaldin.tbot.data.PhotoSizeModel
import com.google.inject.Inject
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class PhotoInteractor @Inject
constructor(
    @PhotoDestinationDirectory
    private val photoStorageDir: String,
) {

    @Throws(DownloadPhotoException::class)
    fun savePhotoToStorage(uniqueId: Int, photos: List<PhotoSizeModel>, fileUrlProvider: (String) -> String): File {
        val photoFolder = Path.of(photoStorageDir)
        val photoIndex = "$uniqueId.jpg"

        if (!Files.exists(photoFolder)) {
            Files.createDirectories(photoFolder)
        }
        val localPhotoFilePath = Path.of(photoStorageDir, photoIndex)
        val localFile = if (Files.exists(localPhotoFilePath)) {
            localPhotoFilePath.toFile()
        } else {
            Files.createFile(localPhotoFilePath).toFile()
        }
        log.info("Saving photo $photoIndex")

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

    fun removePhotoFromStorage(uniqueId: Int) {
        val photoIndex = "$uniqueId.jpg"
        val pathToPhoto = Path.of(photoStorageDir, photoIndex)
        if (Files.exists(pathToPhoto)) {
            Files.delete(pathToPhoto)
        }
    }

    fun putPhotoToTrainingStorage(uniqueId: Int): Path {
        val photoIndex = "$uniqueId.jpg"
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
