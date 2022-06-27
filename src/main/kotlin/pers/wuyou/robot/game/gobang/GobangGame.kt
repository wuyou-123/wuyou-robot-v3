package pers.wuyou.robot.game.gobang

import org.springframework.stereotype.Component
import pers.wuyou.robot.game.common.interfaces.Game

/**
 * 五子棋游戏, 懒得写了
 * @author wuyou
 */
@Component
class GobangGame : Game<GobangGame, GobangRoom, GobangPlayer>() {
    override val id = "gobang"
    override val name = "五子棋"
    override val minPlayerCount = 1
    override val maxPlayerCount = 2
    override val canMultiRoom = true
    override val gameArgs: List<String> = listOf("模式", "难度")


}