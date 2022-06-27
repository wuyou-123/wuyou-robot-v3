package pers.wuyou.robot.game.gobang

import love.forte.simbot.event.GroupMessageEvent
import pers.wuyou.robot.game.common.interfaces.GameArg
import pers.wuyou.robot.game.common.interfaces.Room

/**
 * @author wuyou
 */
class GobangRoom(
    override var id: String,
    override var name: String,
    override val game: GobangGame,
) : Room<GobangGame, GobangPlayer, GobangRoom>() {

    override fun playerFull() {
        println("房间已满!开始游戏")
    }

    override fun createRoom(args: GameArg) {
        println("新建房间成功!$args")
    }

    override fun join(player: GobangPlayer, args: GameArg) {
        println("玩家 $player 加入了房间")
    }

    override fun destroy() {
        println("玩家为空,房间已自动销毁")
    }

    override fun leave(player: GobangPlayer) {
        println("玩家 $player 离开了房间")
    }

    override fun otherMessage(player: GobangPlayer, event: GroupMessageEvent) {
        println("收到其他消息${event.messageContent.plainText}")
    }

    override fun toString(): String {
        return "${game.name}[$name](${playerList.size}/${game.maxPlayerCount})[${playerList.joinToString(", ") { it.name }}]"
    }

}