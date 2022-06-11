package pers.wuyou.robot.game.common.interfaces

import love.forte.simbot.definition.Member
import love.forte.simbot.message.MessageContent
import pers.wuyou.robot.core.common.Sender

/**
 * 游戏房间抽象类
 * @author wuyou
 */
abstract class Room<G : Game<G, R, P>, P : Player<G, R, P>, R : Room<G, P, R>> {
    open lateinit var id: String
    open lateinit var name: String
//
//    /**
//     * 当前房间状态
//     */
//    var status: RoomStatus = RoomStatus.CREATED

    /**
     * 当前玩家索引
     */
    var currentPlayerIndex: Int = -1

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
     * 添加玩家的方法,房间需要手动实例化玩家对象
     * @return 实例化后的玩家对象
     */
    abstract fun addPlayer(qq: Member): P

    /**
     * 判断房间是否已满
     */
    fun isFull(): Boolean = playerList.size >= game.maxPlayerCount

    /**
     * 发送消息
     */
    fun send(message: String) = Sender.sendGroupMsg(id, message)

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
    open fun createRoom() {}

    /**
     * 销毁房间时执行的方法,用于实现类重写
     */
    open fun destroy() {}

    /**
     * 加入房间时执行的方法,用于实现类重写,不需要手动添加玩家!
     */
    open fun join(player: P) {}

    /**
     * 玩家离开房间时执行的方法,用于实现类重写,不需要手动删除玩家!
     */
    open fun leave(player: P) {}

    /**
     * 玩家已满时调用的方法,为了保证游戏流程此方法必须重写
     */
    abstract fun playerFull()

    /**
     * 收到其他消息的处理方法
     */
    open fun otherMessage(messageContent: MessageContent) {}

    /**
     * 获取房间描述信息
     */
    fun getDesc(qq: String): String = toString() + if (isInRoom(qq)) "(你在这里)" else ""

    override fun toString(): String = "${game.name}[$name](${playerList.size}/${game.maxPlayerCount})"

}
