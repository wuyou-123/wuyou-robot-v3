package pers.wuyou.robot.game.common.interfaces

import pers.wuyou.robot.core.common.Sender

/**
 * @author wuyou
 */
abstract class Player<G : Game<G, R, P>, R : Room<G, P, R>, P : Player<G, R, P>>(
    open var id: String,
    open var name: String,
    open var room: R,
) {
    var isPlaying = false
    var pre: P? = null
    var next: P? = null

    fun getRoomId(): String {
        return room.id
    }

    fun isInRoom(roomId: String?): Boolean = room.id == roomId
    fun send(message: String) = Sender.sendPrivateMsg(id, message)
    override fun toString(): String = "${name}[$id]"
    open fun getStatus() = GameStatus("")
}

