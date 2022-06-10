package pers.wuyou.robot.game.common

import love.forte.simbot.attribute
import love.forte.simbot.event.GroupMessageEvent
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.exception.RobotException
import pers.wuyou.robot.core.util.MessageUtil.groupId
import pers.wuyou.robot.game.common.interfaces.Game
import pers.wuyou.robot.game.common.interfaces.Player
import pers.wuyou.robot.game.common.interfaces.Room
import java.lang.reflect.ParameterizedType
import javax.annotation.PostConstruct

/**
 * @author wuyou
 */
@Component
class GameManager {
    lateinit var gameSet: Set<Game<*, *, *>>
    val roomList: MutableList<Room<*, *, *>> = mutableListOf()
    val playerList: MutableList<Player<*, *, *>> = mutableListOf()

    @PostConstruct
    fun init() {
        gameManager = this
        gameSet = RobotCore.applicationContext.getBeansOfType(Game::class.java).let { map ->
            val games: MutableSet<Game<*, *, *>> = HashSet()
            val nameList = mutableListOf<String>()
            map.forEach {
                games.add(it.value)
                if (nameList.contains(it.value.name)) {
                    throw RobotException("已存在名为${it.value.name}的游戏!")
                }
                nameList.add(it.value.name)
                logger { "register game ${it.value.name} success!" }
            }
            games
        }
    }

    companion object {
        lateinit var gameManager: GameManager

        /**
         * 游戏的attribute
         */
        val gameAttribute = attribute<Game<*, *, *>>("game")

        /**
         * 获取[Game]的实例对象
         */
        inline fun <reified T : Game<T, R, P>, R, P> getGame(): T = gameManager.gameSet.find { it is T } as T

        /**
         * 创建房间
         * @param game [Game]的实例对象
         * @param event 群消息事件
         */
        suspend  fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> createRoom(
            game: Game<G, R, P>,
            event: GroupMessageEvent,
        ): R {
            @Suppress("UNCHECKED_CAST") val room =
                (game.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.find {
                    (it as Class<*>).superclass == Room::class.java
                } as Class<R>
            room.kotlin.constructors.find { it.parameters.size == 3 }?.let {
                return it.call(event.groupId(), event.group().name, game).apply {
                    game.roomList.add(this)
                    gameManager.roomList.add(this)
                    gameManager.playerList.add(addPlayer(event.author()))
                    createRoom()
                }
            }
            throw RobotException("实例化房间失败!")
        }

        /**
         * 加入房间
         * @param event 消息事件
         * @param room 房间对象
         */
        suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> joinRoom(
            event: GroupMessageEvent,
            room: Room<G, P, R>,
        ) {
            val player: P = room.addPlayer(event.author())
            gameManager.playerList.add(player)
            room.join(player)
            if (room.isFull()) {
                room.playerFull()
            }
        }

        /**
         * 离开房间
         * @param player 玩家对象
         */
        fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> leaveRoom(player: P) {
            player.room.let {
                assert(it.playerList.contains(player)) { "数据异常!" }
                it.leave(player)
                it.playerList.remove(player)
                if (it.playerList.isEmpty()) {
                    it.destroy()
                    it.game.roomList.remove(it)
                    gameManager.roomList.remove(it)
                }
            }
            gameManager.playerList.remove(player)
        }

        /**
         * 根据名称获取[Game]的实例对象
         */
        fun getGameByName(name: String) = if (name.isNotEmpty()) gameManager.gameSet.find { it.name == name } else null

        /**
         * 根据房间id获取房间列表
         */
        fun getRoomListById(roomId: String) =
            gameManager.roomList.filter { it.id == roomId }.let { it.ifEmpty { null } }

        /**
         * 根据玩家id获取房间
         */
        fun getRoomByPlayerId(qq: String): Room<*, *, *>? {
            gameManager.playerList.find { it.id == qq }?.let {
                return it.room
            }
            return null
        }

        fun getPlayerById(qq: String): Player<*, *, *>? {
            gameManager.playerList.find { it.id == qq }?.let {
                return it
            }
            return null
        }

    }
}
