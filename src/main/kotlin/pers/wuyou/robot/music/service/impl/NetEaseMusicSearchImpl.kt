package pers.wuyou.robot.music.service.impl

import com.alibaba.fastjson2.JSONObject
import com.sun.deploy.util.URLUtil
import org.ktorm.entity.Entity
import org.springframework.stereotype.Service
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.util.HttpUtil
import pers.wuyou.robot.core.util.ResponseEntity
import pers.wuyou.robot.music.config.MusicProperties
import pers.wuyou.robot.music.entity.MusicInfo
import pers.wuyou.robot.music.service.BaseMusicService
import pers.wuyou.robot.music.service.MusicSearchService

/**
 * 网易云音乐实现
 *
 * @author wuyou
 */
@Service("NetEaseMusicSearchImpl")
class NetEaseMusicSearchImpl(
    musicProperties: MusicProperties, private val baseMusicService: BaseMusicService,
) : MusicSearchService {
    private final val uin: String?
    private final val pwd: String?
    private final val serverHost: String?

    private val loginUrl = "login/cellphone?phone=%s&password=%s"
    private val loginRefreshUrl = "login/refresh"
    private val searchUrl = "cloudsearch?keywords=%s&limit=10"
    private val musicPlayUrl = "song/url?id=%s"
    private val musicDownloadUrl = "song/download/url?id=%s&br=%s"
    private val musicJumpUrl = "https://music.163.com/#/song?id=%s"
    private val brArray = arrayOf(1411000, 999000, 320000, 128000)
    private val cookie: MutableMap<String, String> = HashMap()
    private val successCode = 200

    init {
        this.uin = musicProperties.netEase.account
        this.pwd = musicProperties.netEase.password
        this.serverHost = musicProperties.netEase.serverHost
    }

    override fun login(): Boolean {
        cookie.clear()
        val responseEntity: ResponseEntity = HttpUtil.get(serverHost + String.format(loginUrl, uin, pwd))
        cookie.putAll(responseEntity.cookies)
        val json: JSONObject = responseEntity.getJSONResponse()!!
        val code = json["code"]
        if (code == successCode) {
            return true
        }
        logger { "NetEase login fail." }
        logger { json.toJSONString() }
        return false
    }

    @Suppress("DuplicatedCode")
    override fun search(name: String): List<MusicInfo> {
        refreshCookie()
        val json =
            HttpUtil.get {
                url = serverHost + java.lang.String.format(searchUrl, URLUtil.encodePath(name.trim()))
                cookies = { -cookie }
            }.getJSONResponse()!!
        val result = json.getJSONObject("result")
        val jsonArray = result.getJSONArray("songs")
        val list: MutableList<MusicInfo> = ArrayList()
        for (i in jsonArray.indices) {
            val jsonObject = jsonArray.getJSONObject(i)
            val mid = jsonObject.getString("id")
            val title = jsonObject.getString("name")
            val al = jsonObject.getJSONObject("al")
            val album = al.getString("name")
            val previewUrl = al.getString("picUrl")
            val artistsJson = jsonObject.getJSONArray("ar")
            val artistList: List<String> = artistsJson.map {
                (it as JSONObject).getString("name")
            }
            val artists = artistList.joinToString("&")
            val payPlay: Boolean = jsonObject.getInteger("fee")?.let { it != 0 } ?: true
            val music = HttpUtil.get {
                url = serverHost + String.format(musicPlayUrl, mid)
                cookies = { -cookie }
            }.getJSONResponse()!!
            val musicUrl = music.getJSONArray("data").getJSONObject(0).getString("url") ?: ""
            val jumpUrl = String.format(musicJumpUrl, mid)
            val musicInfo = Entity.create<MusicInfo>().also {
                it.mid = mid
                it.artist = artists
                it.album = album
                it.title = title
                it.previewUrl = previewUrl
                it.jumpUrl = jumpUrl
                it.musicUrl = musicUrl
                it.payPlay = payPlay
            }
            list.add(musicInfo)
        }
        return list
    }

    override fun getPreview(musicInfo: MusicInfo): String {
        val response = HttpUtil.get {
            url = "https://music.163.com/song?id=" + musicInfo.mid
            cookies = { -cookie }
        }.response
        val prefix = "f-fl\">\n<img src=\""
        return response.substring(response.indexOf(prefix) + prefix.length, response.indexOf("\" class=\"j-img\""))
    }

    override fun download(musicInfo: MusicInfo): String? {
        for (br in brArray) {
            val json = HttpUtil.get {
                url = serverHost + java.lang.String.format(musicDownloadUrl, musicInfo.mid, br)
                cookies = { -cookie }
            }.getJSONResponse()
            json?.let {
                val downloadUrl = json.getJSONObject("data").getString("url") ?: return@let
                val fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"))
                val downloadSuccess = HttpUtil.downloadFile(downloadUrl, baseMusicService.musicPath + fileName)
                return if (downloadSuccess) fileName else null
            }
        }
        val downloadUrl = musicInfo.musicUrl
        val fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"))
        val downloadSuccess = HttpUtil.downloadFile(downloadUrl, baseMusicService.musicPath + fileName)
        return if (downloadSuccess) fileName else null
    }

    private fun refreshCookie() {
        HttpUtil.get {
            this.url = serverHost + loginRefreshUrl
            cookies = { -cookie }
        }.getJSONResponse()?.let {
            login()
        }
    }

}