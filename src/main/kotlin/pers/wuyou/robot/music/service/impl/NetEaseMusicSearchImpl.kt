package pers.wuyou.robot.music.service.impl

import com.alibaba.fastjson2.JSONObject
import com.sun.deploy.util.URLUtil
import org.ktorm.entity.Entity
import org.springframework.stereotype.Service
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.util.HttpUtil
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
class NetEaseMusicSearchImpl(musicProperties: MusicProperties, baseMusicService: BaseMusicService) :
    MusicSearchService {
    private final val uin: String?
    private final val pwd: String?
    private final val serverHost: String?
    private final val baseMusicService: BaseMusicService

    init {
        this.uin = musicProperties.netEase.account
        this.pwd = musicProperties.netEase.password
        this.serverHost = musicProperties.netEase.serverHost
        this.baseMusicService = baseMusicService
    }

    override fun login(): Boolean {
        NET_EASE_MUSIC_COOKIE.clear()
        val requestEntity: HttpUtil.ResponseEntity = HttpUtil.get(serverHost + String.format(LOGIN_URL, uin, pwd))
        NET_EASE_MUSIC_COOKIE.putAll(requestEntity.cookies)
        val json: JSONObject = requestEntity.getJSONResponse()!!
        val code = json["code"]
        if (code == SUCCESS_CODE) {
            return true
        }
        logger { "NetEase login fail." }
        logger { json.toJSONString() }
        return false
    }

    override fun search(name: String): List<MusicInfo> {
        val json: JSONObject = get(
            serverHost + java.lang.String.format(
                SEARCH_URL,
                URLUtil.encodePath(name.trim { it <= ' ' })
            )
        ).getJSONResponse()!!
        val result = json.getJSONObject("result")
        val jsonArray = result.getJSONArray("songs")
        val list: MutableList<MusicInfo> = ArrayList<MusicInfo>()
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
            val music: JSONObject = get(serverHost + String.format(MUSIC_PLAY_URL, mid)).getJSONResponse()!!
            val musicUrl = music.getJSONArray("data").getJSONObject(0).getString("url")?:""
            val jumpUrl = String.format(MUSIC_JUMP_URL, mid)
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
        val response: String = get("https://music.163.com/song?id=" + musicInfo.mid).response
        val prefix = "f-fl\">\n<img src=\""
        return response.substring(response.indexOf(prefix) + prefix.length, response.indexOf("\" class=\"j-img\""))
    }

    override fun download(musicInfo: MusicInfo): String? {
        for (br in BR_ARRAY) {
            val url = serverHost + java.lang.String.format(MUSIC_DOWNLOAD_URL, musicInfo.mid, br)
            val json: JSONObject? = get(url).getJSONResponse()
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

    private operator fun get(url: String): HttpUtil.ResponseEntity {
        val a = HttpUtil.get {
            this.url = serverHost + LOGIN_REFRESH_URL
            cookies = { -NET_EASE_MUSIC_COOKIE }
        }
        val json: JSONObject? =
            a.getJSONResponse()
        if (json == null) {
            login()
        }
        return HttpUtil.get {
            this.url = url
            cookies = { -NET_EASE_MUSIC_COOKIE }
        }
    }

    companion object {
        private const val LOGIN_URL = "login/cellphone?phone=%s&password=%s"
        private const val LOGIN_REFRESH_URL = "login/refresh"
        private const val SEARCH_URL = "cloudsearch?keywords=%s&limit=10"
        private const val MUSIC_PLAY_URL = "song/url?id=%s"
        private const val MUSIC_DOWNLOAD_URL = "song/download/url?id=%s&br=%s"
        private const val MUSIC_JUMP_URL = "https://music.163.com/#/song?id=%s"
        private val BR_ARRAY = arrayOf(1411000, 999000, 320000, 128000)
        private val NET_EASE_MUSIC_COOKIE: MutableMap<String, String> = HashMap()
        private const val SUCCESS_CODE = 200
    }
}