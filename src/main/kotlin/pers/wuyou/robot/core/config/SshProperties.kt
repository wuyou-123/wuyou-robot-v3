package pers.wuyou.robot.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * @author wuyou
 */
@ConfigurationProperties(prefix = "ssh")
@Component
class SshProperties {
    var host: String? = null
    var port: Int? = null
    var username: String? = null
    var password: String? = null
    var forward: Forward? = null

    class Forward {
        var fromHost: String? = null
        var fromPort: Int? = null
        var toHost: String? = null
        var toPort: Int? = null
    }
}