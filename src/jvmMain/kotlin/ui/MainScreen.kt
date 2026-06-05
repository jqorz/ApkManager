@file:OptIn(ExperimentalMaterialApi::class)

package ui

import adb.AdbDevice
import adb.AdbInstaller
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.ApkInfo
import parser.ApkParser
import settings.AppSettings
import java.io.File

enum class SortMode(val label: String) {
    NAME_ASC("名称 ↑"), NAME_DESC("名称 ↓"),
    SIZE_ASC("大小 ↑"), SIZE_DESC("大小 ↓"),
    VERSION_ASC("版本 ↑"), VERSION_DESC("版本 ↓"),
    SDK_ASC("SDK ↑"), SDK_DESC("SDK ↓")
}

@Composable
fun MainScreen(
    apkList: MutableState<List<ApkInfo>>,
    lastScanPath: MutableState<String>,
    onNavigateToSettings: () -> Unit
) {
    var selectedApk by remember { mutableStateOf<ApkInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var archFilter by remember { mutableStateOf("全部") }
    var sdkFilter by remember { mutableStateOf(0) } // 0 = 全部
    var sortMode by remember { mutableStateOf(SortMode.NAME_ASC) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // 分组相关
    var groupByFolder by remember { mutableStateOf(false) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }

    // 安装相关
    var showInstallDialog by remember { mutableStateOf(false) }
    var installOutput by remember { mutableStateOf("") }
    var installResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // 设备选择相关
    var showDeviceDialog by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf<List<AdbDevice>>(emptyList()) }
    var isFetchingDevices by remember { mutableStateOf(false) }

    // 排序/筛选下拉
    var showSortDropdown by remember { mutableStateOf(false) }
    var showArchDropdown by remember { mutableStateOf(false) }
    var showSdkDropdown by remember { mutableStateOf(false) }

    // 获取所有架构选项
    val archOptions = remember(apkList.value) {
        listOf("全部") + apkList.value.flatMap { it.architectures }.distinct().sorted()
    }

    // 获取所有 SDK 版本选项
    val sdkOptions = remember(apkList.value) {
        listOf(0 to "全部") + apkList.value.map { it.minSdkVersion }
            .distinct()
            .sorted()
            .map { sdk -> sdk to ApkInfo.sdkToVersion(sdk) }
    }

    // 筛选和排序后的列表
    val filteredList = remember(apkList.value, searchQuery, archFilter, sdkFilter, sortMode) {
        apkList.value
            .filter { apk ->
                (searchQuery.isBlank() || apk.name.contains(searchQuery, ignoreCase = true)
                        || apk.packageName.contains(searchQuery, ignoreCase = true))
                        && (archFilter == "全部" || apk.architectures.contains(archFilter))
                        && (sdkFilter == 0 || apk.minSdkVersion <= sdkFilter)
            }
            .sortedWith(Comparator { a, b ->
                when (sortMode) {
                    SortMode.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                    SortMode.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                    SortMode.SIZE_ASC -> a.fileSize.compareTo(b.fileSize)
                    SortMode.SIZE_DESC -> b.fileSize.compareTo(a.fileSize)
                    SortMode.VERSION_ASC -> a.versionCode.compareTo(b.versionCode)
                    SortMode.VERSION_DESC -> b.versionCode.compareTo(a.versionCode)
                    SortMode.SDK_ASC -> a.minSdkVersion.compareTo(b.minSdkVersion)
                    SortMode.SDK_DESC -> b.minSdkVersion.compareTo(a.minSdkVersion)
                }
            })
    }

    // 按文件夹分组
    val groupedByFolder = remember(filteredList) {
        filteredList.groupBy { File(it.path).parent ?: "" }
    }

    // 扫描函数
    fun scanApks() {
        val path = AppSettings.getScanPath()
        if (path.isBlank()) {
            statusMessage = "请先在设置中配置扫描路径"
            return
        }
        lastScanPath.value = path
        isLoading = true
        statusMessage = "扫描中... 已发现 0 个 APK"
        val excludePaths = AppSettings.getExcludePaths()
        Thread {
            val result = ApkParser.scanDirectory(path, excludePaths) { count ->
                statusMessage = "扫描中... 已发现 $count 个 APK"
            }
            apkList.value = result
            selectedApk = null
            isLoading = false
            statusMessage = if (result.isEmpty()) "未找到 APK 文件" else "共找到 ${result.size} 个 APK"
        }.start()
    }

    // 首次加载：仅当列表为空时扫描
    LaunchedEffect(Unit) {
        if (apkList.value.isEmpty() && AppSettings.getScanPath().isNotBlank()) {
            scanApks()
        } else if (apkList.value.isNotEmpty()) {
            statusMessage = "共找到 ${apkList.value.size} 个 APK"
        }
    }

    // 执行安装的辅助函数
    fun doInstall(apkPath: String, deviceSerial: String) {
        showInstallDialog = true
        installOutput = ""
        installResult = null
        AdbInstaller.install(
            apkPath = apkPath,
            deviceSerial = deviceSerial,
            onOutput = { installOutput += it },
            onComplete = { success, message -> installResult = Pair(success, message) }
        )
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = { Text("APK Manager") },
            backgroundColor = MaterialTheme.colors.primarySurface,
            actions = {
                IconButton(onClick = { scanApks() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置")
                }
            }
        )

        // 搜索栏
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            placeholder = { Text("搜索 APK 名称或包名...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )

        // 筛选和排序行
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 架构筛选
            Box {
                OutlinedButton(onClick = { showArchDropdown = true }) {
                    Text("架构: $archFilter", fontSize = 12.sp)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showArchDropdown, onDismissRequest = { showArchDropdown = false }) {
                    archOptions.forEach { arch ->
                        DropdownMenuItem(onClick = {
                            archFilter = arch
                            showArchDropdown = false
                        }) {
                            Text(arch)
                        }
                    }
                }
            }

            // 最低 Android 版本筛选
            Box {
                OutlinedButton(onClick = { showSdkDropdown = true }) {
                    Text("Android: ${if (sdkFilter == 0) "全部" else ApkInfo.sdkToVersion(sdkFilter)}", fontSize = 12.sp)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showSdkDropdown, onDismissRequest = { showSdkDropdown = false }) {
                    sdkOptions.forEach { (sdk, label) ->
                        DropdownMenuItem(onClick = {
                            sdkFilter = sdk
                            showSdkDropdown = false
                        }) {
                            Text(if (sdk == 0) "全部" else "Android $label (API $sdk)")
                        }
                    }
                }
            }

            // 排序选择
            Text("排序", fontSize = 12.sp, color = Color.Gray)
            Box {
                OutlinedButton(onClick = { showSortDropdown = true }) {
                    Text(sortMode.label, fontSize = 12.sp)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showSortDropdown, onDismissRequest = { showSortDropdown = false }) {
                    SortMode.values().forEach { mode ->
                        DropdownMenuItem(onClick = {
                            sortMode = mode
                            showSortDropdown = false
                        }) {
                            Text(mode.label)
                        }
                    }
                }
            }

            // 分组开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = groupByFolder,
                    onCheckedChange = {
                        groupByFolder = it
                        if (!it) expandedFolders = emptySet()
                    }
                )
                Text("按文件夹分组", fontSize = 12.sp)
            }

            Spacer(Modifier.weight(1f))

            // 状态信息
            Text(statusMessage, fontSize = 12.sp, color = Color.Gray)
        }

        // APK 列表
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (filteredList.isEmpty()) {
                Text(
                    if (AppSettings.getScanPath().isBlank()) "请先在设置中配置扫描路径" else "无匹配的 APK 文件",
                    Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else if (groupByFolder) {
                // 按文件夹分组显示，展开后多列自适应
                BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    val minItemWidth = 300.dp
                    val columns = maxOf(1, (maxWidth / minItemWidth).toInt())

                    LazyColumn(Modifier.fillMaxSize()) {
                        groupedByFolder.forEach { (folder, apks) ->
                            val isExpanded = expandedFolders.contains(folder)
                            // 文件夹标题
                            item(key = "header_$folder") {
                                FolderGroupHeader(
                                    folder = folder,
                                    count = apks.size,
                                    isExpanded = isExpanded,
                                    onClick = {
                                        expandedFolders = if (isExpanded) {
                                            expandedFolders - folder
                                        } else {
                                            expandedFolders + folder
                                        }
                                    }
                                )
                            }
                            // 文件夹内的 APK，按行排列实现多列
                            if (isExpanded) {
                                val rows = apks.chunked(columns)
                                rows.forEachIndexed { rowIndex, rowItems ->
                                    item(key = "row_${folder}_$rowIndex") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            rowItems.forEach { apk ->
                                                Box(Modifier.weight(1f)) {
                                                    ApkListItem(
                                                        apk = apk,
                                                        isSelected = selectedApk?.path == apk.path,
                                                        onClick = { selectedApk = apk }
                                                    )
                                                }
                                            }
                                            // 补齐空位保持等宽
                                            repeat(columns - rowItems.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 平铺网格显示
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredList, key = { it.path }) { apk ->
                        ApkListItem(
                            apk = apk,
                            isSelected = selectedApk?.path == apk.path,
                            onClick = { selectedApk = apk }
                        )
                    }
                }
            }
        }

        // 底部安装栏
        Surface(elevation = 8.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 选中信息
                Column(Modifier.weight(1f)) {
                    if (selectedApk != null) {
                        Text(
                            "已选中: ${selectedApk!!.name}",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${selectedApk!!.versionName} | ${selectedApk!!.minSdkDisplay} | ${selectedApk!!.archDisplay}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text("请选择一个 APK", color = Color.Gray)
                    }
                }

                // 安装按钮
                Button(
                    onClick = {
                        selectedApk?.let {
                            isFetchingDevices = true
                            Thread {
                                val devices = AdbInstaller.getDevices()
                                isFetchingDevices = false
                                when {
                                    devices.isEmpty() -> {
                                        showInstallDialog = true
                                        installOutput = ""
                                        installResult = Pair(false, "未检测到 ADB 设备，请确认设备已连接并开启 USB 调试")
                                    }
                                    devices.size == 1 && devices[0].isReady -> {
                                        doInstall(it.path, devices[0].serial)
                                    }
                                    else -> {
                                        deviceList = devices
                                        showDeviceDialog = true
                                    }
                                }
                            }.start()
                        }
                    },
                    enabled = selectedApk != null && !isFetchingDevices
                ) {
                    if (isFetchingDevices) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("安装到设备")
                }
            }
        }
    }

    // 安装输出对话框
    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("ADB 安装") },
            text = {
                Column {
                    Text(
                        installOutput,
                        fontSize = 12.sp,
                        modifier = Modifier.heightIn(min = 100.dp, max = 300.dp)
                    )
                    if (installResult != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            installResult!!.second,
                            color = if (installResult!!.first) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstallDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 设备选择对话框
    if (showDeviceDialog) {
        var selectedDevice by remember { mutableStateOf<AdbDevice?>(deviceList.firstOrNull { it.isReady }) }

        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text("选择目标设备") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("检测到 ${deviceList.size} 台设备，请选择安装目标：", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    deviceList.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (device.isReady) selectedDevice = device }
                                .background(
                                    if (selectedDevice?.serial == device.serial)
                                        MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDevice?.serial == device.serial,
                                onClick = { if (device.isReady) selectedDevice = device },
                                enabled = device.isReady
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(device.serial, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    when (device.status) {
                                        "device" -> "已连接"
                                        "unauthorized" -> "未授权 - 请在设备上确认 USB 调试"
                                        "offline" -> "离线"
                                        else -> device.status
                                    },
                                    fontSize = 12.sp,
                                    color = if (device.isReady) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeviceDialog = false
                        selectedDevice?.let { device ->
                            selectedApk?.let { apk ->
                                doInstall(apk.path, device.serial)
                            }
                        }
                    },
                    enabled = selectedDevice != null
                ) {
                    Text("安装")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun FolderGroupHeader(folder: String, count: Int, isExpanded: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        elevation = 2.dp,
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isExpanded) "▼" else "▶",
                fontSize = 12.sp,
                modifier = Modifier.width(20.dp)
            )
            Text(
                folder,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$count 个 APK",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ApkListItem(apk: ApkInfo, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp).clickable { onClick() },
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = bgColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(apk.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(apk.packageName, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(apk.path, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("v${apk.versionName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(apk.minSdkDisplay, fontSize = 11.sp, color = Color.Gray)
                Text(apk.archDisplay, fontSize = 11.sp, color = MaterialTheme.colors.primary)
                Text(apk.sizeDisplay, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
