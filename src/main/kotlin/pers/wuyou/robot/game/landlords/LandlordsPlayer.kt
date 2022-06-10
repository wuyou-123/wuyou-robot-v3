package pers.wuyou.robot.game.landlords

import pers.wuyou.robot.game.common.GameStatus
import pers.wuyou.robot.game.common.interfaces.Player

/**
 * @author wuyou
 */
class LandlordsPlayer(
    override var id: String,
    override var name: String,
    override var room: LandlordsRoom
) : Player<LandlordsGame, LandlordsRoom, LandlordsPlayer>() {
    lateinit var message: String
    override fun getStatus(): GameStatus {
        return GameStatus("")
    }
}