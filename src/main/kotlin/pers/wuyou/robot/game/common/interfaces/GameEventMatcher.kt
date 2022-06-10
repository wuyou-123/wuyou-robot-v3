package pers.wuyou.robot.game.common.interfaces

/**
 * @author wuyou
 */
fun interface GameEventMatcher {
    /**
     * 执行匹配方法
     * @param msg 收到的消息
     */
    suspend operator fun invoke(msg: String): Boolean
}