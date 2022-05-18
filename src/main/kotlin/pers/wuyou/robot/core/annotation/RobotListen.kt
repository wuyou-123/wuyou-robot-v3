package pers.wuyou.robot.core.annotation

import love.forte.simboot.annotation.ContentTrim
import love.forte.simboot.annotation.Listener
import love.forte.simbot.event.GroupMessageEvent
import pers.wuyou.robot.core.enums.RobotPermission

/**
 * @author wuyou
 */
@Suppress("OPT_IN_USAGE")
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS
)
@Listener
@ContentTrim
annotation class RobotListen(
    /**
     * 描述信息
     */
    val desc: String = "",
    /**
     * 执行监听器所需的权限
     */
    val permission: RobotPermission = RobotPermission.MEMBER,
    /**
     * 没有权限时的提示信息
     */
    val noPermissionTip: String = "操作失败,您没有权限",

    /**
     * 是否在当前群开机的时候执行,仅当监听类型是[GroupMessageEvent]时有效
     */
    val isBoot: Boolean = false

)