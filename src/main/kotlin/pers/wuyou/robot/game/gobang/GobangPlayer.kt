package pers.wuyou.robot.game.gobang

import pers.wuyou.robot.game.common.GameStatus
import pers.wuyou.robot.game.common.interfaces.Player

/**
 * @author wuyou
 */
class GobangPlayer(
    override var id: String,
    override var name: String,
    override var room: GobangRoom
) : Player<GobangGame, GobangRoom, GobangPlayer>() {
    lateinit var message: String
    override fun getStatus(): GameStatus {
        return GameStatus("123123")
    }
}