package pers.wuyou.robot.core.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.logging.LogLevel
import org.springframework.context.annotation.Configuration
import org.springframework.context.weaving.LoadTimeWeaverAware
import org.springframework.instrument.classloading.LoadTimeWeaver
import pers.wuyou.robot.core.common.logger
import javax.annotation.PreDestroy

/**
 * @author wuyou
 */
@Configuration
@EnableConfigurationProperties(SshProperties::class)
class SshConfiguration(sshProperties: SshProperties) : LoadTimeWeaverAware {
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
                    logger {
                        "Forward database success!  ${forward.fromHost}:${forward.fromPort} -> ${forward.toHost}:${forward.toPort}"
                    }
                }
            } catch (e: JSchException) {
                logger(LogLevel.ERROR, e) { "Ssh ${sshProperties.host} failed." }
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