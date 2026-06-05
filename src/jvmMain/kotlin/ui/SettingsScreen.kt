package ui

import adb.AdbInstaller
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import settings.AppSettings
import javax.swing.JFileChooser
import javax.swing.UIManager

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var scanPath by remember { mutableStateOf(AppSettings.getScanPath()) }
    var adbPath by remember { mutableStateOf(AppSettings.getAdbPath()) }
    var excludePathsText by remember { mutableStateOf(AppSettings.getExcludePaths().joinToString("\n")) }
    var saveMessage by remember { mutableStateOf("") }
    var adbStatus by remember { mutableStateOf("") }

    // 检测 ADB 状态
    LaunchedEffect(adbPath) {
        adbStatus = try {
            val adb = AdbInstaller.getAdbPath()
            if (AdbInstaller.isAdbAvailable()) "ADB 可用: $adb" else "ADB 不可用"
        } catch (e: Exception) {
            "ADB 检测失败: ${e.message}"
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = { Text("设置") },
            backgroundColor = MaterialTheme.colors.primarySurface,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 扫描路径设置
            Text("扫描路径", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("指定要扫描 APK 文件的目录路径", fontSize = 12.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = scanPath,
                    onValueChange = { scanPath = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("例如: C:\\Users\\xxx\\Downloads") },
                    singleLine = true
                )
                OutlinedButton(onClick = {
                    chooseDirectory { scanPath = it }
                }) {
                    Text("浏览")
                }
            }

            Divider()

            // 排除路径设置
            Text("排除路径", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("扫描时跳过以下目录，每行一个路径", fontSize = 12.sp, color = Color.Gray)
            OutlinedTextField(
                value = excludePathsText,
                onValueChange = {
                    excludePathsText = it
                    // 编辑即保存，无需手动点击保存按钮
                    AppSettings.setExcludePaths(
                        it.lines().map { line -> line.trim() }.filter { line -> line.isNotBlank() }
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
                placeholder = { Text("每行一个排除路径\n例如:\nC:\\backup\nD:\\old_apks") },
            )
            OutlinedButton(onClick = {
                chooseDirectory { newPath ->
                    val current = excludePathsText.trim()
                    excludePathsText = if (current.isEmpty()) newPath else "$current\n$newPath"
                    AppSettings.setExcludePaths(
                        excludePathsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    )
                }
            }) {
                Text("添加排除目录")
            }

            Divider()

            // ADB 路径设置
            Text("ADB 路径", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("默认从系统环境变量 PATH 中查找，也可自定义路径", fontSize = 12.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = adbPath,
                    onValueChange = { adbPath = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("留空则自动从环境变量查找") },
                    singleLine = true
                )
                OutlinedButton(onClick = {
                    chooseFile("可执行文件", "exe", "bat", "sh") { adbPath = it }
                }) {
                    Text("浏览")
                }
            }
            // ADB 状态
            Text(adbStatus, fontSize = 12.sp, color = if (adbStatus.contains("可用")) Color(0xFF4CAF50) else Color(0xFFF44336))

            Divider()

            // 保存按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    AppSettings.setScanPath(scanPath.trim())
                    AppSettings.setAdbPath(adbPath.trim())
                    saveMessage = "设置已保存"
                }) {
                    Text("保存设置")
                }
                if (saveMessage.isNotEmpty()) {
                    Text(saveMessage, color = Color(0xFF4CAF50), fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * 选择目录
 */
private fun chooseDirectory(onSelected: (String) -> Unit) {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Throwable) {}

    JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
        val result = showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onSelected(selectedFile.absolutePath)
        }
    }
}

/**
 * 选择文件
 */
private fun chooseFile(description: String, vararg extensions: String, onSelected: (String) -> Unit) {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Throwable) {}

    JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
        if (extensions.isNotEmpty()) {
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter(description, *extensions)
        }
        val result = showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onSelected(selectedFile.absolutePath)
        }
    }
}
