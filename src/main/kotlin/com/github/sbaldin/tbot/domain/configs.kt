package com.github.sbaldin.tbot.domain

import com.github.sbaldin.tbot.domain.cnn.CnnImageImageSize


data class BotConf(
    val name: String,
    val token: String
)

data class CnnConf(
    val model:String,
    val inputImageSize: CnnImageImageSize
)