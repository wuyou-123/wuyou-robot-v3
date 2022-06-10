package pers.wuyou.robot.entertainment.listener

import love.forte.simboot.annotation.Filter
import love.forte.simboot.annotation.TargetFilter
import love.forte.simbot.event.GroupMessageEvent
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.send
import pers.wuyou.robot.core.util.MessageUtil.authorId
import pers.wuyou.robot.core.util.MessageUtil.getAtSet
import pers.wuyou.robot.core.util.TianApiTool

/**
 * @author wuyou
 * @date 2022/3/15 17:35
 */
@Component
class ChatListener(val tianApiTool: TianApiTool) {

    @RobotListen(isBoot = true)
    @Filter(target = TargetFilter(atBot = true))
    suspend fun GroupMessageEvent.chat() {
        getAtSet().let {
            if (it.size == 1 && it.contains(bot.id)) {
                send(tianApiTool.chatApi(messageContent.plainText.trim(), authorId()))
            }
        }
    }
}