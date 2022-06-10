package pers.wuyou.robot.game.landlords

import love.forte.simbot.definition.Member
import love.forte.simbot.message.MessageContent
import pers.wuyou.robot.game.common.GameManager
import pers.wuyou.robot.game.common.interfaces.Room

/**
 * @author wuyou
 */
class LandlordsRoom(
    override var id: String,
    override var name: String,
    override val game: LandlordsGame = GameManager.getGame(),
) : Room<LandlordsGame, LandlordsPlayer, LandlordsRoom>() {

    override fun addPlayer(qq: Member): LandlordsPlayer {
        return LandlordsPlayer(qq.id.toString(), qq.nickOrUsername, this).also {
            playerList.add(it)
        }
    }

    override fun playerFull() {
        send("房间已满!开始游戏")
    }

    override fun otherMessage(messageContent: MessageContent) {
        send("收到其他消息")
    }

    override fun toString(): String {
        return "${game.name}(${playerList.size}/${game.maxPlayerCount})[${playerList.joinToString(", ") { it.name }}]"
    }

}