package pers.wuyou.robot.game.gobang

import pers.wuyou.robot.game.common.interfaces.Player

/**
 * @author wuyou
 */
class GobangPlayer(
    id: String,
    name: String,
    room: GobangRoom,
) : Player<GobangGame, GobangRoom, GobangPlayer>(id, name, room) {
}