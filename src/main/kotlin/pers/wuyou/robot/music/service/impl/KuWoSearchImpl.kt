package pers.wuyou.robot.music.service.impl

import com.alibaba.fastjson2.JSONObject
import org.apache.http.HttpHeaders
import org.ktorm.entity.Entity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import pers.wuyou.robot.core.common.Constant
import pers.wuyou.robot.core.util.HttpUtil
import pers.wuyou.robot.core.util.ResponseEntity
import pers.wuyou.robot.music.entity.MusicInfo
import pers.wuyou.robot.music.service.BaseMusicService
import pers.wuyou.robot.music.service.MusicSearchService

/**
 * 酷我音乐实现
 *
 * @author wuyou
 */
@Service("KuWoSearchImpl")
@Component
class KuWoSearchImpl(val baseMusicService: BaseMusicService) : MusicSearchService {

    private val getTokenUrl = "https://www.kuwo.cn/search/list?key=%s"
    private val searchUrl = "https://www.kuwo.cn/api/www/search/searchMusicBykeyWord?key=%s&pn=1&rn=10&httpsStatus=1"
    private val musicPlayUrl =
        "http://antiserver.kuwo.cn/anti.s?rid=%s&response=res&format=mp3%%7Caac&type=convert_url&br=128kmp3&agent=iPhone&callback=getlink&jpcallback=getlink.mp3"
    private val musicDownloadUrl =
        "http://antiserver.kuwo.cn/anti.s?rid=%s&response=res&format=mp3%%7Caac&type=convert_url&br=320kmp3&agent=iPhone&callback=getlink&jpcallback=getlink.mp3"
    private val musicJumpUrl = "https://www.kuwo.cn/play_detail/%s"
    private val cookie: MutableMap<String, String> = HashMap()

    override fun login(): Boolean {
        return true
    }

    @Suppress("DuplicatedCode")
    override fun search(name: String): List<MusicInfo> {
        cookie.clear()
        val tokenUrl = String.format(getTokenUrl, name.trim())
        val requestEntity: ResponseEntity = HttpUtil.get(tokenUrl)
        cookie.putAll(requestEntity.cookies)
        val searchUrl = String.format(searchUrl, name.trim())
        val jsonResponse: JSONObject = HttpUtil.get {
            url = searchUrl
            cookies = { -cookie }
            header = {
                "csrf" - (cookie["kw_token"] ?: "")
                HttpHeaders.REFERER - tokenUrl
            }
        }.getJSONResponse()!!
        val code = jsonResponse.getInteger("code")
        val list: MutableList<MusicInfo> = ArrayList()
        if (code != Constant.SUCCESS_CODE) {
            return list
        }
        val jsonArray = jsonResponse.getJSONObject("data").getJSONArray("list")
        for (i in jsonArray.indices) {
            val jsonObject = jsonArray.getJSONObject(i)
            val mid = jsonObject.getString("musicrid")
            val title = jsonObject.getString("name")
            val artist = jsonObject.getString("artist")
            val previewUrl = jsonObject.getString("albumpic")
            val album = jsonObject.getString("album")
            val payPlay = "1111" == jsonObject.getJSONObject("payInfo").getString("play")
            val musicUrl = String.format(musicPlayUrl, mid)
            val jumpUrl = String.format(musicJumpUrl, mid.substring(6))
            val musicInfo = Entity.create<MusicInfo>().also {
                it.mid = mid
                it.artist = artist
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
        return musicInfo.previewUrl
    }

    override fun download(musicInfo: MusicInfo): String? {
        val downloadUrl = java.lang.String.format(musicDownloadUrl, musicInfo.mid)
        val fileName: String = musicInfo.mid + ".mp3"
        val downloadSuccess = HttpUtil.downloadFile(downloadUrl, baseMusicService.musicPath + fileName)
        return if (downloadSuccess) fileName else null
    }

}