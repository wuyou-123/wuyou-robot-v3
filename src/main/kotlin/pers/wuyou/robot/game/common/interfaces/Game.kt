package pers.wuyou.robot.game.common.interfaces

import pers.wuyou.robot.core.common.isNull
import pers.wuyou.robot.core.exception.RobotException
import pers.wuyou.robot.game.common.GameStatus
import pers.wuyou.robot.game.common.listener.GameListener
import javax.annotation.PostConstruct
import kotlin.reflect.KClass

/**
 * @author wuyou
 */
abstract class Game<G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> {
    /**
     * 游戏名,只能存在一个
     */
    abstract val name: String

    /**
     * 事件Map<[GameEvent],List<[GameEvent]>>
     */
    abstract val eventMap: EventMap<MutableList<GameEvent<G, R, P>>>

    /**
     * 等待玩家列表
     * 当调用[GameEvent.sendRoomAndWaitPlayerNext]时,会等待该玩家的下一条消息,此时不执行事件监听器[GameListener.gameEvent]
     */
    val waitPlayerList: MutableList<P> = mutableListOf()

    /**
     * 该游戏的所有房间列表
     */
    abstract val roomList: MutableList<R>

    /**
     * 最少成员数
     */
    abstract val minPlayerCount: Int

    /**
     * 最大成员数
     */
    abstract val maxPlayerCount: Int

    /**
     * 是否可以多房间
     */
    abstract val canMultiRoom: Boolean

    /**
     * 事件流转,执行目标事件
     */
    suspend fun go(event: KClass<out GameEvent<G, R, P>>, arg: Any) {
        val list = eventMap.values.reduce { acc, gameEvents -> gameEvents.also { it.addAll(acc) } }
        list.find { it::class == event }?.also { it.invoke(arg) }.isNull {
            throw RobotException("game event is not found!")
        }
    }

    @PostConstruct
    fun init() {
        if (minPlayerCount > maxPlayerCount) {
            throw RobotException("maxPlayerCount($maxPlayerCount) mast greater than minPlayerCount($minPlayerCount)")
        }
    }

    /**
     * 根据房间id获取房间列表
     */
    fun roomListById(id: String): List<R> = roomList.filter { it.id == id }
}

/**
 * 事件Map
 */
class EventMap<V> : HashMap<GameStatus, V>() {
    override operator fun get(key: GameStatus): V? = super.get(key).isNull {
        return keys.find { it.status == key.status }?.let { super.get(it) }
    }
}