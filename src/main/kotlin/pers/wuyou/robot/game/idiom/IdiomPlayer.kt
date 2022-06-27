package pers.wuyou.robot.game.idiom

import pers.wuyou.robot.game.common.interfaces.Player

/**
 * @author wuyou
 */
class IdiomPlayer(
    id: String,
    name: String,
    room: IdiomRoom,
) : Player<IdiomGame, IdiomRoom, IdiomPlayer>(id, name, room) {
    var score: Int = 0
}