package pers.wuyou.robot.music.entity

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import pers.wuyou.robot.music.service.BaseMusicService

/**
 * music_info
 *
 * @author wuyou
 */
interface MusicInfo : Entity<MusicInfo> {
    fun download(): String? {
        return type.musicSearchServiceClass.download(this)
    }

    val id: Int

    /**
     * 音乐id
     */
    var mid: String

    /**
     * 标题
     */
    var title: String

    /**
     * 子标题
     */
    var subtitle: String?

    /**
     * 艺术家
     */
    var artist: String

    /**
     * 专辑
     */
    var album: String

    /**
     * 封面链接
     */
    var previewUrl: String

    /**
     * 跳转链接
     */
    var jumpUrl: String

    /**
     * 文件名
     */
    var fileName: String

    /**
     * 是否要付费播放
     */
    var payPlay: Boolean

    /**
     * 跳转链接
     */
    var musicUrl: String

    /**
     * 类型
     */
    var type: BaseMusicService.SearchService
}

@Suppress("unused")
object MusicInfos : Table<MusicInfo>("music_info") {
    val id = int("id").primaryKey().bindTo { it.id }
    val mid = varchar("mid").bindTo { it.mid }
    val title = varchar("title").bindTo { it.title }
    val subtitle = varchar("subtitle").bindTo { it.subtitle }
    val artist = varchar("artist").bindTo { it.artist }
    val album = varchar("album").bindTo { it.album }
    val previewUrl = varchar("preview_url").bindTo { it.previewUrl }
    val jumpUrl = varchar("jump_url").bindTo { it.jumpUrl }
    val fileName = varchar("file_name").bindTo { it.fileName }
    val payPlay = boolean("pay_play").bindTo { it.payPlay }

}
val Database.musicInfos get() = this.sequenceOf(MusicInfos)
