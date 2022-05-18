package pers.wuyou.robot.core.exception

/**
 * @author wuyou
 */
open class RobotException : RuntimeException {
    constructor()
    constructor(message: String?) : super(message)
}