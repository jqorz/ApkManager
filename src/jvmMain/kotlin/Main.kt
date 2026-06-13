import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import model.ApkInfo
import settings.AppSettings
import ui.MainScreen
import ui.SettingsScreen
import java.awt.Dimension

enum class Page { MAIN, SETTINGS }

@Composable
fun App() {
    val currentPage = remember { mutableStateOf(Page.MAIN) }
    val apkList = remember { mutableStateOf<List<ApkInfo>>(emptyList()) }
    val lastScanPath = remember { mutableStateOf("") }

    when (currentPage.value) {
        Page.MAIN -> MainScreen(
            apkList = apkList,
            lastScanPath = lastScanPath,
            onNavigateToSettings = { currentPage.value = Page.SETTINGS }
        )
        Page.SETTINGS -> SettingsScreen(
            onBack = {
                // 路径变化时清空列表，回到主页会自动重新扫描
                if (AppSettings.getScanPath() != lastScanPath.value) {
                    apkList.value = emptyList()
                    lastScanPath.value = ""
                }
                currentPage.value = Page.MAIN
            }
        )
    }
}

fun main() = application {
    val minWidth = 1280.dp
    val minHeight = 720.dp
    Window(
        onCloseRequest = ::exitApplication,
        title = "APK Manager",
        state = rememberWindowState(width = minWidth, height = minHeight)
    ) {
        val density = window.graphicsConfiguration.defaultTransform.scaleX.toFloat()
        window.minimumSize = Dimension(
            (minWidth.value * density).toInt(),
            (minHeight.value * density).toInt()
        )
        App()
    }
}
