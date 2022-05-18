package pers.wuyou.robot.core.listener

import love.forte.simboot.annotation.Filter
import love.forte.simbot.action.sendIfSupport
import love.forte.simbot.event.GroupMessageEvent
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.entity.GroupBootState
import pers.wuyou.robot.core.entity.GroupBootStates
import pers.wuyou.robot.core.entity.groupBootStates
import pers.wuyou.robot.core.enums.RobotPermission
import pers.wuyou.robot.core.util.MessageUtil

/**
 * 监听群开关机
 *
 * @author wuyou
 */
@Component
class BootListener(private val database: Database) {
    @RobotListen(permission = RobotPermission.ADMINISTRATOR)
    @Filter("开机")
    suspend fun GroupMessageEvent.boot() {
        println(messageContent)
        println(MessageUtil.getAtList(messageContent))
        val groupCode = group().id.toString()
        logger { "群${groupCode}开机" }
        if (!RobotCore.BOOT_MAP.getOrDefault(groupCode, false)) {
            bootOrDown(groupCode, true)
        }
        sendIfSupport("已开机")
    }

    @RobotListen(permission = RobotPermission.ADMINISTRATOR, isBoot = true)
    @Filter("关机")
    suspend fun GroupMessageEvent.down() {
        val groupCode = group().id.toString()
        logger { "群${groupCode}关机" }
        bootOrDown(groupCode, false)
        sendIfSupport("已关机")
    }

    private fun bootOrDown(groupCode: String, state: Boolean) {
        RobotCore.BOOT_MAP[groupCode] = state
        val groupBootState = database.groupBootStates.find { GroupBootStates.groupCode eq groupCode }
        if (groupBootState == null) {
            database.groupBootStates.add(Entity.create<GroupBootState>().also {
                it.groupCode = groupCode
                it.state = state
            })
        } else {
            groupBootState.state = state
            groupBootState.flushChanges()
        }
    }
}