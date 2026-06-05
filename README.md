# ApkManager

一个基于 Compose Desktop 的 APK 文件管理工具，支持扫描指定目录下的 APK 文件，解析应用信息，筛选排序，并通过 ADB 安装到设备。

## 功能特性

### APK 扫描与解析

- 递归扫描指定目录下的所有 `.apk` 文件
- 自动解析 APK 信息：
  - 包名（packageName）
  - 版本名 / 版本号（versionName / versionCode）
  - 最低支持 Android 版本（minSdkVersion）
  - 目标 Android 版本（targetSdkVersion）
  - 支持架构（arm64-v8a、armeabi-v7a、x86、x86_64 等，从 `lib/` 目录推断）
  - 文件大小
- 扫描过程中实时显示已发现的 APK 数量
- 支持 Windows UNC 网络路径（如 `\\server\share\path`）
- 支持配置排除路径，扫描时自动跳过指定目录

### 筛选与排序

- **搜索**：按文件名或包名模糊搜索
- **架构筛选**：下拉选择，仅显示支持指定架构的 APK
- **Android 版本筛选**：下拉选择，显示最低支持版本 ≤ 所选版本的 APK（例如选 Android 7，会显示 Android 5/6/7 的 APK）
- **排序**：支持按名称、版本号、文件大小、SDK 版本升降序排列

### 显示模式

- **网格模式**（默认）：自适应多列网格布局，窗口越宽显示越多列（最小列宽 300dp）
- **分组模式**：按文件夹分组显示，每组可展开/收起，展开后同样支持多列自适应布局

### ADB 安装

- 一键安装选中的 APK 到 Android 设备
- 自动检测已连接设备（`adb devices`）
- 单设备时直接安装，多设备时弹出设备选择对话框
- 设备状态显示：已连接 / 未授权 / 离线
- 实时输出 ADB 安装日志
- 支持 `adb -s <serial>` 指定设备安装

### 设置

- **扫描路径**：指定要扫描的根目录（支持手动输入或浏览选择）
- **排除路径**：指定扫描时需要跳过的目录，支持多个（每行一个路径，编辑即保存）
- **ADB 路径**：默认从系统环境变量 `PATH` 中自动查找，也可手动指定

## 使用说明

### 环境要求

- JDK 17 或更高版本
- ADB 已安装并配置到环境变量（或在设置中指定 ADB 路径）

### 启动

```bash
# 开发模式运行
gradlew run

# 打包为安装包
gradlew packageMsi    # Windows
gradlew packageDmg    # macOS
gradlew packageDeb    # Linux
```

### 基本流程

1. **配置扫描路径**：启动后点击右上角齿轮图标进入设置，填写扫描路径（也可通过浏览按钮选择目录），返回主页后自动开始扫描
2. **查看 APK 列表**：扫描完成后，所有 APK 以网格形式展示，显示文件名、包名、版本、最低 SDK、架构和文件大小
3. **筛选和排序**：通过顶部的下拉菜单筛选架构或 Android 版本，点击排序按钮切换排序方式
4. **分组显示**：勾选"按文件夹分组"可按目录分组浏览，点击文件夹标题展开/收起
5. **安装到设备**：点击选中一个 APK，底部会显示选中信息，点击"安装到设备"按钮，如有多个设备会弹出选择框

### 排除路径配置

在设置页的"排除路径"文本框中，每行输入一个要排除的目录路径。输入即自动保存，无需手动点击保存按钮。也可通过"添加排除目录"按钮浏览选择。

支持的路径格式示例：
```
C:\Users\xxx\backup
D:\old_apks
\\server\share\excluded
```

## 技术栈

- **语言**：Kotlin 1.7.20
- **UI 框架**：JetBrains Compose Multiplatform 1.2.2（Material Design 1）
- **APK 解析**：net.dongliu:apk-parser 2.6.10
- **构建工具**：Gradle 8.13
- **JVM**：17

## 项目结构

```
src/jvmMain/kotlin/
├── Main.kt                 # 应用入口、页面路由
├── model/
│   └── ApkInfo.kt          # APK 信息数据类
├── parser/
│   └── ApkParser.kt        # APK 扫描与解析
├── adb/
│   └── AdbInstaller.kt     # ADB 设备管理与安装
├── settings/
│   └── AppSettings.kt      # 设置持久化（Java Preferences API）
└── ui/
    ├── MainScreen.kt       # 主界面（列表、筛选、排序、分组、安装）
    └── SettingsScreen.kt   # 设置页面（路径配置）
```
