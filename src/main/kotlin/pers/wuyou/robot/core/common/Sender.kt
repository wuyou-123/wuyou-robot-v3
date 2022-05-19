package pers.wuyou.robot.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import love.forte.simbot.ID
import love.forte.simbot.action.SendSupport
import love.forte.simbot.definition.Friend
import love.forte.simbot.definition.Group
import love.forte.simbot.event.Event
import love.forte.simbot.message.Message
import love.forte.simbot.message.Messages
import love.forte.simbot.message.MessagesBuilder


/**
 * @author wuyou
 */
@Suppress("unused")
object Sender {
    fun send(
        event: Event,
        messages: Any,
        separator: String = "",
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            if (event is SendSupport) event.send(buildMessage(messages, separator))
        }
    }

    fun sendGroupMsg(
        group: Group,
        messages: Any,
        separator: String = "",
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            group.send(buildMessage(messages, separator))
        }
    }

    fun sendGroupMsg(
        group: ID,
        messages: Any,
        separator: String = "",
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            getGroup(group)?.send(buildMessage(messages, separator))
        }
    }

    fun sendGroupMsg(
        group: String,
        messages: Any,
        separator: String = "",
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            getGroup(group.ID)?.send(buildMessage(messages, separator))
        }
    }

    fun sendPrivateMsg(
        friend: Friend,
        messages: Any,
        separator: String = "",
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            friend.send(buildMessage(messages, separator))
        }
    }

    fun sendPrivateMsg(
        friend: ID,
        messages: Any,
        separator: String = "",
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            getFriend(friend)?.send(buildMessage(messages, separator))
        }
    }

    fun sendPrivateMsg(
        friend: String,
        messages: Any,
        separator: String = "",
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            getFriend(friend.ID)?.send(buildMessage(messages, separator))
        }
    }

    private fun getFriend(friend: ID): Friend? = runBlocking { RobotCore.getBot()?.friend(friend) }

    private fun getGroup(group: ID): Group? = runBlocking { RobotCore.getBot()?.group(group) }

    private fun buildMessage(
        messages: Any,
        separator: String = "",
    ): Messages = MessagesBuilder().apply {
        when (messages) {
            is Array<*> -> {
                messages.forEachIndexed { index, it ->
                    append(it.toString())
                    if (index != messages.size - 1) {
                        append(separator)
                    }
                }
            }
            is Iterable<*> -> {
                messages.forEachIndexed { index, it ->
                    append(it.toString())
                    if (index != messages.count() - 1) {
                        append(separator)
                    }
                }
            }
            is Message.Element<*> -> append(messages)
            else -> append(messages.toString())
        }
    }.build()
}