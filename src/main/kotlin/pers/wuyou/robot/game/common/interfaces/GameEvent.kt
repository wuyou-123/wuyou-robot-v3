package pers.wuyou.robot.game.common.interfaces

import love.forte.simbot.event.EventMatcher
import love.forte.simbot.event.MessageEvent
import love.forte.simbot.message.MessageContent
import pers.wuyou.robot.core.common.Sender
import pers.wuyou.robot.core.common.getBean
import pers.wuyou.robot.core.common.logger
import java.lang.reflect.ParameterizedType
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * @author wuyou
 */
abstract class GameEvent<G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> {
    /**
     * 匹配方法
     */
    abstract val matcher: GameEventMatcher

    /**
     * 事件方法
     */
    abstract suspend fun invoke(e: Any)

    /**
     * 绑定的游戏状态
     */
    abstract fun getStatus(): GameStatus

    @PostConstruct
    fun init() {
        @Suppress("UNCHECKED_CAST") val game =
            getBean((this.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.find {
                (it as Class<*>).superclass == Game::class.java
            } as Class<G>)
        when (val gameEvents = game.eventMap[getStatus()]) {
            null -> game.eventMap[getStatus()] = mutableListOf(this)
            else -> gameEvents.add(this)
        }
        logger { "初始化游戏事件 ${this@GameEvent::class}" }
    }

    /**
     * 向房间内发送消息并等待玩家的下一条消息
     * @param player 玩家对象
     * @param message 消息内容
     * @param timeout 超时时间,单位[timeUnit]
     * @param timeUnit 超时时间单位
     * @param eventMatcher 收到消息时的匹配方法,只返回匹配通过时的消息
     */
    open suspend fun sendRoomAndWaitPlayerNext(
        player: P,
        message: String,
        timeout: Long = 1,
        timeUnit: TimeUnit = TimeUnit.MINUTES,
        eventMatcher: EventMatcher<MessageEvent> = EventMatcher,
    ): MessageContent? {
        player.room.game.waitPlayerList.add(player)
        return Sender.sendGroupAndWait(player.room.id, player.id, message, "", timeout, timeUnit, eventMatcher).also {
            player.room.game.waitPlayerList.remove(player)
        }
    }

    /**
     * 向房间内发送消息
     * @param room
     * @param message 消息内容
     */
    open suspend fun sendRoom(room: R, message: String) = Sender.sendGroupMsg(room.id, message)

}

/**
 * 游戏状态
 */
class GameStatus(val status: String) {
    override fun toString(): String {
        return status
    }
}

/**
 * 事件匹配器
 */
fun interface GameEventMatcher {
    /**
     * 执行匹配方法
     * @param msg 收到的消息
     */
    suspend operator fun invoke(msg: MessageContent): Boolean
}