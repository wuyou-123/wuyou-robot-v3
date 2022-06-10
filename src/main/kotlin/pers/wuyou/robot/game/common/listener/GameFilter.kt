package pers.wuyou.robot.game.common.listener

import love.forte.simboot.annotation.AnnotationEventFilter
import love.forte.simboot.annotation.Filter
import love.forte.simboot.annotation.Filters
import love.forte.simbot.attribute
import love.forte.simbot.event.EventListener
import love.forte.simbot.event.EventListenerProcessingContext
import love.forte.simbot.event.EventProcessingContext
import love.forte.simbot.event.GroupMessageEvent
import pers.wuyou.robot.core.common.substring
import pers.wuyou.robot.core.util.MessageUtil.authorId
import pers.wuyou.robot.game.common.GameManager

/**
 * @author wuyou
 */
private interface GameFilter : AnnotationEventFilter {
    override fun init(listener: EventListener, filter: Filter, filters: Filters) =
        AnnotationEventFilter.InitType.INDEPENDENT
}

class CreateRoomFilter : GameFilter {
    override suspend fun test(context: EventListenerProcessingContext): Boolean {
        when (context.event) {
            is GroupMessageEvent -> {
                val text = (context.event as GroupMessageEvent).messageContent.plainText.substring("新建", "房间")
                GameManager.getGameByName(text)?.let {
                    context[EventProcessingContext]?.put(attribute("game"), it)
                    return true
                }
                return false
            }
            else -> return false
        }
    }
}

class JoinGameFilter : GameFilter {
    override suspend fun test(context: EventListenerProcessingContext): Boolean {
        when (context.event) {
            is GroupMessageEvent -> {
                val text = (context.event as GroupMessageEvent).messageContent.plainText
                GameManager.getGameByName(text)?.let {
                    context[EventProcessingContext]?.put(GameManager.gameAttribute, it)
                    return true
                }
                return false
            }
            else -> return false
        }
    }
}

/**
 * 游戏事件过滤器
 *
 * 发消息的人在房间里则通过
 */
class GameEventFilter : GameFilter {
    override suspend fun test(context: EventListenerProcessingContext): Boolean {
        return when (context.event) {
            is GroupMessageEvent -> {
                GameManager.getPlayerById((context.event as GroupMessageEvent).authorId()).let {
                    if (it != null) {
                        context[EventProcessingContext]?.put(attribute("player"), it)
                        context[EventProcessingContext]?.put(attribute("room"), it.room)
                        context[EventProcessingContext]?.put(attribute("game"), it.room.game)
                        return true
                    }
                    return false
                }
            }
            else -> false
        }
    }
}