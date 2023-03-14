import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

@Composable
@Preview
fun app(window: ComposeWindow) {
    val result = remember { mutableStateOf("") }

    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    result.value = ShellUtils.execCmd("adb devices", "").output ?: ""
                }) {
                Text(result.value)
            }
            Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    result.value = ""
                }) {
                Text("Reset")
            }

            UIUtil.DropBoxPanel(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Color.Cyan),
                window = window
            ) {
                println(it.joinToString(","))
            }
        }


    }
}


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "test app") {
        app(window)
    }
}
