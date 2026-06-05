package model

data class ApkInfo(
    val name: String,          // 文件名
    val path: String,          // 完整路径
    val packageName: String,   // 包名
    val versionName: String,   // 版本名
    val versionCode: Long,     // 版本号
    val minSdkVersion: Int,    // 最低支持安卓版本
    val targetSdkVersion: Int, // 目标安卓版本
    val architectures: List<String>, // 支持的架构
    val fileSize: Long         // 文件大小(字节)
) {
    val sizeDisplay: String
        get() {
            val kb = fileSize / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
        }

    val minSdkDisplay: String
        get() = "Android ${sdkToVersion(minSdkVersion)} (API $minSdkVersion)"

    val archDisplay: String
        get() = if (architectures.isEmpty()) "通用" else architectures.joinToString(", ")

    companion object {
        fun sdkToVersion(sdk: Int): String = when (sdk) {
            1 -> "1.0"
            2 -> "1.1"
            3 -> "1.5"
            4 -> "1.6"
            5 -> "2.0"
            6 -> "2.0.1"
            7 -> "2.1"
            8 -> "2.2"
            9 -> "2.3"
            10 -> "2.3.3"
            11 -> "3.0"
            12 -> "3.1"
            13 -> "3.2"
            14 -> "4.0"
            15 -> "4.0.3"
            16 -> "4.1"
            17 -> "4.2"
            18 -> "4.3"
            19 -> "4.4"
            20 -> "4.4W"
            21 -> "5.0"
            22 -> "5.1"
            23 -> "6.0"
            24 -> "7.0"
            25 -> "7.1"
            26 -> "8.0"
            27 -> "8.1"
            28 -> "9"
            29 -> "10"
            30 -> "11"
            31 -> "12"
            32 -> "12L"
            33 -> "13"
            34 -> "14"
            35 -> "15"
            else -> sdk.toString()
        }
    }
}
