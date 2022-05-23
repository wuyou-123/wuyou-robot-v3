package pers.wuyou.robot.music.listener

import kotlinx.coroutines.withTimeout
import love.forte.simboot.annotation.Filter
import love.forte.simboot.annotation.FilterValue
import love.forte.simboot.filter.MatchType.REGEX_MATCHES
import love.forte.simbot.ExperimentalSimbotApi
import love.forte.simbot.ID
import love.forte.simbot.event.*
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.Sender
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.util.FileUtil
import pers.wuyou.robot.core.util.MessageUtil.getMusicShare
import pers.wuyou.robot.music.entity.MusicInfo
import pers.wuyou.robot.music.entity.musicInfos
import pers.wuyou.robot.music.service.BaseMusicService
import java.io.File

/**
 * @author wuyou
 */
@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalSimbotApi::class)
@Component
class MusicListener(private val database: Database, private val musicSearchService: BaseMusicService) {
    @Value("\${robot.host}")
    private val host: String? = null

    @RobotListen(desc = "点歌", isBoot = true)
    @Filter(value = "^(网易.*|[Q,q]{2}.*|)(点歌|搜歌){{name}}$", matchType = REGEX_MATCHES)
    suspend fun MessageEvent.music(
        @FilterValue("name") name: String,
        session: ContinuousSessionContext,
    ) {
        // 根据前缀搜索歌曲
        val musicInfoList: List<MusicInfo> = when {
            messageContent.plainText.lowercase().startsWith(NET_EASE_PREFIX) -> {
                musicSearchService.search(name, BaseMusicService.SearchService.NET_EASE)
            }
            messageContent.plainText.lowercase().startsWith(QQ_PREFIX) -> {
                musicSearchService.search(name, BaseMusicService.SearchService.QQ)
            }
            else -> {
                musicSearchService.search(name)
            }
        }
        if (musicInfoList.isEmpty()) {
            Sender.send(this, "搜索失败~")
//            getContext(listenerContext).remove(getKey(qq))
            return
        }
        // 构建发送内容
        val list = ArrayList<String>().apply {
            add("\"$name\"的搜索结果, 来源: ${musicInfoList[0].type.serviceName}")
            for (i in musicInfoList.indices) {
                add("${i + 1}. ${musicInfoList[i].title}\n${musicInfoList[i].artist} - ${musicInfoList[i].album}")
            }
            add("发送序号听歌")
            add("如果你想要下载的话,也可以发送\"下载+序号\"来下载歌曲")
        }
        Sender.send(this, list, "\n")
        getNum(session)?.let {
            it.and(0xf).minus(1).let { index ->
                if (index >= 0 && index < musicInfoList.size) {
                    when (it.shr(4)) {
                        1 -> download(musicInfoList[index])
                        0 -> play(musicInfoList[index])
                    }
                }
            }
        }
    }

    @Suppress("FunctionName")
    @RobotListen(desc = "QQ音乐登录")
    @Filter("QQ音乐登录")
    suspend fun FriendMessageEvent.QQLogin() {
        val state = BaseMusicService.SearchService.QQ.musicSearchServiceClass.login()
        logger { "QQ login ${if (state) "success" else "fail"}" }
    }

    private suspend fun MessageEvent.getNum(session: ContinuousSessionContext): Int? = getId(this)?.let { id ->
        try {
            withTimeout(60000L) {
                val pattern = """^(?:下载|播放)?\s*(\d*)$"""
                val text = session.waitingForNextMessage(id.ID, EventMatcher {
                    return@EventMatcher Regex(pattern).matches(it.messageContent.plainText)
                }).plainText
                val num = Regex(pattern).find(text)?.groups?.get(1)?.value
                return@withTimeout num?.let {
                    when {
                        text.startsWith("下载") -> 0x10.or(num.toInt())
                        else -> num.toInt()
                    }
                }
            }
        } catch (e: Exception) {
            return@let -1
        }
    }


    private suspend fun getId(event: MessageEvent): String? {
        return when (event) {
            is GroupMessageEvent -> "music${event.group().id}${event.author().id}"
            is FriendMessageEvent -> "music${event.friend().id}"
            else -> null
        }
    }

    suspend fun MessageEvent.download(musicInfo: MusicInfo) {
        var info: MusicInfo = musicInfo.copy()
        if (musicInfo.payPlay && musicInfo.musicUrl.length < 40) {
            Sender.send(this, "你要下载的歌为付费播放歌曲, 正在通过其他渠道搜索歌曲~")
            info = musicSearchService.search(musicInfo.title, BaseMusicService.SearchService.KU_WO).stream()
                .filter(MusicInfo::payPlay).findFirst().get()
        }
        database.musicInfos.find { it.mid eq info.mid }?.let {
            info = it
        }
        if (info.fileName.isBlank() || !FileUtil.exist(BaseMusicService.TYPE_NAME + File.separator + info.fileName)) {
            val fileName = info.download()
            if (fileName != null && fileName.isNotEmpty()) {
                Sender.send(this, host + "music/" + info.mid)
                info.fileName = fileName
                when (info.id) {
                    0 -> database.musicInfos.add(info)
                    else -> info.flushChanges()
                }
            } else Sender.send(this, "获取下载链接失败,换一个吧~")
            return
        }
        Sender.send(this, host + "music/" + info.mid)
    }

    fun MessageEvent.play(musicInfo: MusicInfo) {
        var info: MusicInfo = musicInfo.copy()
        if (musicInfo.payPlay && musicInfo.musicUrl.length < 40) {
            Sender.send(this, "你点的歌为付费播放歌曲, 正在通过其他渠道搜索歌曲~")
            info = musicSearchService.search(musicInfo.title, BaseMusicService.SearchService.KU_WO).stream()
                .filter(MusicInfo::payPlay).findFirst().get()
        }
        if (info.previewUrl.isEmpty()) {
            info.previewUrl = info.getPreview()
        }
        Sender.send(this, info.getMusicShare())
        database.musicInfos.find { it.mid eq info.mid }?.let {
            it.previewUrl = info.previewUrl
            it.flushChanges()
        }
    }


    companion object {
        private const val NET_EASE_PREFIX = "网易"
        private const val QQ_PREFIX = "qq"
    }
}