package pers.wuyou.robot.game.gobang

import org.springframework.stereotype.Component
import pers.wuyou.robot.game.common.interfaces.EventMap
import pers.wuyou.robot.game.common.interfaces.Game
import pers.wuyou.robot.game.common.interfaces.GameEvent

/**
 * @author wuyou
 */
@Component
class GobangGame : Game<GobangGame, GobangRoom, GobangPlayer>() {
    override val name = "五子棋"
    override val eventMap = EventMap<MutableList<GameEvent<GobangGame, GobangRoom, GobangPlayer>>>()
    override val roomList = mutableListOf<GobangRoom>()
    override val minPlayerCount = 1
    override val maxPlayerCount = 2
    override val canMultiRoom = true

}