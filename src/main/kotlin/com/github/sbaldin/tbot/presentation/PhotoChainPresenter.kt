package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.feature.chain.terminateChain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.domain.BiggestPhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassifierInteractor
import com.github.sbaldin.tbot.presentation.base.BaseGuessBirdChainPresenter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.ResourceBundle
