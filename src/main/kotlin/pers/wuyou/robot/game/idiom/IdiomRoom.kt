package pers.wuyou.robot.game.idiom

import kotlinx.coroutines.runBlocking
import pers.wuyou.robot.core.common.Timer
import pers.wuyou.robot.core.common.isNull
import pers.wuyou.robot.core.common.then
import pers.wuyou.robot.game.common.GameManager
import pers.wuyou.robot.game.common.GameManager.Companion.destroyRoom
import pers.wuyou.robot.game.common.interfaces.GameArg
import pers.wuyou.robot.game.common.interfaces.Room
import java.util.concurrent.TimeUnit

/**
 * @author wuyou
 */
class IdiomRoom(
    override var id: String,
    override var name: String,
    override val game: IdiomGame = GameManager.getGame(),
) : Room<IdiomGame, IdiomPlayer, IdiomRoom>() {
    var timer: Timer<GameArg>? = null
    val idiomList = mutableListOf<Idiom>()
    lateinit var nowIdiom: Idiom

    /**
     * 创建房间,开启定时器
     */
    override fun createRoom(args: GameArg) {
        timer = Timer(120, TimeUnit.SECONDS, args, true) {
            onStart {
                val sendList = mutableListOf("成语接龙 开始咯! 倒计时120秒!")
                args["默认成语"]?.let {
                    val idiom = game.idiomFactory.verify(it.toString()) ?: return@let null
                    if (idiom.word.isEmpty()) return@let null
                    sendList.add("第一个成语: $idiom")
                    nowIdiom = idiom
                }.isNull {
                    game.idiomFactory.randomIdiom().let {
                        nowIdiom = it
                        sendList.add("我先来: $it")
                    }
                }
                send(sendList, "\n")
            }
            60 {
                playerList.any { it.score > 0 }.then {
                    send("还剩下一分钟!")
                }
            }
            onFinish {
                send("游戏结束了!")
                destroyRoom()
            }
        }
    }

    override fun destroy() {
        sendScore()
        // 中断定时器
        timer?.interrupt()
    }

    fun sendScore() {
        // 如果没有人有分数则不发送
        playerList.none { it.score > 0 }.then { return }
        val list = mutableListOf("当前分数")
        // 排序
        playerList.sortByDescending { it.score }
        playerList.filter { it.score > 0 }.forEachIndexed { index, it ->
            list.add("${index + 1}.${it.name}(${it.score})")
        }
        send(list, "\n")
    }

    /**
     * 玩家加入房间的事件
     */
    override fun join(player: IdiomPlayer, args: GameArg) {
        runBlocking {
            // 因为在加入房间时判断了消息是成语,所以这里直接流转到游戏事件
            game.go(IdiomEvent::class, player, args)
        }
    }
}