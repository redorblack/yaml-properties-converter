package dev.red.yaml2props.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.ContentFactory
import javax.swing.Timer

/**
 * 注册右侧 "YAML⇄Properties" 工具窗口。
 * 侧栏图标用 IconLoader 加载 SVG，开窗时闪光。
 *
 * @author Red
 * @since 2026-08-14
 */
class ConverterToolWindowFactory : ToolWindowFactory {

    private companion object {
        const val TW_ID = "YAML⇄Properties"
        val ICON_REST = IconLoader.getIcon("/icons/eye.svg", ConverterToolWindowFactory::class.java)
        val ICON_GLOW = IconLoader.getIcon("/icons/eye_glow.svg", ConverterToolWindowFactory::class.java)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ConverterPanel()
        val content = ContentFactory.getInstance().createContent(panel.component, "", false)
        toolWindow.contentManager.addContent(content)

        project.messageBus.connect(toolWindow.disposable).subscribe(
            ToolWindowManagerListener.TOPIC,
            object : ToolWindowManagerListener {
                override fun toolWindowShown(shown: ToolWindow) {
                    if (shown.id == TW_ID) flashStripe(shown)
                }
            }
        )

        flashStripe(toolWindow)
    }

    private fun flashStripe(toolWindow: ToolWindow) {
        toolWindow.setIcon(ICON_GLOW)
        Timer(600) {
            toolWindow.setIcon(ICON_REST)
        }.apply { isRepeats = false; start() }
    }
}
