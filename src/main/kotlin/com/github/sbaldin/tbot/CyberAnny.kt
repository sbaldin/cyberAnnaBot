package com.github.sbaldin.tbot

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
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
        val message = update.message.text
        val chatId = update.message.chatId.toString()

        log.info(message)

        // We check if the update has a message and the message has text
        if (message.equals("/pic")) {
            // User sent /pic
            val msg = SendPhoto().also {
                it.chatId = chatId
                it.photo = InputFile("AgACAgQAAxkBAAMfYEmVQCgRgXw_CvssJ9BGk4_JXx4AAnupMRtPI2VT8769TpKTXAXkXiAbAAQBAAMCAAN3AAPuXAYAAR4E")
                it.caption = "Photo"
            }
            execute(msg)
        } else if (update.hasMessage() && update.message.hasText()) {
            sendMsg(chatId, message)
        } else if (update.hasMessage() && update.message.hasPhoto()) {
            sendPhoto(update, chatId)
        } else {
            // Unknown command
            sendMsg(chatId, "Unknown command")
        }
    }


    @Synchronized
    private fun sendPhoto(update: Update, chatId: String) {
        // Message contains photo
        // Array with photo objects with different sizes
        // We will get the biggest photo from that array
        val photos = update.message.photo

        val biggestPhoto = photos.asSequence().sortedByDescending { it.fileSize }.first()
        val fileId = biggestPhoto.fileId
        val width = biggestPhoto.width
        val height = biggestPhoto.height
        log.info("File id is $fileId")

        // Set photo caption
        val caption = """
                    file_id: $fileId
                    width: $width
                    height: $height
                    """.trimIndent()

        val msg = SendPhoto().apply {
            photo = InputFile(fileId)
            setChatId(chatId)
            setCaption(caption)
        }

        execute(msg)
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