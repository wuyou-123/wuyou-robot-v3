package pers.wuyou.robot.game.gobang.event

import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.isNull
import pers.wuyou.robot.game.common.interfaces.GameArg
import pers.wuyou.robot.game.common.interfaces.GameEvent
import pers.wuyou.robot.game.common.interfaces.GameEventMatcher
import pers.wuyou.robot.game.common.interfaces.GameStatus
import pers.wuyou.robot.game.gobang.GobangGame
import pers.wuyou.robot.game.gobang.GobangPlayer
import pers.wuyou.robot.game.gobang.GobangRoom
import java.util.concurrent.TimeUnit

/**
 * @author wuyou
 */
@Component
class Event2 : GameEvent<GobangGame, GobangRoom, GobangPlayer>() {
    override val matcher: GameEventMatcher = GameEventMatcher { it, _ ->
        it.plainText == "开始游戏"
    }

    override suspend fun invoke(player: GobangPlayer, gameArg: GameArg) {
        play(player)

    }

    override fun getStatus(): GameStatus {
        return GameStatus("123123")
    }

    suspend fun play(player: GobangPlayer) {
        val next = sendRoomAndWaitPlayerNext(player, "开始游戏, 请在10秒内回复", 10, TimeUnit.SECONDS)
        next?.let {
            sendRoom(player.room, it.plainText)
        }.isNull {
            sendRoom(player.room, "超时了!")
        }
    }
}