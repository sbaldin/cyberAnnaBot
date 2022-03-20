import com.elbekD.bot.types.Message
import com.github.sbaldin.helpers.MessageProvider
import com.github.sbaldin.tbot.presentation.base.message.isSentInLast5minutes

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PredicatesSpec : Spek({

    describe("#${Message::isSentInLast5minutes.name}") {

        on("Incoming message") {

            it("return false if message appears too late") {
                val earlierThanFiveMinuteAgo = LocalDateTime.of(2022, 1,  1, 0, 0).toInstant(ZoneOffset.UTC)
                val message = MessageProvider.message(1, earlierThanFiveMinuteAgo.epochSecond.toInt() , "test")
               // val message = MessageProvider.message(1, (Date().time /1000L).toInt() , "test")
                assertFalse {
                    message.isSentInLast5minutes()
                }
            }

            it("returns true if message up to date ") {
                val message = MessageProvider.message(1, (Date().time /1000L).toInt() , "test")
               assertTrue {
                    message.isSentInLast5minutes()
                }
            }
        }
    }
})
