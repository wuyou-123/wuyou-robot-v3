package pers.wuyou.robot.game.gobang

import love.forte.simbot.definition.Member
import love.forte.simbot.message.MessageContent
import pers.wuyou.robot.game.common.interfaces.Room

/**
 * @author wuyou
 */
class GobangRoom(
    override var id: String,
    override var name: String,
    override val game: GobangGame,
) : Room<GobangGame, GobangPlayer, GobangRoom>() {

    override fun addPlayer(qq: Member): GobangPlayer {
        return GobangPlayer(qq.id.toString(), qq.nickOrUsername, this).also {
            playerList.add(it)
        }
    }

    override fun playerFull() {
        send("房间已满!开始游戏")
    }

    override fun createRoom() {
        send("新建房间成功!")
    }

    override fun join(player: GobangPlayer) {
        send("玩家 $player 加入了房间")
    }

    override fun destroy() {
        send("玩家为空,房间已自动销毁")
    }

    override fun leave(player: GobangPlayer) {
        send("玩家 $player 离开了房间")
    }

    override fun otherMessage(messageContent: MessageContent) {
        send("收到其他消息")
    }

    override fun toString(): String {
        return "${game.name}[$name](${playerList.size}/${game.maxPlayerCount})[${playerList.joinToString(", ") { it.name }}]"
    }

}