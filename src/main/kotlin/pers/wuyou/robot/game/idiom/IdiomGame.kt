package pers.wuyou.robot.game.idiom

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.isNull
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.util.MessageUtil.groupId
import pers.wuyou.robot.game.common.interfaces.Game
import pers.wuyou.robot.game.common.interfaces.GameArg
import pers.wuyou.robot.game.common.interfaces.Room
import kotlin.io.path.Path
import kotlin.io.path.inputStream

/**
 * 成语接龙
 * @author wuyou
 */
@Component
class IdiomGame : Game<IdiomGame, IdiomRoom, IdiomPlayer>() {
    override val id = "idiom"
    override val name = "成语接龙"
    override val minPlayerCount: Int = 1
    override val maxPlayerCount: Int = 3000
    override val canMultiRoom: Boolean = false
    override val gameArgs: List<String> = listOf("默认成语")
    val idiomFactory = IdiomFactory()

    /**
     * 初始化成语
     */
    override suspend fun load() {
        logger { "[$name] Loading idiom list..." }
        val text = Path(getTempPath() + "idiom.json").inputStream().reader().readText()
        JSON.parseArray(text).forEach {
            (it as JSONObject).let { obj ->
                idiomFactory.add(Idiom(obj.getString("word"), obj.getString("pinyin")))
            }
        }
        idiomFactory.check()
        logger { "[$name] Load idiom list success, ${idiomFactory.size} idioms loaded" }
    }

    /**
     * 检查消息,如果通过则创建游戏或将玩家加入到游戏中
     */
    override fun checkMessage(gameArg: GameArg): GameArg? {
        roomList.find { it.id == runBlocking { gameArg.event!!.groupId() } }.isNull {
            if (gameArg.map["startsWithName"] == true) {
                return gameArg
            }
        }
        // 这里执行判断是不是成语的代码
        idiomFactory.verify(gameArg.event!!.messageContent.plainText)?.also {
            gameArg["idiom"] = it
        }.isNull {
            return null
        }
        return gameArg
    }

    override fun alreadyInRoomTip(room: Room<*, *, *>): String = ""

}