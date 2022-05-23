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
class SshConfiguration(private val sshProperties: SshProperties) : LoadTimeWeaverAware {
    private var session: Session? = null

    init {
        if (sshProperties.host != null) {
            try {
                session = JSch().getSession(sshProperties.username, sshProperties.host, sshProperties.port!!).apply {
                    setConfig("StrictHostKeyChecking", "no")
                    setPassword(sshProperties.password)
                    serverAliveInterval = 20000
                    connect()
                    sshProperties.forward?.let {
                        setPortForwardingL(it.fromHost, it.fromPort!!, it.toHost, it.toPort!!)
                        logger {
                            "Forward database success!  ${it.fromHost}:${it.fromPort} -> ${it.toHost}:${it.toPort}"
                        }
                    }
                }
            } catch (e: JSchException) {
                logger(LogLevel.ERROR, e) { "Ssh ${sshProperties.host} failed." }
            }
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