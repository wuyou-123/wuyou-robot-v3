package pers.wuyou.robot.game.idiom

import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.Timer
import pers.wuyou.robot.core.common.then
import pers.wuyou.robot.game.common.GameManager.Companion.destroyRoom
import pers.wuyou.robot.game.common.GameManager.Companion.leaveRoom
import pers.wuyou.robot.game.common.interfaces.GameArg
import pers.wuyou.robot.game.common.interfaces.GameEvent
import pers.wuyou.robot.game.common.interfaces.GameEventMatcher
import java.util.concurrent.TimeUnit

/**
 * 成语接龙游戏事件
 * @author wuyou
 */
@Component
class IdiomEvent(val game: IdiomGame) : GameEvent<IdiomGame, IdiomRoom, IdiomPlayer>() {
    override val matcher = GameEventMatcher { msg, gameArg ->
        // 判断收到的消息,是成语则执行下面的方法
        gameArg["player"]?.let { player ->
            if (player is IdiomPlayer) {
                game.idiomFactory.verifyByEnd(msg.plainText, player.room.nowIdiom)?.also {
                    gameArg["idiom"] = it
                    return@GameEventMatcher true
                }
            }
        }
        false
    }

    override suspend fun invoke(player: IdiomPlayer, gameArg: GameArg) {
        // 游戏事件
        gameArg["idiom"]?.let {
            val room = player.room
            if (it is Idiom) {
                if (it.word.isEmpty()) sendRoom(room, "这个成语不可用哦")
                else {
                    room.idiomList.contains(it).then {
                        sendRoom(room, "这个成语已经用过了哦~")
                        return
                    }
                    room.idiomList.add(it)
                    sendRoom(room, "当前成语: $it\n玩家: ${player.name} 加一分, 当前分数: ${++player.score}")
                    room.nowIdiom = it
                }
            }
            room.timer?.interrupt()
            room.timer = Timer(120, TimeUnit.SECONDS, gameArg, true) {
                60 { room.send("还剩下一分钟!") }
                90 { room.send("还剩下三十秒了哦, 倒计时结束将发送分数!") }
                onFinish {
                    room.send("游戏结束了!")
                    room.destroyRoom()
                }
            }
        }
    }
}

@Component
class IdiomLeaveEvent : GameEvent<IdiomGame, IdiomRoom, IdiomPlayer>() {
    override val matcher = GameEventMatcher { msg, _ ->
        Regex("离开|退出|离开房间|退出房间|不玩了|exit").matches(msg.plainText)
    }

    override suspend fun invoke(player: IdiomPlayer, gameArg: GameArg) = player.leaveRoom()
}

@Component
class IdiomSendScoreEvent : GameEvent<IdiomGame, IdiomRoom, IdiomPlayer>() {
    override val matcher = GameEventMatcher { msg, _ ->
        Regex("查看分数|发送分数|分数").matches(msg.plainText)
    }

    override suspend fun invoke(player: IdiomPlayer, gameArg: GameArg) = player.room.sendScore()
}