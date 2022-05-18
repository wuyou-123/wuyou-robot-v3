package pers.wuyou.robot.music.service

import pers.wuyou.robot.music.entity.MusicInfo

/**
 * @author wuyou
 */
interface MusicSearchService {
    /**
     * 登录
     *
     * @return 登录成功返回true
     */
    fun login(): Boolean

    /**
     * 搜索音乐
     *
     * @param name 音乐名
     * @return 返回的结果
     */
    fun search(name: String): List<MusicInfo>

    /**
     * 获取专辑图片
     *
     * @param musicInfo 音乐信息
     * @return 图片链接
     */
    fun getPreview(musicInfo: MusicInfo): String

    /**
     * 下载音乐
     *
     * @param musicInfo 音乐信息
     * @return 下载后的文件名
     */
    fun download(musicInfo: MusicInfo): String?
}