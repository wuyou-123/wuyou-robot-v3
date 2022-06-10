package pers.wuyou.robot.core.util

import kotlinx.coroutines.runBlocking
import love.forte.simbot.ID
import love.forte.simbot.component.mirai.message.MiraiMusicShare
import love.forte.simbot.event.ChatRoomMessageEvent
import love.forte.simbot.event.FriendMessageEvent
import love.forte.simbot.event.GroupEvent
import love.forte.simbot.event.GroupMessageEvent
import love.forte.simbot.message.At
import love.forte.simbot.message.Message
import love.forte.simbot.resources.Resource.Companion.toResource
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.music.entity.MusicInfo
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * @author wuyou
 */
@Suppress("MemberVisibilityCanBePrivate")
object MessageUtil {

    fun GroupMessageEvent.getAtList(): List<ID> {
        return messageContent.messages.filter { it.key == At.Key }.map { (it as At).target }
    }

    fun GroupMessageEvent.getAtSet(): Set<ID> {
        return HashSet(getAtList())
    }

    fun String.getImageMessage(): Message {
        return Path(this).getImageMessage()
    }

    fun Path.getImageMessage(): Message {
        return runBlocking { RobotCore.getBot()!!.uploadImage(this@getImageMessage.toResource()) }
    }

    fun MusicInfo.getMusicShare(): Message {
        return MiraiMusicShare(type.kind, title, artist, jumpUrl, previewUrl, musicUrl, "[分享]${title}")
    }

    suspend fun ChatRoomMessageEvent.authorId(): String {
        return author().id.toString()
    }

    suspend fun FriendMessageEvent.authorId(): String {
        return friend().id.toString()
    }

    suspend fun GroupEvent.groupId(): String {
        return group().id.toString()
    }
}