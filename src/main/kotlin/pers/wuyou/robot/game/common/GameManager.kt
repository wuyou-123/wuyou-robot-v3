package pers.wuyou.robot.game.common

import kotlinx.coroutines.runBlocking
import love.forte.simbot.definition.Member
import love.forte.simbot.event.GroupMessageEvent
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.exception.RobotException
import pers.wuyou.robot.core.util.MessageUtil.authorId
import pers.wuyou.robot.core.util.MessageUtil.groupId
import pers.wuyou.robot.game.common.interfaces.Game
import pers.wuyou.robot.game.common.interfaces.GameArg
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
    val roomList = mutableListOf<Room<*, *, *>>()
    val playerList = mutableListOf<Player<*, *, *>>()

    @PostConstruct
    fun init() {
        gameManager = this
        gameSet = RobotCore.applicationContext.getBeansOfType(Game::class.java).let { map ->
            val games = HashSet<Game<*, *, *>>()
            val nameList = mutableListOf<String>()
            map.forEach {
                logger { "registering game ${it.value.name}(${it.value.id})..." }
                it.value.let { game ->
                    games.add(game)
                    if (nameList.contains(game.id)) {
                        throw RobotException("已存在id为${game.id}的游戏!")
                    }
                    nameList.add(game.id)
                    if (game.minPlayerCount > game.maxPlayerCount) {
                        throw RobotException("maxPlayerCount(${game.maxPlayerCount}) must greater than minPlayerCount(${game.minPlayerCount})")
                    }
                    if (game.minPlayerCount <= 0 || game.maxPlayerCount <= 0) {
                        throw RobotException("maxPlayerCount(${game.maxPlayerCount}) and minPlayerCount(${game.minPlayerCount}) must greater than 1")
                    }
                    game.copyResources()
                    runBlocking { game.load() }
                }
                logger { "register game ${it.value.name}(${it.value.id}) success!" }
            }
            games
        }
    }

    companion object {
        lateinit var gameManager: GameManager

        /**
         * 获取[Game]的实例对象
         */
        inline fun <reified T : Game<T, R, P>, R, P> getGame(): T = gameManager.gameSet.find { it is T } as T

        /**
         * 创建房间
         * @param game [Game]的实例对象
         * @param event 群消息事件
         */
        suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> createRoom(
            game: Game<G, R, P>,
            event: GroupMessageEvent,
            args: GameArg,
        ): R {
            return instanceRoom(game, event)?.apply {
                game.roomList.add(this)
                gameManager.roomList.add(this)
                instancePlayer(this, event.author())?.also { player ->
                    playerList.add(player)
                    gameManager.playerList.add(player)
                    if (!playerList.contains(player)) {
                        playerList.add(player)
                    }
                } ?: throw RobotException("初始化玩家失败!")
                createRoom(args)
            } ?: throw RobotException("初始化房间失败!")
        }

        /**
         * 加入房间
         * @param event 消息事件
         * @param room 房间对象
         */
        suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> joinRoom(
            event: GroupMessageEvent,
            room: R,
            args: GameArg,
        ) {
            val player = instancePlayer(room, event.author()) ?: throw RobotException("初始化玩家失败!")
            gameManager.playerList.add(player)
            room.playerList.add(player)
            room.join(player, args)
            if (room.isFull()) {
                room.playerFull()
            }
        }

        /**
         * 离开房间
         */
        fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> P.leaveRoom() {
            room.let {
                assert(it.playerList.contains(this)) { "数据异常!" }
                it.leave(this)
                it.playerList.remove(this)
                if (it.playerList.isEmpty()) {
                    it.destroy()
                    it.game.roomList.remove(it)
                    gameManager.roomList.remove(it)
                }
            }
            gameManager.playerList.remove(this)
        }

        /**
         * 销毁房间
         */
        fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> R.destroyRoom() {
            this.playerList.forEach {
                gameManager.playerList.remove(it)
            }
            destroy()
            game.roomList.remove(this)
            gameManager.roomList.remove(this)
        }

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

        suspend fun GroupMessageEvent.getPlayer(): Player<*, *, *>? {
            gameManager.playerList.find { it.id == authorId() }?.let {
                if (it.getRoomId() == groupId()) {
                    return it
                }
            }
            return null
        }

        /**
         * 实例化房间
         */
        private suspend fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> instanceRoom(
            game: Game<G, R, P>,
            event: GroupMessageEvent,
        ): R? {
            @Suppress("UNCHECKED_CAST") val room =
                (game.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.find {
                    (it as Class<*>).superclass == Room::class.java
                } as Class<R>
            room.kotlin.constructors.find { it.parameters.size == 3 }?.let {
                return it.call(event.groupId(), event.group().name, game)
            }
            return null
        }

        /**
         * 实例化房间
         */
        private fun <G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> instancePlayer(
            room: R,
            qq: Member,
        ): P? {
            @Suppress("UNCHECKED_CAST") val player =
                (room.game.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.find {
                    (it as Class<*>).superclass == Player::class.java
                } as Class<P>
            player.kotlin.constructors.find { it.parameters.size == 3 }?.let {
                return it.call(qq.id.toString(), qq.nickOrUsername, room)
            }
            return null
        }

    }
}
