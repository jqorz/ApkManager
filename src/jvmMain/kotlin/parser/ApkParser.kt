package parser

import model.ApkInfo
import net.dongliu.apk.parser.ApkFile
import java.io.File

object ApkParser {

    /**
     * 解析单个 APK 文件
     */
    fun parse(apkFile: File): ApkInfo? {
        return try {
            ApkFile(apkFile).use { apk ->
                val meta = apk.apkMeta
                val architectures = parseArchitectures(apkFile)

                ApkInfo(
                    name = apkFile.name,
                    path = apkFile.absolutePath,
                    packageName = meta.packageName ?: "",
                    versionName = meta.versionName ?: "",
                    versionCode = meta.versionCode ?: 0L,
                    minSdkVersion = meta.minSdkVersion?.toString()?.toIntOrNull() ?: 0,
                    targetSdkVersion = meta.targetSdkVersion?.toString()?.toIntOrNull() ?: 0,
                    architectures = architectures,
                    fileSize = apkFile.length()
                )
            }
        } catch (e: Exception) {
            System.err.println("解析失败: ${apkFile.name} - ${e.message}")
            null
        }
    }

    /**
     * 扫描目录下所有 APK 文件（递归）
     * @param excludePaths 排除的目录路径列表，这些目录下的文件将被跳过
     * @param onProgress 扫描进度回调，参数为当前已发现的 APK 数量
     */
    fun scanDirectory(
        dirPath: String,
        excludePaths: List<String> = emptyList(),
        onProgress: ((Int) -> Unit)? = null
    ): List<ApkInfo> {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        // 规范化排除路径用于比较
        val normalizedExcludes = excludePaths
            .map { File(it).canonicalPath.lowercase() }
            .toSet()

        val result = mutableListOf<ApkInfo>()
        var scanned = 0
        dir.walk()
            .onEnter { subDir ->
                // 跳过排除目录
                val normalized = subDir.canonicalPath.lowercase()
                !normalizedExcludes.any { exclude -> normalized.startsWith(exclude) }
            }
            .filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
            .forEach { file ->
                parse(file)?.let { result.add(it) }
                scanned++
                onProgress?.invoke(scanned)
            }
        return result
    }

    /**
     * 从 APK 的 lib/ 目录解析支持的架构
     */
    private fun parseArchitectures(apkFile: File): List<String> {
        return try {
            java.util.zip.ZipFile(apkFile).use { zip ->
                zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith("lib/") && it.count { c -> c == '/' } >= 2 }
                    .map { it.split("/")[1] }
                    .distinct()
                    .filter { it in KNOWN_ARCHITECTURES }
                    .toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val KNOWN_ARCHITECTURES = setOf(
        "arm64-v8a", "armeabi-v7a", "armeabi", "x86", "x86_64"
    )
}
