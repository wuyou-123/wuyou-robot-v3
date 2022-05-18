package pers.wuyou.robot.music.service

import net.mamoe.mirai.message.data.MusicKind
import org.springframework.boot.CommandLineRunner
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.music.entity.MusicInfo
import pers.wuyou.robot.music.service.impl.KuWoSearchImpl
import pers.wuyou.robot.music.service.impl.NetEaseMusicSearchImpl
import pers.wuyou.robot.music.service.impl.QQMusicSearchImpl
import java.io.File

/**
 * @author wuyou
 */
interface BaseMusicService : CommandLineRunner {
    val musicPath get() = RobotCore.PROJECT_PATH + TYPE_NAME + File.separator
    var musicSearchServiceList: ArrayList<MusicSearchService>?

    /**
     * 搜索音乐
     *
     * @param name 音乐名
     * @return 返回的音乐列表
     */
    fun search(name: String): List<MusicInfo>

    /**
     * 搜索音乐
     *
     * @param name    音乐名
     * @param service 搜索引擎
     * @return 返回的音乐列表
     */
    fun search(name: String, service: SearchService): List<MusicInfo>

    enum class SearchService(
        musicSearchServiceClass: Class<out MusicSearchService>,
        val kind: MusicKind,
        val serviceName: String,
        val priority: Int,
    ) {
        /**
         * qq音乐
         */
        QQ(QQMusicSearchImpl::class.java, MusicKind.QQMusic, "QQ音乐", 0),

        /**
         * 网易云音乐
         */
        NET_EASE(NetEaseMusicSearchImpl::class.java, MusicKind.NeteaseCloudMusic, "网易云音乐", 1),

        /**
         * 网易云音乐
         */
        KU_WO(KuWoSearchImpl::class.java, MusicKind.KuwoMusic, "酷我音乐", 2);

        val musicSearchServiceClass: MusicSearchService

        init {
            this.musicSearchServiceClass = RobotCore.applicationContext?.getBean(musicSearchServiceClass)!!
        }
    }

    companion object {
        const val TYPE_NAME = "music"
    }
}