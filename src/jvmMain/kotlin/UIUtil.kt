import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
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

object UIUtil {

    @Composable
    fun DropBoxPanel(
        modifier: Modifier, window: ComposeWindow, component: JPanel = JPanel(), onFileDrop: (List<String>) -> Unit
    ) {

        val dropBoundsBean = remember {
            mutableStateOf(DropBoundsBean())
        }

        Box(modifier = modifier.onPlaced {
            dropBoundsBean.value = DropBoundsBean(
                x = it.positionInWindow().x,
                y = it.positionInWindow().y,
                width = it.size.width,
                height = it.size.height
            )
        }) {
            LaunchedEffect(true) {
                component.setBounds(
                    dropBoundsBean.value.x.roundToInt(),
                    dropBoundsBean.value.y.roundToInt(),
                    dropBoundsBean.value.width,
                    dropBoundsBean.value.height
                )
                window.contentPane.add(component)

                val target = object : DropTarget(component, object : DropTargetAdapter() {
                    override fun drop(event: DropTargetDropEvent) {

                        event.acceptDrop(DnDConstants.ACTION_REFERENCE)
                        val dataFlavors = event.transferable.transferDataFlavors
                        dataFlavors.forEach {
                            if (it == DataFlavor.javaFileListFlavor) {
                                val list = event.transferable.getTransferData(it) as List<*>

                                val pathList = mutableListOf<String>()
                                list.forEach { filePath ->
                                    pathList.add(filePath.toString())
                                }
                                onFileDrop(pathList)
                            }
                        }
                        event.dropComplete(true)

                    }
                }) {

                }
            }

            SideEffect {
                component.setBounds(
                    dropBoundsBean.value.x.roundToInt(),
                    dropBoundsBean.value.y.roundToInt(),
                    dropBoundsBean.value.width,
                    dropBoundsBean.value.height
                )
            }

            DisposableEffect(true) {
                onDispose {
                    window.contentPane.remove(component)
                }
            }
        }
    }

    fun showFileSelector(suffixList: Array<String>, onFileSelected: (String) -> Unit) {
        JFileChooser().apply {
            //设置页面风格
            try {
                val lookAndFeel = UIManager.getSystemLookAndFeelClassName()
                UIManager.setLookAndFeel(lookAndFeel)
                SwingUtilities.updateComponentTreeUI(this)
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = false
            fileFilter = FileNameExtensionFilter("文件过滤", *suffixList)

            val result = showOpenDialog(ComposeWindow())
            if (result == JFileChooser.APPROVE_OPTION) {
                val dir = this.currentDirectory
                val file = this.selectedFile
                println("Current apk dir: ${dir.absolutePath} ${dir.name}")
                println("Current apk name: ${file.absolutePath} ${file.name}")
                onFileSelected(file.absolutePath)
            }
        }
    }

    class DropBoundsBean(val x: Float, val y: Float, val width: Int, val height: Int) {
        constructor() : this(0f, 0f, 0, 0)
    }
}