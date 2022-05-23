package pers.wuyou.robot.entertainment.listener

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.delay
import love.forte.simboot.annotation.Filter
import love.forte.simbot.ID
import love.forte.simbot.component.mirai.message.MiraiShare
import love.forte.simbot.event.GroupMessageEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.annotation.RobotListen
import pers.wuyou.robot.core.common.Sender
import pers.wuyou.robot.core.util.TianApiTool
import java.util.*

/**
 * @author wuyou
 * @date 2022/2/19 0:06
 */
@Component
class LookListener(private val tianApiTool: TianApiTool) {
    @Value("\${robot.ip-host}")
    private val host: String = ""

    private val LOOK_MAP: MutableMap<ID, TreeSet<String>> = HashMap()
    private val IP_MAP: MutableMap<String, String> = HashMap()
    private val IPV4_LENGTH = 4
    fun addIp(ip: String, group: ID) {
        if (LOOK_MAP[group] == null) {
            return
        }
        LOOK_MAP[group]?.add(ip)
        val ipDetail = getIpDetail(ip)
        IP_MAP[ip] = ipDetail
    }

    @Suppress("OPT_IN_USAGE")
    @RobotListen(desc = "窥屏检测", isBoot = true)
    @Filter("窥屏检测")
    suspend fun GroupMessageEvent.look() {
        if (LOOK_MAP.containsKey(group().id)) {
            return
        }
        LOOK_MAP[group().id] = TreeSet()
//        val msg = Entity.create<MusicInfo>().also {
//            it.title = "窥屏检测中..."
//            it.artist = "请稍后..."
//            it.jumpUrl = host
//            it.musicUrl = host
//            it.previewUrl = "${host}look?group=${group().id}&amp;time=$now"
//        }.getMusicShare()
        val msg = MiraiShare("", "窥屏检测中...", "电脑端窥屏暂时无法检测...", "${host}look?group=${group().id}")
        Sender.send(this, msg)
        delay(10000L)
        val list = ArrayList<String>()
        LOOK_MAP[group().id]?.let {
            list.add("检测结束, 有${it.size}人窥屏")
            it.forEach { ip ->
                list.add(getIpDetail(ip))
            }
        }
        if (list.size > 0) {
            Sender.send(this, list, "\n")
        }
        LOOK_MAP.remove(group().id)

    }

    private fun getIpDetail(ip: String): String {
        val newslist: JSONObject = tianApiTool.getIpDetailApi(ip) ?: return ""
        val province: String = newslist.getString("province")
        val city: String = newslist.getString("city")
        return "${encryptIp(ip)}  ${if (province == city) province else "$province  $city"}\n"
    }

    private fun encryptIp(ip: String): String {
        val split = ip.split(".")
        return if (split.size == IPV4_LENGTH) listOf(split[0], split[1], "xxx", "xxx").joinToString(".")
        else ip
    }
}