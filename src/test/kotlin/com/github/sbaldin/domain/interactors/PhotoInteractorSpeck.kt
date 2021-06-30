package com.github.sbaldin.domain.interactors

import com.github.sbaldin.tbot.data.PhotoSizeModel
import com.github.sbaldin.tbot.domain.PhotoInteractor
import io.mockk.InternalPlatformDsl.toStr
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class PhotoInteractorSpeck : Spek(
    {
        given("Photo Interactor Works Correctly") {
            val tmpdir = Files.createTempDirectory("PhotoInteractorSpeck")
            val photoInteractor = PhotoInteractor(tmpdir.toString())

            val photoModelToPhotoStubMap = mapOf(
                PhotoSizeModel("111", 100, 100, 200) to File.createTempFile("111", "")
                    .apply { writeBytes("test file 1".toByteArray()) },
                PhotoSizeModel("222", 200, 200, 300) to File.createTempFile("222", "")
                    .apply { writeBytes("test file 2".toByteArray()) },
                PhotoSizeModel("333", 300, 300, 100) to File.createTempFile("333", "")
                    .apply { writeBytes("test file 3".toByteArray()) },
            )

            on("Saving photo to storage") {
                val biggestPhotoId = photoInteractor.getBiggestPhoto(photoModelToPhotoStubMap.keys.toList())

                it("The biggest photo from message should be selected") {
                    assertEquals("222", biggestPhotoId)
                }
                it("Photo should be saved to storage dir correctly with highest dimensions") {
                    val savedPhoto = savePhoto(photoInteractor, biggestPhotoId.toInt(), photoModelToPhotoStubMap)
                    assertEquals(listOf("test file 2"), savedPhoto.readLines())
                }
            }
            on("Removing photo from storage") {
                val messageId = 123
                savePhoto(photoInteractor, messageId, photoModelToPhotoStubMap)
                it("Photo should be removed without exceptions") {
                    photoInteractor.removePhotoFromStorage(messageId)
                    assertTrue { Files.notExists(Path.of(tmpdir.toString(), messageId.toString())) }
                }
            }

            on("Copy photo to training storage") {
                val messageId = 123
                savePhoto(photoInteractor, messageId, photoModelToPhotoStubMap)

                val expectedPathToPhoto =
                    Path.of(tmpdir.toString(), PhotoInteractor.TRAINING_DATA_SET_DIR, "$messageId.jpg")
                val actualPathToPhotoInTrainingStorage = photoInteractor.putPhotoToTrainingStorage(messageId)
                val actualPhoto = File(actualPathToPhotoInTrainingStorage.toUri())

                it("Photo should be removed from regular storage") {
                    assertTrue { Files.notExists(Path.of(tmpdir.toString(), messageId.toString())) }
                }
                it("Path to training data set folder should be correct") {
                    assertEquals(expectedPathToPhoto, actualPathToPhotoInTrainingStorage)
                }
                it("Content of photo file should be correct") {
                    assertEquals(listOf("test file 2"), actualPhoto.readLines())
                }
            }
        }
    },
)

private fun savePhoto(
    photoInteractor: PhotoInteractor,
    messageId: Int,
    list: Map<PhotoSizeModel, File>,
) = photoInteractor.savePhotoToStorage(messageId, list.keys.toList()) { fileId ->
    val file = list.entries.find { it.key.fileId == fileId }!!.value
    file.toURI().toURL().toStr()
}
