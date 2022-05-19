package pers.wuyou.robot.music.controller

import com.sun.deploy.util.URLUtil
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import pers.wuyou.robot.music.entity.musicInfos
import pers.wuyou.robot.music.service.BaseMusicService
import java.io.File
import java.io.IOException
import javax.servlet.http.HttpServletResponse

/**
 * @author wuyou
 */
@Controller
@RequestMapping("music")
class MusicController {
    @Autowired
    lateinit var database: Database

    @GetMapping("/{mid}")
    fun getMusic(@PathVariable("mid") mid: String, response: HttpServletResponse, model: Model) {
        val musicInfo = database.musicInfos.find { it.mid eq mid }
        if (musicInfo == null) {
            response.status = 404
            return
        }
        val file = File(BaseMusicService.TYPE_NAME + File.separator + musicInfo.fileName)
        if (!file.exists()) {
            response.status = 404
            return
        }
        response.contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE
        var fileName = "${musicInfo.title} - ${musicInfo.artist}${
            musicInfo.fileName.substring(musicInfo.fileName.lastIndexOf("."))
        }"
        fileName = URLUtil.encodePath(fileName)
        response.setHeader("Content-Disposition", "attachment; filename=$fileName")
        response.setHeader("Content-Length", file.length().toString())
        try {
            response.outputStream.use { outputStream ->
                outputStream.write(file.readBytes())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            //出现异常返回给页面失败的信息
            model.addAttribute("result", "下载失败")
        }
        //成功后返回成功信息
        model.addAttribute("result", "下载成功")
    }
}