package com.github.sbaldin.tbot.domain

import org.slf4j.LoggerFactory

// Handling state allows bot to gather photo that was recognized incorrectly and prepare new training dataset
class GuessingStateHandler(val photoInteractor: PhotoInteractor) {

    fun onSuccessGuessing(msg: Int) {
        log.info("Handle Success Guessing. Photo from msg($msg) will be removed.")
        photoInteractor.removePhotoFromStorage(msg)
    }

    fun onFailedGuessing(msg: Int) {
        log.info("Handle Failed Guessing. Photo from msg($msg) will be added to training dataset.")
        photoInteractor.putPhotoToTrainingStorage(msg)
    }

    companion object{
        val log = LoggerFactory.getLogger(GuessingStateHandler::class.java)
    }
}