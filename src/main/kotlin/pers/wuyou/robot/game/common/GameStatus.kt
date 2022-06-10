package pers.wuyou.robot.game.common

/**
 * @author wuyou
 */
//enum class PlayerStatus(val msg: String) {
//    NO_READY("未准备"), READY("已准备"), PLAYING("游戏中")
//}
//
//enum class RoomStatus(val msg: String) {
//    CREATED("等待玩家加入")
//}

class GameStatus(val status: String) {
    override fun toString(): String {
        return status
    }
}