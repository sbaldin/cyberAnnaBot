package com.github.sbaldin.tbot.data

import java.util.Locale

data class BotConf(
    val name: String,
    val locale: String,
    val token: String
) {
    fun locale(): Locale = Locale(locale.toLowerCase(), locale.toUpperCase())
}

data class CnnConf(
    val modelFileName: String,
    val cnnInputLayerSize: CnnInputLayerSizeModel
)