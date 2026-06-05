package settings

import java.util.prefs.Preferences

/**
 * 应用设置管理，使用 Java Preferences API 持久化
 */
object AppSettings {
    private val prefs: Preferences = Preferences.userNodeForPackage(AppSettings::class.java)

    private const val KEY_SCAN_PATH = "scan_path"
    private const val KEY_ADB_PATH = "adb_path"
    private const val KEY_EXCLUDE_PATHS = "exclude_paths"

    private const val SEPARATOR = "\n"

    /**
     * 获取扫描路径
     */
    fun getScanPath(): String {
        return prefs.get(KEY_SCAN_PATH, "")
    }

    /**
     * 设置扫描路径
     */
    fun setScanPath(path: String) {
        prefs.put(KEY_SCAN_PATH, path)
    }

    /**
     * 获取自定义 ADB 路径
     */
    fun getAdbPath(): String {
        return prefs.get(KEY_ADB_PATH, "")
    }

    /**
     * 设置自定义 ADB 路径
     */
    fun setAdbPath(path: String) {
        prefs.put(KEY_ADB_PATH, path)
    }

    /**
     * 获取排除路径列表
     */
    fun getExcludePaths(): List<String> {
        val raw = prefs.get(KEY_EXCLUDE_PATHS, "")
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
    }

    /**
     * 设置排除路径列表
     */
    fun setExcludePaths(paths: List<String>) {
        prefs.put(KEY_EXCLUDE_PATHS, paths.joinToString(SEPARATOR))
    }
}
