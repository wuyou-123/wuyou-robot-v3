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
import pers.wuyou.robot.core.util.*
import pers.wuyou.robot.music.config.MusicProperties
import pers.wuyou.robot.music.entity.MusicInfo
import pers.wuyou.robot.music.service.BaseMusicService
import pers.wuyou.robot.music.service.MusicSearchService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * QQ音乐实现
 *
 * @author wuyou
 */
@Suppress("SpellCheckingInspection")
@Service("QQMusicSearchImpl")
@Configuration
@EnableConfigurationProperties(MusicProperties::class)
class QQMusicSearchImpl(musicProperties: MusicProperties, private val baseMusicService: BaseMusicService) :
    MusicSearchService {
    private val uin: String?
    private val pwd: String?
    private var isWaitScan = false


    private val getQrUrl =
        "https://ssl.ptlogin2.qq.com/ptqrshow?appid=716027609&e=2&l=M&s=3&d=72&v=4&t=%s&pt_3rd_aid=0"
    private val initCookieUrl =
        "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?appid=716027609&target=self&style=40&s_url=https://y.qq.com/"
    private val checkQrUrl =
        "https://ssl.ptlogin2.qq.com/ptqrlogin?u1=https://y.qq.com/&ptqrtoken=%s&ptredirect=0&h=1&t=1&g=1&from_ui=1&ptlang=2052&action=0-0-%s&js_ver=10233&js_type=1&login_sig=%s&pt_uistyle=40&aid=716027609&"
    private val searchUrl =
        "{\"req\":{\"module\":\"music.search.SearchCgiService\",\"method\":\"DoSearchForQQMusicDesktop\",\"param\":{\"search_type\":0,\"query\":\"%s\",\"page_num\":1,\"num_per_page\":10}},\"comm\":{\"uin\":0,\"format\":\"json\",\"cv\":0}}"
    private val check =
        "https://ssl.ptlogin2.qq.com/check?regmaster=&pt_tea=2&pt_vcode=1&uin=%s&appid=716027609&js_ver=21122814&js_type=1&login_sig=u1cFxLxCIZyhQiuufGpUqedhK9g9VlQWIXW1ybpCg-G0-q9wd0mdzw3R9vNHFz2S&u1=https://graph.qq.com/oauth2.0/login_jump&r=0.004892586794276843&pt_uistyle=40"
    private val musicUFcg = "https://u.y.qq.com/cgi-bin/musicu.fcg"
    private val musicJson =
        "{\"req\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"g\",\"songmid\":[\"%s\"],\"songtype\":[0],\"uin\":\"0\",\"loginflag\":1,\"platform\":\"20\"}},\"comm\":{\"uin\":0,\"format\":\"json\",\"ct\":24,\"cv\":0}}"
    private val musicJson2 =
        "{\"req\":{\"module\":\"QQConnectLogin.LoginServer\",\"method\":\"QQLogin\",\"param\":{\"code\":\"%s\"}},\"comm\":{\"g_tk\":%s,\"platform\":\"yqq\",\"ct\":24,\"cv\":0}}"
    private val musicDownloadJson =
        "{\"req\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"g\",\"filename\":[\"%s\"],\"songmid\":[\"%s\"],\"platform\":\"20\"}},\"comm\":{\"uin\":0,\"format\":\"json\",\"cv\":0}}"
    private val musicPlayUrl = "https://dl.stream.qqmusic.qq.com/%s"
    private val musicJumpUrl = "https://y.qq.com/n/ryqq/songDetail/%s"
    private val cookie: MutableMap<String, String> = HashMap()
    private val jsFileName = "getQQMusicAuth.js"
    private val pyFileName = "getQQMusicAuth.py"
    private var jsFilePath: String? = null
    private var pyFilePath: String? = null
    private val nowTime = "now_time"

    init {
        this.uin = musicProperties.tencent.account
        this.pwd = musicProperties.tencent.password

        try {
            val inputStream =
                QQMusicSearchImpl::class.java.classLoader.getResourceAsStream(BaseMusicService.TYPE_NAME + File.separator + jsFileName)
            val inputStream1 =
                QQMusicSearchImpl::class.java.classLoader.getResourceAsStream(BaseMusicService.TYPE_NAME + File.separator + pyFileName)
            if (inputStream == null || inputStream1 == null) {
                throw ResourceNotFoundException()
            }
            jsFilePath = FileUtil.saveTempFile(inputStream, jsFileName, BaseMusicService.TYPE_NAME)
            pyFilePath = FileUtil.saveTempFile(inputStream1, pyFileName, BaseMusicService.TYPE_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
            throw ResourceNotFoundException()
        }
    }

    @Suppress("DuplicatedCode")
    override fun search(name: String): List<MusicInfo> {
        val json: JSONObject = HttpUtil.get {
            url = musicUFcg
            params = { "data" - String.format(searchUrl, name.trim { it <= ' ' }) }
            cookies = { -cookie }
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
            data["data"] = String.format(musicJson, mid)
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
            val musicUrl = String.format(musicPlayUrl, purl)
            val jumpUrl = String.format(musicJumpUrl, mid)
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
        return java.lang.String.format("https:%s",
            HttpUtil.getJson(musicInfo.jumpUrl, "__INITIAL_DATA__").getJSONObject("detail").getString("picurl"))
    }

    private fun getPurl(data: Map<String, String>): String {
        return HttpUtil.get {
            url = musicUFcg
            params = { -data }
            cookies = { -cookie }
        }.getJSONResponse()?.getJSONObject("req")?.getJSONObject("data")?.getJSONArray("midurlinfo")?.getJSONObject(0)
            ?.getString("purl") ?: ""
    }

    override fun download(musicInfo: MusicInfo): String? {
        val musicUrl: String = musicInfo.musicUrl
        val name =
            musicUrl.substring(musicUrl.indexOf(Br.M4A.prefix) + Br.M4A.prefix.length, musicUrl.indexOf(Br.M4A.suffix))
        val data: MutableMap<String, String> = HashMap(2)
        for (i in Br.values().indices) {
            val br = Br.values()[i]
            val fileName: String = br.prefix + name + br.suffix
            data["data"] = java.lang.String.format(musicDownloadJson, fileName, musicInfo.mid)
            val purl = getPurl(data)
            if (purl.isEmpty()) {
                continue
            }
            val downloadUrl = String.format(musicPlayUrl, purl)
            val downloadSuccess = HttpUtil.downloadFile(downloadUrl, baseMusicService.musicPath + fileName)
            return if (downloadSuccess) fileName else null
        }
        return null
    }

    private fun cookieIsExpires(): Boolean {
        if (cookie[nowTime] == null) {
            return true
        }
        val time = cookie[nowTime]!!.toLong()
        return System.currentTimeMillis() - time > TimeUnit.HOURS.toMillis(10)
    }

    override fun login(): Boolean {
        cookie.clear()
        val responseEntity: ResponseEntity = HttpUtil.get(String.format(check, uin))
        val response: String = responseEntity.response
        cookie.putAll(responseEntity.cookies)
        val resultArray = getResultArray(response)
        val list: List<String> = CommandUtil.exec("node",
            jsFilePath,
            uin,
            pwd,
            resultArray[1],
            resultArray[3],
            resultArray[5],
            resultArray[6])
        if (list.isEmpty()) {
            logger(LogLevel.WARN) { "js执行失败,请检查是否安装了node环境" }
            return false
        }
        logger { "js执行成功." }
        val url = list[0]
        val responseEntity1: ResponseEntity = HttpUtil.get(url)
        val response1: String = responseEntity1.response
        cookie.putAll(responseEntity1.cookies)
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
        cookie.putAll(cookies)
        val list1: List<String> =
            CommandUtil.exec("python", pyFilePath, cookie["p_uin"], cookie["p_skey"], cookie["pt_oauth_token"])
        if (list1.isEmpty()) {
            logger(LogLevel.WARN) { "python执行失败,请检查是否安装了python环境" }
            return false
        }
        logger { "python执行成功." }
        val url2 = list1[0]
        val code = url2.substring(url2.indexOf("code=") + 5)
        if (code.isNotEmpty()) {
            val json = String.format(musicJson2, code, gtk())
            val responseEntity2: ResponseEntity = HttpUtil.post {
                url = musicUFcg
                this.json = json
                cookies = cookie
            }
            val cookies1: Map<String, String> = responseEntity2.cookies
            if (cookies.isEmpty()) {
                logger(LogLevel.WARN) { "qq music login fail, Cookie is empty!" }
                return false
            }
            cookie.putAll(cookies1)
            cookie[nowTime] = System.currentTimeMillis().toString() + ""
            return true
        }
        logger(LogLevel.WARN) { "qq music login fail, Code is empty!" }
        return false
    }

    private fun scanLogin(): Boolean {
        if (isWaitScan) {
            return false
        }
        cookie.clear()
        cookie.putAll(HttpUtil.get(String.format(check, uin)).cookies)
        val path = loginQrCode()
        Sender.sendPrivateMsg(RobotCore.ADMINISTRATOR[0], MessageUtil.getImageMessage(path))
        isWaitScan = true
        try {
            while (true) {
                Thread.sleep(1000)
                val response: String = getLoginState().response
                if (response.contains("ptuiCB('0'")) {
                    val url = response.split("'".toRegex()).find { it.contains("http") }!!
                    val responseEntity: ResponseEntity = HttpUtil.get(url)
                    cookie.putAll(responseEntity.cookies)
                    cookie[nowTime] = System.currentTimeMillis().toString() + ""
                    logger { "scan login success." }
                    isWaitScan = false
                    return true
                } else if (response.contains("ptuiCB('65'")) {
                    val imagePath = loginQrCode()
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

    private fun gtk(): String {
        val sKey = cookie["p_skey"]
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
            .dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    /**
     * 初始化cookie
     */
    private fun initScanCookies() {
        val responseEntity: ResponseEntity = HttpUtil.get(initCookieUrl)
        cookie.putAll(responseEntity.cookies)
    }

    /**
     * 获取二维码
     *
     * @return 二维码图片路径
     */
    private fun loginQrCode(): String {
        val now = System.currentTimeMillis().toString() + ""
        initScanCookies()
        val responseEntity: ResponseEntity = HttpUtil.get(String.format(getQrUrl, now))
        cookie.putAll(responseEntity.cookies)
        cookie["key"] = now
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
    private fun getLoginState(): ResponseEntity {
        val urlCheckTimeout =
            String.format(checkQrUrl, getPtqrtoken(cookie["qrsig"]), cookie["key"], cookie["pt_login_sig"])
        return HttpUtil.get {
            url = urlCheckTimeout
            cookies = { -cookie }
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
}