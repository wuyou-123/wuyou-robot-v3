package pers.wuyou.robot.core.util

import org.springframework.core.io.Resource
import pers.wuyou.robot.core.common.RobotCore
import java.io.*
import java.nio.file.Files

/**
 * @author wuyou
 */
object FileUtil {
    /**
     * 将文件保存到缓存目录
     *
     * @param inputStream 文件流
     * @param fileName   文件名
     * @param folderName 文件夹名
     * @return 保存后的文件路径
     * @throws IOException IOException
     */
    @Throws(IOException::class)
    fun saveTempFile(inputStream: InputStream?, fileName: String?, folderName: String): String {
        val tempDir = File(RobotCore.TEMP_PATH + folderName + File.separator)
        val temp = File(RobotCore.TEMP_PATH + folderName + File.separator + fileName)
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw IOException("Destination '$tempDir' directory cannot be created")
        }
        if (!tempDir.canWrite()) {
            throw IOException("Destination '$tempDir' cannot be written to")
        }
        if (inputStream != null) {
            BufferedInputStream(inputStream).use { bis ->
                BufferedOutputStream(Files.newOutputStream(temp.toPath())).use { bos ->
                    var len: Int
                    val buf = ByteArray(10240)
                    while (bis.read(buf).also { len = it } != -1) {
                        bos.write(buf, 0, len)
                    }
                    bos.flush()
                }
            }
        }
        return temp.path
    }

    /**
     * 将resource文件保存到临时路径
     *
     * @param resource   resource文件
     * @param folderName 文件夹名
     * @throws IOException IOException
     */
    @Suppress("unused")
    @Throws(IOException::class)
    fun saveResourceToTempDirectory(resource: Resource, folderName: String) {
        saveTempFile(resource.inputStream, resource.filename, folderName)
    }

    /**
     * 判断文件是否存在
     *
     * @param path 文件路径
     * @return 如果存在返回true
     */
    fun exist(path: String?): Boolean {
        return null != path && File(path).exists()
    }
}