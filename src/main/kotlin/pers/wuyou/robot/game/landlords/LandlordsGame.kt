package pers.wuyou.robot.game.landlords

import org.springframework.stereotype.Component
import pers.wuyou.robot.game.common.interfaces.EventMap
import pers.wuyou.robot.game.common.interfaces.Game
import pers.wuyou.robot.game.common.interfaces.GameEvent

/**
 * @author wuyou
 */
@Component
class LandlordsGame : Game<LandlordsGame, LandlordsRoom, LandlordsPlayer>() {
    override val name = "斗地主"
    override val eventMap = EventMap<MutableList<GameEvent<LandlordsGame, LandlordsRoom, LandlordsPlayer>>>()
    override val roomList: MutableList<LandlordsRoom> = mutableListOf()
    override val minPlayerCount: Int = 1
    override val maxPlayerCount: Int = 3
    override val canMultiRoom: Boolean = false

}