package pers.wuyou.robot.core.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.weaving.LoadTimeWeaverAware
import org.springframework.instrument.classloading.LoadTimeWeaver
import javax.annotation.PreDestroy

/**
 * @author wuyou
 */
@Configuration
@EnableConfigurationProperties(SshProperties::class)
class SshConfiguration(sshProperties: SshProperties) : LoadTimeWeaverAware {
    private val log = LoggerFactory.getLogger(SshConfiguration::class.java)
    private var session: Session? = null

    init {
        if (sshProperties.host != null) {
            var s: Session? = null
            try {
                s = JSch().getSession(sshProperties.username, sshProperties.host, sshProperties.port!!)
                s.setConfig("StrictHostKeyChecking", "no")
                s.setPassword(sshProperties.password)
                s.connect()
                val forward = sshProperties.forward
                if (forward != null) {
                    s.setPortForwardingL(forward.fromHost, forward.fromPort!!, forward.toHost, forward.toPort!!)
                    log.info(
                        "Forward database success!  {}:{} -> {}:{}",
                        forward.fromHost,
                        forward.fromPort,
                        forward.toHost,
                        forward.toPort
                    )
                }
            } catch (e: JSchException) {
                log.error("Ssh " + sshProperties.host + " failed.", e)
            }
            session = s
        }
    }

    /**
     * 配置销毁时，断开 SSH 链接
     */
    @PreDestroy
    fun disconnect() {
        session?.disconnect()
    }

    override fun setLoadTimeWeaver(loadTimeWeaver: LoadTimeWeaver) {
    }

}