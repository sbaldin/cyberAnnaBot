package com.github.sbaldin.tbot

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import javax.ws.rs.container.AsyncResponse
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.InputStream
import java.net.URL
import org.apache.commons.io.FileUtils
import org.asynchttpclient.DefaultAsyncHttpClient
import java.io.File
import javax.ws.rs.container.AsyncResponse.NO_TIMEOUT

import org.asynchttpclient.DefaultAsyncHttpClientConfig

import org.asynchttpclient.AsyncHttpClientConfig
import org.asynchttpclient.RequestBuilder
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.nio.charset.Charset
import org.asynchttpclient.util.ProxyUtils.PROXY_PORT

import org.asynchttpclient.util.ProxyUtils.PROXY_HOST

import com.gargoylesoftware.htmlunit.BrowserVersion
import org.asynchttpclient.util.ProxyUtils
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController





class CyberAnnyBot constructor(
    private val botName: String,
    private val token: String
) : TelegramLongPollingBot() {


    var clientConfig: AsyncHttpClientConfig = DefaultAsyncHttpClientConfig.Builder()
        .setMaxRequestRetry(4)
        .setMaxConnectionsPerHost(1)
        .setMaxConnections(2)  //count of subdomains
        .setIoThreadsCount(1)
        .setReadTimeout(180000) // 3 min
        .setRequestTimeout(180000) // 3 min
        .build()

    private val asyncHttpClient = DefaultAsyncHttpClient(
        clientConfig
    )


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
        when {
            update.hasInlineQuery() -> onInlineQuery(update)
            update.hasMessage() -> onMessage(update)
        }
    }

    private fun onInlineQuery(update: Update) {

        log.info(update.inlineQuery.query)
        log.info(update.inlineQuery.from.userName)

    }

    private fun onMessage(update: Update) {
        val message = update.message.text
        val chatId = update.message.chatId.toString()

        log.info(message)

        // We check if the update has a message and the message has text
        if (update.message.hasText() && message.equals("/pic")) {
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

    //TODO parse google
    //TODO ADD CNN
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

        val uploadedFileId = fileId
        val uploadedFile = GetFile()
        uploadedFile.fileId = uploadedFileId
        val uploadedFilePath: String = execute(uploadedFile).filePath
        val localFile = File.createTempFile(
            "telegram",
            "jpg"
        )
        val imageUrl = "https://api.telegram.org/file/bot$token/$uploadedFilePath"
        val stream = URL(imageUrl).openStream()
        FileUtils.copyInputStreamToFile(stream, localFile)

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

        val googleUrl = "https://www.google.com/searchbyimage?site=search&sa=X&image_url=$imageUrl"
        val request = RequestBuilder("GET").setUrl(googleUrl)
            .setHeader("Authorization", "Bearer $token")
        request.setHeader("Content-Type", "application/json")
        request.setCharset(Charset.forName("UTF-8"))

        //val response = asyncHttpClient.executeRequest(request).get()

        val search = "Apple" //your word to be search on google
        val userAgent = "ExampleBot 1.0 (+http://example.com/bot)"

        val webClient = WebClient(
            BrowserVersion.FIREFOX_78
        )
        webClient.options.isCssEnabled = false
        webClient.options.isJavaScriptEnabled = true
        webClient.options.isThrowExceptionOnScriptError = false
        webClient.options.isThrowExceptionOnFailingStatusCode = false
        webClient.options.timeout = 20000
        webClient.javaScriptTimeout = 20000
        webClient.options.isJavaScriptEnabled = true
        webClient.ajaxController = NicelyResynchronizingAjaxController()

        val myPage: HtmlPage = webClient.getPage(URL(googleUrl))
        // convert page to generated HTML and convert to document
        val doc = Jsoup.parse(myPage.asXml())

        val links: Elements = doc.select(".g>.r>a")

        for (link in links) {
            val title = link.text()
            val url = link.absUrl("href") // Google returns URLs in

            if (!url.startsWith("http")) {
                continue // Ads/news/etc.
            }
            println("Title: " + title)
            println("URL: " + url)
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
