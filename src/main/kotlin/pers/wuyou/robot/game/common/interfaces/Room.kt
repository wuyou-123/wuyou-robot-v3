package pers.wuyou.robot.game.common.interfaces

import love.forte.simbot.event.GroupMessageEvent
import pers.wuyou.robot.core.common.Sender

/**
 * 游戏房间抽象类
 * @author wuyou
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Room<G : Game<G, R, P>, P : Player<G, R, P>, R : Room<G, P, R>> {
    open lateinit var id: String
    open lateinit var name: String

    /**
     * 玩家携带数据
     */
    val playerDataMap: MutableMap<String, MutableMap<String, Any>> = HashMap()

    /**
     * 玩家列表
     */
    val playerList: MutableList<P> = ArrayList()

    /**
     * game实例
     */
    abstract val game: Game<G, R, P>

    /**
     * 判断房间是否已满
     */
    fun isFull(): Boolean = playerList.size >= game.maxPlayerCount

    /**
     * 发送消息
     */
    fun send(messages: Any, separator: String = "") = Sender.sendGroupMsg(id, messages, separator)

    /**
     * 根据QQ号判断玩家是否在房间内
     * @return 玩家是否在房间内
     */
    fun isInRoom(qq: String): Boolean = getPlayer(qq) != null

    /**
     * 根据QQ号获取玩家对象
     * @return 获取到的玩家对象,如果没有则返回null
     */
    fun getPlayer(qq: String): P? = playerList.find { it.id == qq }

    /**
     * 创建房间时执行的方法,用于实现类重写
     */
    open fun createRoom(args: GameArg) {}

    /**
     * 销毁房间时执行的方法,用于实现类重写
     */
    open fun destroy() {}

    /**
     * 加入房间时执行的方法,用于实现类重写,不需要手动添加玩家!
     */
    open fun join(player: P, args: GameArg) {}

    /**
     * 玩家离开房间时执行的方法,用于实现类重写,不需要手动删除玩家!
     */
    open fun leave(player: P) {}

    /**
     * 玩家已满时调用的方法,用于实现类重写
     */
    open fun playerFull() {}

    /**
     * 收到其他消息的处理方法
     */
    open fun otherMessage(player: P, event: GroupMessageEvent) {}

    /**
     * 获取房间描述信息
     */
    fun getDesc(qq: String): String = toString() + if (isInRoom(qq)) "(你在这里)" else ""

    override fun toString(): String = "${game.name}[$name](${playerList.size}/${game.maxPlayerCount})"

}
