package pers.wuyou.robot.entertainment.listener

import love.forte.simboot.annotation.Filter
import love.forte.simboot.annotation.FilterValue
import love.forte.simboot.filter.MatchType
import love.forte.simbot.ID
import love.forte.simbot.component.mirai.event.MiraiNudgeEvent
import love.forte.simbot.event.FriendMessageEvent
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.core.common.send
import pers.wuyou.robot.entertainment.entity.Sentence
import pers.wuyou.robot.entertainment.entity.Sentences
import pers.wuyou.robot.entertainment.entity.sentences

/**
 * @author wuyou
 */
@Component
class NudgedListener(private val database: Database) {
    private val messageList: MutableList<String>
    private val cacheMessageList: MutableList<String>

    init {
        val list = database.sequenceOf(Sentences).map { it.text }.toMutableList()
        if (list.size > 3) {
            cacheMessageList = ArrayList(list.subList(0, 3))
            messageList = ArrayList(list.subList(3, list.size))
        } else {
            messageList = list
            cacheMessageList = mutableListOf()
        }
    }

    @RobotListen(isBoot = true)
    suspend fun MiraiNudgeEvent.nudge() {
        if (messageContent.nudgeEvent.target.id.ID != bot.id || messageList.isEmpty()) {
            return
        }
        val text = messageList.removeAt(RobotCore.RANDOM.nextInt(messageList.size))
        cacheMessageList.add(text)
        messageList.add(cacheMessageList.removeAt(0))
        send(text)
    }

    @RobotListen()
    @Filter(value = "^add{{text}}", matchType = MatchType.REGEX_MATCHES)
    suspend fun FriendMessageEvent.addNudgeMessage(
        @FilterValue("text") text: String,
    ) {
        if (RobotCore.ADMINISTRATOR.contains(friend().id.toString())) {
            messageList.add(text)
            database.sentences.add(Entity.create<Sentence>().also { it.text = text })
        }
    }

    @RobotListen()
    @Filter(value = "^remove{{text}}", matchType = MatchType.REGEX_MATCHES)
    suspend fun FriendMessageEvent.removeNudgeMessage(
        @FilterValue("text") text: String,
    ) {
        if (RobotCore.ADMINISTRATOR.contains(friend().id.toString())) {
            messageList.remove(text)
            database.sentences.removeIf { Sentences.text eq text }
        }
    }

    @RobotListen()
    @Filter("list")
    suspend fun FriendMessageEvent.listNudgeMessage() {
        if (RobotCore.ADMINISTRATOR.contains(friend().id.toString())) {
            send(messageList, "\n")
        }
    }
}