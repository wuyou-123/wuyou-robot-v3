package pers.wuyou.robot.entertainment.controller

import love.forte.simbot.ID
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.entertainment.listener.LookListener
import java.net.URL
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author wuyou
 * @date 2022/2/19 0:23
 */
@Controller
class LookController(private val lookListener: LookListener) {
    @GetMapping("/look")
    fun getPic(req: HttpServletRequest, resp: HttpServletResponse, group: String = "") {
        val headerNames = req.headerNames
        logger { "header-start ----" }
        while (headerNames.hasMoreElements()) {
            val element = headerNames.nextElement()
            logger { element + "---" + req.getHeader(element) }
        }
        logger { "header-ends ----" }
        logger { "来源ip" + req.remoteAddr }
        lookListener.addIp(req.getHeader("x-real-ip"), group.ID)
        val url = URL("https://acg.toubiec.cn/random.php")
        url.openStream().use {
            resp.contentType = MediaType.IMAGE_PNG_VALUE
            resp.outputStream.write(it.readBytes())
        }
    }
}