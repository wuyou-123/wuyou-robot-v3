package pers.wuyou.robot.music.service.impl

import com.alibaba.fastjson2.JSONObject
import org.ktorm.entity.Entity
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.logging.LogLevel
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.core.common.Sender
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.exception.ResourceNotFoundException
import pers.wuyou.robot.core.util.CommandUtil
import pers.wuyou.robot.core.util.FileUtil
import pers.wuyou.robot.core.util.HttpUtil
import pers.wuyou.robot.core.util.MessageUtil
import pers.wuyou.robot.music.config.MusicProperties
import pers.wuyou.robot.music.entity.MusicInfo
import pers.wuyou.robot.music.service.BaseMusicService
import pers.wuyou.robot.music.service.MusicSearchService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * QQ音乐实现
 *
 * @author wuyou
 */
@Suppress("SpellCheckingInspection")
@Service("QQMusicSearchImpl")
@Configuration
@EnableConfigurationProperties(
    MusicProperties::class
)
class QQMusicSearchImpl(musicProperties: MusicProperties, baseMusicService: BaseMusicService) : MusicSearchService {
    private val uin: String?
    private val pwd: String?
    private val baseMusicService: BaseMusicService
    private var isWaitScan = false

    init {
        uin = musicProperties.tencent.account
        pwd = musicProperties.tencent.password
        this.baseMusicService = baseMusicService
    }

    override fun search(name: String): List<MusicInfo> {
        val json: JSONObject = HttpUtil.get {
            url = MUSIC_U_FCG
            params = { "data" - String.format(SEARCH_URL, name.trim { it <= ' ' }) }
            cookies = { -COOKIE }
        }.getJSONResponse() ?: return emptyList()
        val jsonArray = json.getJSONObject("req").getJSONObject("data").getJSONObject("body").getJSONObject("song")
            .getJSONArray("list")
        val list: MutableList<MusicInfo> = ArrayList<MusicInfo>()
        for (i in jsonArray.indices) {
            val jsonObject = jsonArray.getJSONObject(i)
            val mid = jsonObject.getString("mid")
            val title = jsonObject.getString("name")
            val subtitle = jsonObject.getString("subtitle")
            val album = jsonObject.getJSONObject("album").getString("name")
            val artistsJson = jsonObject.getJSONArray("singer")
            val artistList: List<String> = artistsJson.map {
                (it as JSONObject).getString("name")
            }
            val artists = java.lang.String.join("&", artistList)
            val payPlay: Boolean = jsonObject.getJSONObject("pay").getBooleanValue("pay_play")
            val data: MutableMap<String, String> = HashMap(2)
            data["data"] = String.format(MUSIC_JSON, mid)
            val purl = getPurl(data)
            if (purl.isEmpty() && cookieIsExpires()) {
                logger(LogLevel.WARN) { "cookie已过期, 重新获取cookie" }
                val qqAuthSuccess = login()
                return if (qqAuthSuccess) {
                    search(name)
                } else {
                    logger(LogLevel.WARN) { "登录失败,返回空集合" }
                    emptyList()
                }
            }
            val musicUrl = String.format(MUSIC_PLAY_URL, purl)
            val jumpUrl = String.format(MUSIC_JUMP_URL, mid)
            val musicInfo = Entity.create<MusicInfo>().also {
                it.mid = mid
                it.artist = artists
                it.album = album
                it.title = title
                it.subtitle = subtitle
                it.jumpUrl = jumpUrl
                it.musicUrl = musicUrl
                it.payPlay = payPlay
            }
            list.add(musicInfo)
        }
        return list
    }

    override fun getPreview(musicInfo: MusicInfo): String {
        return java.lang.String.format(
            "https:%s",
            HttpUtil.getJson(musicInfo.jumpUrl, "__INITIAL_DATA__").getJSONObject("detail").getString("picurl")
        )
    }

    private fun getPurl(data: Map<String, String>): String {
        return HttpUtil.get {
            url = MUSIC_U_FCG
            params = { -data }
            cookies = { -COOKIE }
        }.getJSONResponse()?.getJSONObject("req")
            ?.getJSONObject("data")?.getJSONArray("midurlinfo")?.getJSONObject(0)?.getString("purl") ?: ""
    }

    override fun download(musicInfo: MusicInfo): String? {
        val musicUrl: String = musicInfo.musicUrl
        val name = musicUrl.substring(
            musicUrl.indexOf(Br.M4A.prefix) + Br.M4A.prefix.length,
            musicUrl.indexOf(Br.M4A.suffix)
        )
        val data: MutableMap<String, String> = HashMap(2)
        for (i in Br.values().indices) {
            val br = Br.values()[i]
            val fileName: String = br.prefix + name + br.suffix
            data["data"] = java.lang.String.format(MUSIC_DOWNLOAD_JSON, fileName, musicInfo.mid)
            val purl = getPurl(data)
            if (purl.isEmpty()) {
                continue
            }
            val downloadUrl = String.format(MUSIC_PLAY_URL, purl)
            val downloadSuccess = HttpUtil.downloadFile(downloadUrl, baseMusicService.musicPath + fileName)
            return if (downloadSuccess) fileName else null
        }
        return null
    }

    private fun cookieIsExpires(): Boolean {
        if (COOKIE[NOW_TIME] == null) {
            return true
        }
        val time = COOKIE[NOW_TIME]!!.toLong()
        return System.currentTimeMillis() - time > TimeUnit.HOURS.toMillis(10)
    }

    override fun login(): Boolean {
        COOKIE.clear()
        val responseEntity: HttpUtil.ResponseEntity = HttpUtil.get(String.format(CHECK, uin))
        val response: String = responseEntity.response
        COOKIE.putAll(responseEntity.cookies)
        val resultArray = getResultArray(response)
        val list: List<String> = CommandUtil.exec(
            "node",
            JS_FILE_PATH,
            uin,
            pwd,
            resultArray[1],
            resultArray[3],
            resultArray[5],
            resultArray[6]
        )
        if (list.isEmpty()) {
            logger(LogLevel.WARN) { "js执行失败,请检查是否安装了node环境" }
            return false
        }
        logger { "js执行成功." }
        val url = list[0]
        val responseEntity1: HttpUtil.ResponseEntity = HttpUtil.get(url)
        val response1: String = responseEntity1.response
        COOKIE.putAll(responseEntity1.cookies)
        val resultArray1 = getResultArray(response1)
        logger { resultArray1.contentToString() }
        // 登录成功返回code
        val successCode = "0"
        // 扫描登录返回code
        val scanCode = "10005"
        val retry = "7"
        return when (resultArray1[0]) {
            successCode -> getCookies(resultArray1[2])
            scanCode, retry -> scanLogin()
            else -> {
                logger(LogLevel.WARN) { "qq music login fail!" }
                false
            }
        }
    }

    private fun getCookies(s: String): Boolean {
        var cookies: MutableMap<String, String> = HttpUtil.get(s).cookies.toMutableMap()
        COOKIE.putAll(cookies)
        val list1: List<String> =
            CommandUtil.exec("python", PY_FILE_PATH, COOKIE["p_uin"], COOKIE["p_skey"], COOKIE["pt_oauth_token"])
        if (list1.isEmpty()) {
            logger(LogLevel.WARN) { "python执行失败,请检查是否安装了python环境" }
            return false
        }
        logger { "python执行成功." }
        val url2 = list1[0]
        val code = url2.substring(url2.indexOf("code=") + 5)
        if (code.isNotEmpty()) {
            val json = String.format(MUSIC_JSON2, code, gtk)
            val responseEntity2: HttpUtil.ResponseEntity = HttpUtil.post {
                url = MUSIC_U_FCG
                this.json = json
                cookies = COOKIE
            }
            val cookies1: Map<String, String> = responseEntity2.cookies
            if (cookies.isEmpty()) {
                logger(LogLevel.WARN) { "qq music login fail, Cookie is empty!" }
                return false
            }
            COOKIE.putAll(cookies1)
            COOKIE[NOW_TIME] = System.currentTimeMillis().toString() + ""
            return true
        }
        logger(LogLevel.WARN) { "qq music login fail, Code is empty!" }
        return false
    }

    private fun scanLogin(): Boolean {
        if (isWaitScan) {
            return false
        }
        COOKIE.clear()
        val responseEntity: HttpUtil.ResponseEntity = HttpUtil.get(String.format(CHECK, uin))
        COOKIE.putAll(responseEntity.cookies)
        val path = loginQrCode
        Sender.sendPrivateMsg(RobotCore.ADMINISTRATOR[0], MessageUtil.getImageMessage(path))
        isWaitScan = true
        try {
            while (true) {
                Thread.sleep(1000)
                val response: String = loginState.response
                if (response.contains("ptuiCB('0'")) {
                    val url = Arrays.stream(response.split("'".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()).filter { i: String -> i.contains("http") }
                        .collect(Collectors.toList())[0]
                    val responseEntity1: HttpUtil.ResponseEntity = HttpUtil.get(url)
                    COOKIE.putAll(responseEntity1.cookies)
                    COOKIE[NOW_TIME] = System.currentTimeMillis().toString() + ""
                    logger { "scan login success." }
                    isWaitScan = false
                    return true
                } else if (response.contains("ptuiCB('65'")) {
                    val imagePath = loginQrCode
                    Sender.sendPrivateMsg(RobotCore.ADMINISTRATOR[0], MessageUtil.getImageMessage(imagePath))
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 支持的几种音乐格式, 用来获取前缀和后缀
     */
    private enum class Br(val prefix: String, val end: String) {
        /**
         * flac格式
         */
        FLAC("F00000", "flac"),

        /**
         * mp3格式
         */
        MP3("M80000", "mp3"),

        /**
         * m4a格式
         */
        M4A("C40000", "m4a");

        val suffix: String = "." + this.end
    }

    companion object {
        private const val SEARCH_URL =
            "{\"req\":{\"module\":\"music.search.SearchCgiService\",\"method\":\"DoSearchForQQMusicDesktop\",\"param\":{\"search_type\":0,\"query\":\"%s\",\"page_num\":1,\"num_per_page\":10}},\"comm\":{\"uin\":0,\"format\":\"json\",\"cv\":0}}"
        private const val CHECK =
            "https://ssl.ptlogin2.qq.com/check?regmaster=&pt_tea=2&pt_vcode=1&uin=%s&appid=716027609&js_ver=21122814&js_type=1&login_sig=u1cFxLxCIZyhQiuufGpUqedhK9g9VlQWIXW1ybpCg-G0-q9wd0mdzw3R9vNHFz2S&u1=https://graph.qq.com/oauth2.0/login_jump&r=0.004892586794276843&pt_uistyle=40"
        private const val MUSIC_U_FCG = "https://u.y.qq.com/cgi-bin/musicu.fcg"
        private const val MUSIC_JSON =
            "{\"req\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"g\",\"songmid\":[\"%s\"],\"songtype\":[0],\"uin\":\"0\",\"loginflag\":1,\"platform\":\"20\"}},\"comm\":{\"uin\":0,\"format\":\"json\",\"ct\":24,\"cv\":0}}"
        private const val MUSIC_JSON2 =
            "{\"req\":{\"module\":\"QQConnectLogin.LoginServer\",\"method\":\"QQLogin\",\"param\":{\"code\":\"%s\"}},\"comm\":{\"g_tk\":%s,\"platform\":\"yqq\",\"ct\":24,\"cv\":0}}"
        private const val MUSIC_DOWNLOAD_JSON =
            "{\"req\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"g\",\"filename\":[\"%s\"],\"songmid\":[\"%s\"],\"platform\":\"20\"}},\"comm\":{\"uin\":0,\"format\":\"json\",\"cv\":0}}"
        private const val MUSIC_PLAY_URL = "https://dl.stream.qqmusic.qq.com/%s"
        private const val MUSIC_JUMP_URL = "https://y.qq.com/n/ryqq/songDetail/%s"
        private val COOKIE: MutableMap<String, String> = HashMap()
        private const val JS_FILE_NAME = "getQQMusicAuth.js"
        private const val PY_FILE_NAME = "getQQMusicAuth.py"
        private var JS_FILE_PATH: String? = null
        private var PY_FILE_PATH: String? = null
        private const val NOW_TIME = "now_time"

        init {
            try {
                val inputStream =
                    QQMusicSearchImpl::class.java.classLoader.getResourceAsStream(BaseMusicService.TYPE_NAME + File.separator + JS_FILE_NAME)
                val inputStream1 =
                    QQMusicSearchImpl::class.java.classLoader.getResourceAsStream(BaseMusicService.TYPE_NAME + File.separator + PY_FILE_NAME)
                if (inputStream == null || inputStream1 == null) {
                    throw ResourceNotFoundException()
                }
                JS_FILE_PATH = FileUtil.saveTempFile(
                    inputStream,
                    JS_FILE_NAME,
                    BaseMusicService.TYPE_NAME
                )
                PY_FILE_PATH = FileUtil.saveTempFile(
                    inputStream1,
                    PY_FILE_NAME,
                    BaseMusicService.TYPE_NAME
                )
            } catch (e: Exception) {
                e.printStackTrace()
                throw ResourceNotFoundException()
            }
        }

        private val gtk: String
            get() {
                val sKey = COOKIE["p_skey"]
                var hash = 5381
                var i = 0
                val len = sKey!!.length
                while (i < len) {
                    hash += (hash shl 5) + sKey[i].code
                    ++i
                }
                return (hash and 0x7fffffff).toString()
            }

        private fun getResultArray(response: String): Array<String> {
            val result = response.replace("'", "")
            return result.substring(response.indexOf("(") + 1, result.length - 1).split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        }

        /**
         * 初始化cookie
         */
        private val scanCookies: Unit
            get() {
                val url =
                    "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?appid=716027609&target=self&style=40&s_url=https://y.qq.com/"
                val responseEntity: HttpUtil.ResponseEntity = HttpUtil.get(url)
                responseEntity.cookies.let { COOKIE.putAll(it) }
            }

        /**
         * 获取二维码
         *
         * @return 二维码图片路径
         */
        private val loginQrCode: String
            get() {
                val now = System.currentTimeMillis().toString() + ""
                scanCookies
                val url1 = String.format(
                    "https://ssl.ptlogin2.qq.com/ptqrshow?appid=716027609&e=2&l=M&s=3&d=72&v=4&t=%s&pt_3rd_aid=0",
                    now
                )
                val responseEntity: HttpUtil.ResponseEntity = HttpUtil.get(url1)
                COOKIE.putAll(responseEntity.cookies)
                COOKIE["key"] = now
                val bytes: ByteArray = responseEntity.entity
                try {
                    FileOutputStream(RobotCore.TEMP_PATH + now).use { fileOutputStream -> fileOutputStream.write(bytes) }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                return RobotCore.TEMP_PATH + now
            }

        /**
         * 获取登录状态
         *
         * @return 返回的登录实体
         */
        private val loginState: HttpUtil.ResponseEntity
            get() {
                val map: Map<String, String?> = COOKIE
                val urlCheckTimeout =
                    ("https://ssl.ptlogin2.qq.com/ptqrlogin?u1=https://y.qq.com/&ptqrtoken=" + getPtqrtoken(
                        map["qrsig"]
                    )
                            + "&ptredirect=0&h=1&t=1&g=1&from_ui=1&ptlang=2052&action=0-0-" + map["key"]
                            + "&js_ver=10233&js_type=1&login_sig=" + map["pt_login_sig"] + "&pt_uistyle=40&aid=716027609&")
                return HttpUtil.get {
                    url = urlCheckTimeout
                    cookies = { -map }
                }
            }

        /**
         * 计算qrsig
         *
         * @return 计算后的结果
         */
        private fun getPtqrtoken(qrsig: String?): Int {
            var e = 0
            val n = qrsig!!.length
            for (j in 0 until n) {
                e += (e shl 5)
                e += qrsig.toCharArray()[j].code
                e = 2147483647 and e
            }
            return e
        }
    }
}