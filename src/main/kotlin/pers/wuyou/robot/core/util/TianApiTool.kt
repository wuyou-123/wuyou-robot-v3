package pers.wuyou.robot.core.util

import com.alibaba.fastjson2.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.common.Constant
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.util.HttpUtil.get
import java.net.URLEncoder

/**
 * 天行api工具
 *
 * @author wuyou
 * @date 2022/3/15 18:36
 */
@Suppress("unused")
@Component
class TianApiTool {
    @Value("\${robot.tianapi-key}")
    private val tianapiKey: String? = null

    private val robotApiUrl = "https://api.tianapi.com/robot/index?key=%s&uniqueid=%s&question=%s"
    private val ipDetailApiUrl = "https://api.tianapi.com/ipquery/index?key=%s&ip=%s"
    private val successCode = Constant.SUCCESS_CODE
    private val code = Constant.CODE
    fun chatApi(msg: String, uin: String?): String {
        logger { "请求聊天$uin-$msg" }
        val response = get(String.format(robotApiUrl,
            tianapiKey,
            uin,
            URLEncoder.encode(msg.trim(), "utf-8"))).getJSONResponse()
        return when (successCode) {
            response!!.getInteger(code) -> {
                response.getJSONArray("newslist").getJSONObject(0).getString("reply").trim().also {
                    logger { "聊天 $msg 返回$it" }
                }
            }
            else -> ""
        }
    }

    fun getIpDetailApi(ip: String?): JSONObject? {
        val response = get(String.format(ipDetailApiUrl, tianapiKey, ip)).getJSONResponse()
        return if (response!!.getInteger(code) == successCode) {
            response.getJSONArray("newslist").getJSONObject(0)
        } else null
    }
}