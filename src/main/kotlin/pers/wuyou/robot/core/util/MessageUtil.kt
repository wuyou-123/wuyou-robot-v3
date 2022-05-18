package pers.wuyou.robot.core.util

import kotlinx.coroutines.runBlocking
import love.forte.simbot.ID
import love.forte.simbot.component.mirai.message.MiraiMusicShare
import love.forte.simbot.message.At
import love.forte.simbot.message.Message
import love.forte.simbot.message.ReceivedMessageContent
import love.forte.simbot.resources.Resource.Companion.toResource
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.music.entity.MusicInfo
import kotlin.io.path.Path

/**
 * @author wuyou
 */
object MessageUtil {

    fun getAtList(messageContent: ReceivedMessageContent): List<ID> {
        return messageContent.messages.filter { it.key == At.Key }.map { (it as At).target }
    }

    fun getImageMessage(path: String): Message {
        return runBlocking { RobotCore.getBot()!!.uploadImage(Path(path).toResource()) }
    }

    fun getMusicShare(musicInfo: MusicInfo): Message {
        return MiraiMusicShare(
            musicInfo.type.kind,
            musicInfo.title,
            musicInfo.artist,
            musicInfo.jumpUrl,
            musicInfo.previewUrl,
            musicInfo.musicUrl,
            "[分享]${musicInfo.title}"
        )
    }
}