package pers.wuyou.robot.core.common

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.forte.simbot.event.Event
import love.forte.simbot.event.GroupMessageEvent
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.enums.RobotPermission

/**
 * 拦截监听器
 * @author wuyou
 */

@Component
@Aspect
class ListenerAspect {

    /**
     * 拦截监听器方法
     */
    @Around("@annotation(pers.wuyou.robot.core.annotation.RobotListen) && @annotation(annotation)) && args(continuation)")
    fun ProceedingJoinPoint.doAroundAdvice(annotation: RobotListen): Any? {
        val start = System.currentTimeMillis()
        val event = args.find { it is Event } ?: return proceed()
        fun proceedSuccess(): Any? {
            logger {
                -"执行了监听器${signature.name}(${annotation.desc})"
                -("执行拦截器耗时: " + (System.currentTimeMillis() - start))
            }
            return proceed()
        }

        fun proceedFailed(tip: String) {
            logger {
                -"执行监听器${signature.name}(${annotation.desc})失败, $tip"
                -("执行拦截器耗时: " + (System.currentTimeMillis() - start))
            }
            return
        }

        if (event is GroupMessageEvent) {
            val group = runBlocking { event.group() }
            val author = runBlocking { event.author() }
            val role = runBlocking { author.roles().first() }
            // 判断是否开机
            if (annotation.isBoot && !RobotCore.BOOT_MAP.getOrDefault(group.id.toString(), false)) {
                return proceedFailed("当前群未开机")
            }
            // 判断是否有权限
            if (
                annotation.permission != RobotPermission.MEMBER &&
                annotation.permission > role && !RobotCore.isBotAdministrator(author.id.toString())
            ) {
                if (annotation.noPermissionTip.isNotBlank()) {
                    Sender.send(group, annotation.noPermissionTip)
                    return proceedFailed("权限不足")
                }
            }
        }
        return proceedSuccess()
    }
}

