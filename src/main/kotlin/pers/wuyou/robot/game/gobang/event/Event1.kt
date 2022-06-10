package pers.wuyou.robot.game.gobang.event

import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.Sender
import pers.wuyou.robot.game.common.GameStatus
import pers.wuyou.robot.game.common.interfaces.GameEvent
import pers.wuyou.robot.game.common.interfaces.GameEventMatcher
import pers.wuyou.robot.game.gobang.GobangGame
import pers.wuyou.robot.game.gobang.GobangPlayer
import pers.wuyou.robot.game.gobang.GobangRoom


@Component
class Event1 : GameEvent<GobangGame, GobangRoom, GobangPlayer>() {
    private val pattern = Regex("""^(\d|1[0-4])\s*[^\da-z]*\s*([a-o])$|^([a-o])\s*[^\da-z]*\s*(\d|1[0-4])$""")
    override val matcher: GameEventMatcher = GameEventMatcher {
        pattern.matches(it)
    }

    override suspend fun invoke(e: Any) {
        play(e as GobangPlayer)
    }

    override fun getStatus(): GameStatus {
        return GameStatus("123123")
    }

    suspend fun play(player: GobangPlayer) {
        println("执行了play")
        Sender.sendGroupMsg(player.getRoomId(), "执行了play")
        player.room.game.go(Event2::class, player)
    }
}

