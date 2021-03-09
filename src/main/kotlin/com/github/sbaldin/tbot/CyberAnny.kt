package com.github.sbaldin.tbot

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class CyberAnnyBot constructor(
    private val botName: String,
    private val token: String
) : TelegramLongPollingBot() {

    /**
     * Метод возвращает token бота для связи с сервером Telegram
     * @return token для бота
     */
    override fun getBotToken(): String {
        return this.token
    }

    /**
     * Метод возвращает имя бота, указанное при регистрации.
     * @return имя бота
     */
    override fun getBotUsername(): String {
        return this.botName
    }

    /**
     * Метод для приема сообщений.
     * @param update Содержит сообщение от пользователя.
     */
    override fun onUpdateReceived(update: Update) {
        val message = update.getMessage().getText()
        log.info(message)
        sendMsg(update.getMessage().getChatId().toString(), message)
    }

    /**
     * Метод для настройки сообщения и его отправки.
     * @param chatId id чата
     * @param s Строка, которую необходимот отправить в качестве сообщения.
     */
    @Synchronized
    fun sendMsg(chatId: String, s: String) {
        val sendMessage = SendMessage()
        sendMessage.enableMarkdown(true)
        sendMessage.chatId = chatId
        sendMessage.text = s
        try {
            execute(sendMessage)
        } catch (e: TelegramApiException) {
            log.error("Exception during sendMessage: ", e)
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(CyberAnnyBot::class.java)
    }
}