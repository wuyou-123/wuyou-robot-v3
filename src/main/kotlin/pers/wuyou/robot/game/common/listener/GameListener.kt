package pers.wuyou.robot.game.common.listener

import love.forte.simboot.annotation.Filter
import love.forte.simboot.annotation.Filters
import love.forte.simboot.filter.MultiFilterMatchType
import love.forte.simbot.PriorityConstant
import love.forte.simbot.event.ContinuousSessionContext
import love.forte.simbot.event.GroupMessageEvent
import love.forte.simbot.message.At
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.isNull
import pers.wuyou.robot.core.common.send
import pers.wuyou.robot.core.common.sendAndWait
import pers.wuyou.robot.core.common.stringMutableList
import pers.wuyou.robot.core.util.MessageUtil.authorId
import pers.wuyou.robot.core.util.MessageUtil.groupId
import pers.wuyou.robot.game.common.GameAttr
import pers.wuyou.robot.game.common.GameManager
import pers.wuyou.robot.game.common.interfaces.Game
import pers.wuyou.robot.game.common.interfaces.Player
import pers.wuyou.robot.game.common.interfaces.Room
import java.util.concurrent.TimeUnit

/**
 * @author wuyou
 */
@Component
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

    @RobotListen(isBoot = true)
    @Filters(
        Filter("离开|退出|离开房间|退出房间|不玩了|exit"),
        Filter(by = GameEventFilter::class),
        multiMatchType = MultiFilterMatchType.ALL
    )
    @Suppress("UNCHECKED_CAST")
    suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> GroupMessageEvent.leaveRoom(
        @GameAttr("player") player: Player<G, R, P>,
    ) {
        println("离开")
        GameManager.leaveRoom(player as P)
    }

    @RobotListen(isBoot = true)
    @Filter(by = JoinGameFilter::class)
    suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> GroupMessageEvent.game(
        @GameAttr("game") game: Game<G, R, P>,
    ) {
        GameManager.getRoomByPlayerId(authorId())?.let {
            send("你已经在房间 $it 里了")
        }.isNull {
            game.let {
                if (it.canMultiRoom) {
                    // 可以多房间
                    val list = it.roomListById(groupId()).ifEmpty { null }
                    list?.also { roomList ->
                        val msg = stringMutableList()
                        msg += "${it.name}房间列表:"
                        roomList.forEachIndexed { i, room ->
                            msg += "${i + 1}. ${room.getDesc(authorId())}"
                        }
                        msg += "请选择房间编号或者发送\"新建${it.name}房间\"来新建一个房间"
                        sendAndWait(msg, "\n", 1, TimeUnit.MINUTES) { event ->
                            event.messageContent.plainText.toIntOrNull()?.let { index ->
                                return@sendAndWait index > 0 && roomList.size >= index
                            }
                            false
                        }?.plainText?.toInt()?.let { num ->
                            GameManager.joinRoom(this, roomList[num - 1])
                        }
                    }.isNull {
                        // 这个群没有这个类型的房间
                        GameManager.createRoom(it, this)
                    }
                } else {
                    it.roomListById(groupId()).ifEmpty { null }?.also {
                        send("当前已有房间,此游戏不可多房间!")
                    }.isNull {
                        GameManager.createRoom(it, this)
                    }
                }
            }
        }
    }

    @RobotListen(isBoot = true)
    @Filter(by = CreateRoomFilter::class)
    suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> GroupMessageEvent.createRoom(
        @GameAttr("game") game: Game<G, R, P>,
    ) {
        GameManager.getRoomByPlayerId(authorId())?.let {
            send("你已经在房间 $it 里了")
        }.isNull {
            game.let {
                if (it.canMultiRoom) {
                    // 可以多房间
                    GameManager.createRoom(it, this)
                } else {
                    it.roomListById(groupId()).ifEmpty { null }?.also {
                        send("当前已有房间,此游戏不可多房间!")
                    }.isNull {
                        GameManager.createRoom(it, this)
                    }
                }
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    @RobotListen(isBoot = true, priority = PriorityConstant.PRIORITIZED_1)
    @Filter(by = GameEventFilter::class)
    suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> GroupMessageEvent.gameEvent(
        @GameAttr("game") game: Game<G, R, P>,
        @GameAttr("player") player: Player<G, R, P>,
        session: ContinuousSessionContext,
    ) {
        // 如果玩家在等待列表里则不执行游戏事件
        if (game.waitPlayerList.contains(player)) return
        // 根据玩家状态获取游戏事件并执行
        game.eventMap[player.getStatus()]?.let { map ->
            map.find {
                it.matcher.invoke(messageContent.plainText)
            }?.invoke(player).isNull {
                player.room.otherMessage(messageContent)
            }
        }
    }
}
