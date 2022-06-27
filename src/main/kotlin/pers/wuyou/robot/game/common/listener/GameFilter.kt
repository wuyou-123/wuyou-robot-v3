package pers.wuyou.robot.game.common.listener

import love.forte.simboot.annotation.AnnotationEventFilterFactory
import love.forte.simboot.annotation.Filter
import love.forte.simboot.annotation.Filters
import love.forte.simbot.MutableAttributeMap
import love.forte.simbot.attribute
import love.forte.simbot.event.*
import pers.wuyou.robot.game.common.GameManager
import pers.wuyou.robot.game.common.GameManager.Companion.getPlayer
import pers.wuyou.robot.game.common.interfaces.Game
import kotlin.reflect.full.createInstance

@Suppress("unused")
class GameAnnotationEventFilterFactory : AnnotationEventFilterFactory {
    override fun resolveFilter(
        listener: EventListener,
        listenerAttributes: MutableAttributeMap,
        filter: Filter,
        filters: Filters,
    ): EventFilter? {
//        通过监听器id获取过滤器,但现在id获取不到,等下个版本再用
        return when (listener.id) {
            "JoinGame" -> JoinGameFilter::class.createInstance()
            "GameEvent" -> GameEventFilter::class.createInstance()
            else -> null
        }
    }
}

class JoinGameFilterFactory : AnnotationEventFilterFactory {
    override fun resolveFilter(
        listener: EventListener,
        listenerAttributes: MutableAttributeMap,
        filter: Filter,
        filters: Filters,
    ): EventFilter {
        return JoinGameFilter::class.createInstance()
    }
}

class GameEventFilterFactory : AnnotationEventFilterFactory {
    override fun resolveFilter(
        listener: EventListener,
        listenerAttributes: MutableAttributeMap,
        filter: Filter,
        filters: Filters,
    ): EventFilter {
        return GameEventFilter::class.createInstance()
    }
}

/**
 * 加入游戏过滤器
 *
 * 发送的消息匹配[Game.checkMessage]方法则通过
 */
class JoinGameFilter : EventFilter {
    override suspend fun test(context: EventListenerProcessingContext): Boolean {
        if (context.event is GroupMessageEvent) {
            GameManager.gameManager.gameSet.let {
                it.forEach { game ->
                    game.checkMessage(context.event as GroupMessageEvent)?.let { args ->
                        context[EventProcessingContext]?.put(attribute("game"), game)
                        context[EventProcessingContext]?.put(attribute("args"), args)
                        return true
                    }
                }
            }
        }
        return false
    }

}

/**
 * 游戏事件过滤器
 *
 * 发消息的人在房间里则通过
 */
class GameEventFilter : EventFilter {
    override suspend fun test(context: EventListenerProcessingContext): Boolean {
        context.event.let { event ->
            if (event is GroupMessageEvent) {
                event.getPlayer()?.let {
                    context[EventProcessingContext]?.put(attribute("player"), it)
                    context[EventProcessingContext]?.put(attribute("room"), it.room)
                    context[EventProcessingContext]?.put(attribute("game"), it.room.game)
                    return true
                }
            }
        }
        return false
    }
}