package pers.wuyou.robot.core.util

import org.springframework.boot.logging.LogLevel
import pers.wuyou.robot.core.common.Constant
import pers.wuyou.robot.core.common.RobotCore
import pers.wuyou.robot.core.common.logger
import pers.wuyou.robot.core.exception.RobotException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.stream.Collectors

/**
 * 命令行工具类
 *
 * @author wuyou
 */
object CommandUtil {
    @Suppress("unused")
    fun checkEnv(name: String): Boolean {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf(name, "--version"))
            var reader = BufferedReader(InputStreamReader(proc.errorStream))
            var s = reader.readLine()
            if (s == null) {
                reader = BufferedReader(InputStreamReader(proc.inputStream))
                s = reader.readLine()
            }
            if (s == null || !s.lowercase().contains(name)) {
                throw RobotException()
            }
        } catch (e: Exception) {
            return true
        }
        return false
    }

    private fun exec(args: List<String?>): List<String> {
        try {
            val list: MutableList<String> = ArrayList()
            val command = args.stream().filter { obj: String? -> Objects.nonNull(obj) }.collect(Collectors.toList())
            if (Constant.PYTHON == command[0] && RobotCore.PYTHON_PATH != null) {
                command[0] = RobotCore.PYTHON_PATH
            }
            val proc = Runtime.getRuntime().exec(command.toTypedArray())
            proc.waitFor()
            val inputStream = BufferedReader(InputStreamReader(proc.inputStream))
            var line = ""
            while (inputStream.readLine().also { it?.let { line = it } } != null) {
                list.add(line)
                logger { line }
            }
            inputStream.close()
            val in2 = BufferedReader(InputStreamReader(proc.errorStream))
            var line2 = ""
            while (in2.readLine().also { it?.let { line2 = it } } != null) {
                logger(LogLevel.WARN) { line2 }
            }
            in2.close()
            return list
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return emptyList()
    }

    fun exec(cmd: String, vararg args: String?): List<String> {
        val list: MutableList<String?> = ArrayList()
        list.add(cmd)
        list.addAll(listOf(*args))
        return exec(list)
    }

}