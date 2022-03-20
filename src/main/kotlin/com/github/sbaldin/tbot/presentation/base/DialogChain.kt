package com.github.sbaldin.tbot.presentation.base

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.Chain
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.types.Message
import com.vdurmont.emoji.EmojiParser
import org.slf4j.Logger
import java.util.ResourceBundle

interface DialogChain {

    fun chain(bot: Bot): ChainBuilder

    fun logger(): Logger

    /**
     * Helper method for creating a [Chain] that is triggered by text message.
     * Catches all exception in action and predicate lambdas.
     * @param trigger a text from [Message] that triggers this [Chain]
     * @param action an action that is called when [Chain] is triggered
     * @return [ChainBuilder]
     */
    fun Bot.safeChain(trigger: String, action: (Message) -> Unit): ChainBuilder {
        val runActionCatching: (Message) -> Unit = { msg: Message ->
            runCatching { action(msg) }.onFailure { logger().error("Error during chain action processing", it) }
        }

        return chain(trigger, runActionCatching)
    }

    /**
     * Helper method for creating a [Chain] that triggered by
     * specified predicate, catches all exception in action and predicate lambdas.
     * @param label used just as a name of the chain
     * @param predicate a predicate that fires the chain if returns `true`
     *                  for incoming [Message]. If there are several predicates
     *                  with the same condition only the first one is called
     * @param action an action that will be called when the chain is fired
     * @return [ChainBuilder]
     */
    fun Bot.safeChain(label: String, predicate: (Message) -> Boolean, action: (Message) -> Unit): ChainBuilder {
        val runActionCatching: (Message) -> Unit = { msg: Message ->
            runCatching { action(msg) }.onFailure { logger().error("Error during chain action processing", it) }
        }

        val runPredicateCatching: (Message) -> Boolean = { msg: Message ->
            runCatching { predicate(msg) }
                .onFailure { logger().error("Error during chain predicates processing", it) }
                .getOrDefault(false)
        }
        return chain(label, runPredicateCatching, runActionCatching)
    }

    fun ResourceBundle.getStringWithEmoji(id: String): String = EmojiParser.parseToUnicode(getString(id))
}
