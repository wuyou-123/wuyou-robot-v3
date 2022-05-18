package pers.wuyou.robot.music.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * @author wuyou
 */
@ConfigurationProperties(prefix = "music")
@Configuration
class MusicProperties {
    // QQ音乐
    val tencent = Tencent()

    // 网易云音乐
    val netEase = NetEase()

    class Tencent : Properties()

    class NetEase : Properties() {
        var serverHost: String? = null
    }

    open class Properties {
        var account: String? = null
        var password: String? = null
    }
}