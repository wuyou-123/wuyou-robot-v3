package pers.wuyou.robot.game.common.listener

import love.forte.di.annotation.Beans
import love.forte.simboot.annotation.Filter
import love.forte.simboot.listener.ParameterBinder
import love.forte.simboot.listener.ParameterBinderFactory
import love.forte.simboot.listener.ParameterBinderResult
import love.forte.simbot.PriorityConstant
import love.forte.simbot.attribute
import love.forte.simbot.event.EventListenerProcessingContext
import love.forte.simbot.event.GroupMessageEvent
import love.forte.simbot.message.At
import org.springframework.boot.logging.LogLevel
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.*
import pers.wuyou.robot.core.util.MessageUtil.authorId
import pers.wuyou.robot.core.util.MessageUtil.groupId
import pers.wuyou.robot.game.common.GameManager
import pers.wuyou.robot.game.common.interfaces.Game
import pers.wuyou.robot.game.common.interfaces.GameArg
import pers.wuyou.robot.game.common.interfaces.Player
import pers.wuyou.robot.game.common.interfaces.Room
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.findAnnotation

@Beans
class GameParameterBinderFactory : ParameterBinderFactory {
    override fun resolveToBinder(context: ParameterBinderFactory.Context): ParameterBinderResult {
        val attrAnnotation = context.parameter.findAnnotation<GameAttr>() ?: return ParameterBinderResult.empty()
        return ParameterBinderResult.normal(AttrBinder(attrAnnotation.value))
    }

    private class AttrBinder(attrName: String) : ParameterBinder {
        private val attr = attribute<Any>(attrName)
        override suspend fun arg(context: EventListenerProcessingContext): Result<Any?> {
            return kotlin.runCatching { context[attr] }
        }
    }
}

annotation class GameAttr(val value: String)


@Beans
class GameListener {
    @RobotListen(isBoot = true)
    @Filter("房间列表")
    suspend fun GroupMessageEvent.roomList() {
        val msg = mutableListOf<Any>(At(author().id))
        GameManager.getRoomListById(groupId())?.also {
            msg.add("房间列表:")
            it.forEachIndexed { index, room ->
                msg.add("\t${index + 1}." + room.getDesc(authorId()))
            }
        }.isNull {
            msg.add("当前群暂无房间")
        }
        send(msg, "\n")
    }

    /**
     * 加入游戏, 只在玩家不在任何游戏中时执行
     */
    @RobotListen(isBoot = true, id = "JoinGame")
    @Filter(by = GameAnnotationEventFilterFactory::class)
    suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> GroupMessageEvent.game(
        @GameAttr("game") game: Game<G, R, P>,
        @GameAttr("args") args: GameArg,
    ) {
        GameManager.getRoomByPlayerId(authorId())?.also {
            when (it.id) {
                groupId() -> send(it.game.alreadyInRoomTip(it))
                else -> send(it.game.alreadyInOtherRoomTip(it))
            }
        }.isNull {
            if (game.canMultiRoom) {
                // 可以多房间
                val list = game.roomListById(groupId()).ifEmpty { null }
                list?.also { roomList ->
                    val msg = stringMutableList()
                    msg += "${game.name}房间列表:"
                    roomList.forEachIndexed { i, room ->
                        msg += "${i + 1}. ${room.getDesc(authorId())}"
                    }
                    msg += "请选择房间编号或者发送\"新建${game.name}房间\"来新建一个房间"
                    sendAndWait(msg, "\n", 1, TimeUnit.MINUTES) { event ->
                        event.messageContent.plainText.toIntOrNull()?.let { index ->
                            return@sendAndWait index > 0 && roomList.size >= index
                        }
                        false
                    }?.plainText?.toInt()?.let { num ->
                        GameManager.joinRoom(this, roomList[num - 1], args)
                    }
                }.isNull {
                    // 这个群没有这个类型的房间
                    GameManager.createRoom(game, this, args)
                }
            } else {
                game.roomListById(groupId()).ifEmpty { null }?.also {
                    GameManager.joinRoom(this, it[0], args)
                }.isNull {
                    GameManager.createRoom(game, this, args)
                }
            }
        }
    }

    @Suppress("OPT_IN_USAGE", "UNCHECKED_CAST")
    @RobotListen(isBoot = true, priority = PriorityConstant.PRIORITIZED_9, id = "GameEvent")
    @Filter(by = GameAnnotationEventFilterFactory::class)
    suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> GroupMessageEvent.gameEvent(
        @GameAttr("game") game: Game<G, R, P>,
        @GameAttr("player") player: Player<G, R, P>,
    ) {
        // 如果玩家在等待列表里则不执行游戏事件
        if (game.waitPlayerList.contains(player)) return
        // 根据玩家状态获取游戏事件并执行
        game.eventMap[player.getStatus()].let { map ->
            val gameArg = GameArg(this).apply {
                this["player"] = player
                this["game"] = game
            }
            map?.find {
                logger(LogLevel.DEBUG) { "[${game.name}]执行了游戏事件${it.javaClass.simpleName}" }
                it.matcher.invoke(messageContent, gameArg)
            }?.invoke(player as P, gameArg).isNull {
                player.room.otherMessage(player as P, this)
            }
        }
    }
}
