package pers.wuyou.robot.game.gobang.event

import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.isNull
import pers.wuyou.robot.game.common.GameStatus
import pers.wuyou.robot.game.common.interfaces.GameEvent
import pers.wuyou.robot.game.common.interfaces.GameEventMatcher
import pers.wuyou.robot.game.gobang.GobangGame
import pers.wuyou.robot.game.gobang.GobangPlayer
import pers.wuyou.robot.game.gobang.GobangRoom
import java.util.concurrent.TimeUnit

/**
 * @author wuyou
 */
@Component
class Event2 : GameEvent<GobangGame, GobangRoom, GobangPlayer>() {
    override val matcher: GameEventMatcher = GameEventMatcher {
        it == "开始游戏"
    }

    override suspend fun invoke(e: Any) {
        play(e as GobangPlayer)

    }

    override fun getStatus(): GameStatus {
        return GameStatus("123123")
    }

    suspend fun play(player: GobangPlayer) {
        val next = sendRoomAndWaitPlayerNext(player, "开始游戏, 请在10秒内回复", 10, TimeUnit.SECONDS)
        next?.let {
            sendRoom(player, it.plainText)
        }.isNull {
            sendRoom(player, "超时了!")
        }
    }
}