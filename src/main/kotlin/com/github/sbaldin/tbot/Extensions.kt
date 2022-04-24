package com.github.sbaldin.tbot

import com.elbekD.bot.types.Message
import com.elbekD.bot.types.PhotoSize
import com.github.sbaldin.tbot.data.PhotoSizeModel
import org.slf4j.Logger
import java.time.LocalTime
import java.time.temporal.ChronoUnit

fun <T> Logger.measure(operation: String, fn: () -> T): T {
    this.info("Operation started $operation.")
    val t0 = LocalTime.now()
    val result = fn()
    val t1 = LocalTime.now()
    this.info("Operation finished, took ${ChronoUnit.SECONDS.between(t0, t1).toInt()} seconds.")
    return result
}

fun Double.toPercentage(): Double {
    return this * 100
}

fun Message.hasPhoto(): Boolean {
    val hasNoPhoto = new_chat_photo.isNullOrEmpty() && photo.isNullOrEmpty()
    return hasNoPhoto.not()
}

fun PhotoSize.toPhotoSizeModel(): PhotoSizeModel = PhotoSizeModel(
    fileId = file_id,
    fileSize = file_size,
    width = width,
    height = height,
)
