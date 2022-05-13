@file:Suppress("MemberVisibilityCanBePrivate")

package pers.wuyou.robot.core.common

import org.ktorm.database.Database
import org.ktorm.entity.forEach
import org.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import pers.wuyou.robot.core.entity.GroupBootStates
import java.io.File
import java.util.concurrent.*
import javax.annotation.PostConstruct

/**
 * @author wuyou
 */
@Suppress("unused")
@Order(1)
@Component
class RobotCore {
    @Autowired
    lateinit var database: Database

    @PostConstruct
    fun init() {
        setApplicationContext()
        initGroupBootMap()
    }

    @Synchronized
    private fun setApplicationContext() {
        robotCore = this
    }

    private fun initGroupBootMap() {
        val list = database.sequenceOf(GroupBootStates)
        list.forEach {
            BOOT_MAP[it.groupCode] = it.state
        }

    }

    companion object {
        /**
         * 项目名
         */
        const val PROJECT_NAME: String = "wuyou-robot"

        /**
         * 项目路径
         */
        val PROJECT_PATH: String = System.getProperty("user.dir") + File.separator

        /**
         * 临时路径
         */
        val TEMP_PATH: String = System.getProperty("java.io.tmpdir") + File.separator + PROJECT_NAME + File.separator

        /**
         * python路径
         */
        var PYTHON_PATH: String? = null

        /**
         * 线程池
         */
        var THREAD_POOL: ExecutorService? = null

        /**
         * 机器人管理员
         */
        val ADMINISTRATOR: List<String> = ArrayList(listOf("1097810498"))

        /**
         * 缓存群开关
         */
        val BOOT_MAP: MutableMap<String?, Boolean> = HashMap()

        var robotCore: RobotCore? = null

        init {
            val pythonEnvPath = "venv"
            PYTHON_PATH = if (File(PROJECT_PATH + pythonEnvPath).exists()) {
                PROJECT_PATH + "venv" + File.separator + "Scripts" + File.separator + "python"
            } else {
                null
            }
            THREAD_POOL = ThreadPoolExecutor(
                50,
                50,
                200,
                TimeUnit.SECONDS,
                LinkedBlockingQueue(50),
                ThreadFactory { Thread() })
        }

        fun isBotAdministrator(accountCode: String): Boolean {
            return ADMINISTRATOR.contains(accountCode)
        }
    }
}