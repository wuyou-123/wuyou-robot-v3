package pers.wuyou.robot.core.listener

import love.forte.simboot.annotation.Filter
import love.forte.simbot.event.GroupMessageEvent
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.entity.GroupBootState
import pers.wuyou.robot.core.entity.GroupBootStates
import pers.wuyou.robot.core.enums.RobotPermission

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
        val groupCode = group().id.toString()
        logger { "群${groupCode}开机" }
        bootOrDown(groupCode, true)
    }

    @RobotListen(permission = RobotPermission.ADMINISTRATOR)
    @Filter("关机")
    suspend fun GroupMessageEvent.down() {
        val groupCode = group().id.toString()
        logger { "群${groupCode}关机" }
        bootOrDown(groupCode, false)
    }

    private fun bootOrDown(groupCode: String, state: Boolean) {
        RobotCore.BOOT_MAP[groupCode] = state
        val groupBootState = database.from(GroupBootStates).select().where {
            GroupBootStates.groupCode eq groupCode
        }.map {
            GroupBootState(
                id = it[GroupBootStates.id],
                groupCode = it[GroupBootStates.groupCode]!!,
                state = it[GroupBootStates.state]!!
            )
        }.firstOrNull()
        if (groupBootState == null) {
            database.insert(GroupBootStates) {
                set(it.groupCode, groupCode)
                set(it.state, state)
            }
        } else {
            database.update(GroupBootStates) {
                set(it.groupCode, groupCode)
                set(it.state, state)
                where {
                    it.id eq groupBootState.id!!
                }
            }
        }
//        if (atList.isEmpty() || atList.contains(RobotCore.getDefaultBotCode())) {
//            groupBootStateService.setGroupState(GroupBootState(group(), true));
//        }
    }
}