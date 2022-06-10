package pers.wuyou.robot.game.common.interfaces

import pers.wuyou.robot.core.common.Sender
import pers.wuyou.robot.game.common.GameStatus

/**
 * @author wuyou
 */
abstract class Player<G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>> {
    open lateinit var id: String
    open lateinit var name: String
    open lateinit var room: R
    var isPlaying = false
    var pre: P? = null
    var next: P? = null

    fun getRoomId(): String {
        return room.id
    }

    fun isInRoom(roomId: String?): Boolean = room.id == roomId
    fun send(message: String) = Sender.sendPrivateMsg(id, message)
    override fun toString(): String = "${name}[$id]"
    abstract fun getStatus(): GameStatus
}

