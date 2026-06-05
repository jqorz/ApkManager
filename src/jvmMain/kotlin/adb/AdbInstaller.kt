package adb

import settings.AppSettings
import java.io.File

data class AdbDevice(
    val serial: String,      // 设备序列号（如 IP:端口 或 USB 序列号）
    val status: String       // device / unauthorized / offline 等
) {
    val displayName: String
        get() = "$serial ($status)"
    val isReady: Boolean
        get() = status == "device"
}

object AdbInstaller {

    /**
     * 获取 ADB 可执行文件路径
     * 优先使用用户自定义路径，否则从环境变量 PATH 中查找
     */
    fun getAdbPath(): String {
        val customPath = AppSettings.getAdbPath()
        if (customPath.isNotBlank() && File(customPath).exists()) {
            return customPath
        }

        // 从环境变量 PATH 中查找 adb
        val pathEnv = System.getenv("PATH") ?: ""
        val pathSeparator = if (System.getProperty("os.name").lowercase().contains("win")) ";" else ":"
        val adbName = if (System.getProperty("os.name").lowercase().contains("win")) "adb.exe" else "adb"

        for (dir in pathEnv.split(pathSeparator)) {
            val adbFile = File(dir, adbName)
            if (adbFile.exists() && adbFile.canExecute()) {
                return adbFile.absolutePath
            }
        }

        return "adb" // fallback，让系统自己找
    }

    /**
     * 检查 ADB 是否可用
     */
    fun isAdbAvailable(): Boolean {
        return try {
            val process = ProcessBuilder(getAdbPath(), "version")
                .redirectErrorStream(true)
                .start()
            val result = process.waitFor()
            result == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取已连接的设备列表
     * 解析 `adb devices` 输出
     */
    fun getDevices(): List<AdbDevice> {
        return try {
            val process = ProcessBuilder(getAdbPath(), "devices")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.lines()
                .drop(1) // 跳过 "List of devices attached" 标题行
                .map { it.trim() }
                .filter { it.isNotBlank() && it.contains("\t") }
                .map { parts ->
                    val cols = parts.split("\t")
                    AdbDevice(serial = cols[0], status = cols.getOrElse(1) { "unknown" })
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 通过 ADB 安装 APK
     * @param apkPath APK 文件路径
     * @param deviceSerial 目标设备序列号，为 null 时不指定设备（仅单设备时可用）
     * @param onOutput 输出回调（实时输出安装进度）
     * @param onComplete 安装完成回调（成功/失败，消息）
     */
    fun install(
        apkPath: String,
        deviceSerial: String? = null,
        onOutput: (String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        Thread {
            try {
                val adbPath = getAdbPath()
                val cmd = mutableListOf(adbPath)
                if (deviceSerial != null) {
                    cmd.addAll(listOf("-s", deviceSerial))
                }
                cmd.addAll(listOf("install", "-r", apkPath))

                onOutput("执行: ${cmd.joinToString(" ")}\n")

                val process = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start()

                val reader = process.inputStream.bufferedReader()
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        output.append(it).append("\n")
                        onOutput(it + "\n")
                    }
                }

                val exitCode = process.waitFor()
                if (exitCode == 0 && output.toString().contains("Success")) {
                    onComplete(true, "安装成功")
                } else {
                    onComplete(false, "安装失败: ${output.toString().trim()}")
                }
            } catch (e: Exception) {
                onComplete(false, "安装异常: ${e.message}")
            }
        }.start()
    }
}
