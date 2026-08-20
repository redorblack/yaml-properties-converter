package dev.red.yaml2props.plugin

import dev.red.yaml2props.core.YamlPropertiesConverter
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * 双栏转换面板：左输入 / 右输出 + 顶部工具条 + 底部状态栏。
 * <p>
 * 纯 Swing，转换/校验全部委托 {@link YamlPropertiesConverter}，在本地完成，无网络。
 * </p>
 *
 * @author Red
 * @since 2026-08-14
 */
class ConverterPanel {

    private val input = JTextArea()
    private val output = JTextArea().apply { isEditable = false }
    private val status = JLabel(" ")
    private val dirCombo = JComboBox(arrayOf("YAML → Properties", "Properties → YAML"))
    private val debounce: Timer

    val component: JComponent

    init {
        input.font = JBUI.Fonts.create("monospaced", 13)
        output.font = JBUI.Fonts.create("monospaced", 13)
        status.border = JBUI.Borders.empty(4, 8)

        val convertBtn = JButton("转换 →").apply { addActionListener { convert() } }
        val validateBtn = JButton("校验").apply { addActionListener { validateOnly() } }
        val swapBtn = JButton("⇅ 交换").apply { addActionListener { swap() } }
        val copyBtn = JButton("复制结果").apply { addActionListener { copyOutput() } }
        val clearBtn = JButton("清空").apply {
            addActionListener {
                input.text = ""; output.text = ""; setStatus(" ", null)
            }
        }
        dirCombo.addActionListener { setStatus(" ", null) }

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 6, 6)).apply {
            add(dirCombo); add(convertBtn); add(validateBtn); add(swapBtn); add(copyBtn); add(clearBtn)
        }

        val split = OnePixelSplitter(false, 0.5f).apply {
            firstComponent = titled("输入", JScrollPane(input))
            secondComponent = titled("输出", JScrollPane(output))
            preferredSize = Dimension(700, 460)
        }

        component = JPanel(BorderLayout()).apply {
            add(toolbar, BorderLayout.NORTH)
            add(split, BorderLayout.CENTER)
            add(status, BorderLayout.SOUTH)
        }

        debounce = Timer(400) { convert() }.apply { isRepeats = false }
        input.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = debounce.restart()
            override fun removeUpdate(e: DocumentEvent) = debounce.restart()
            override fun changedUpdate(e: DocumentEvent) = debounce.restart()
        })
    }

    private fun titled(title: String, comp: JComponent): JComponent =
        JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder(title)
            add(comp, BorderLayout.CENTER)
        }

    private fun isY2p(): Boolean = dirCombo.selectedIndex == 0

    private fun convert() {
        val text = input.text
        if (text.isBlank()) {
            output.text = ""; setStatus("请输入内容", null); return
        }
        try {
            if (isY2p()) {
                val v = YamlPropertiesConverter.validateYaml(text)
                if (!v.valid) {
                    output.text = ""; setStatus(v.message, false); return
                }
                output.text = YamlPropertiesConverter.yamlToProperties(text)
            } else {
                output.text = YamlPropertiesConverter.propertiesToYaml(text)
            }
            val n = output.text.lines().count { it.isNotBlank() && !it.startsWith("#") }
            setStatus("✓ 转换成功，共 $n 条配置项", true)
        } catch (e: Exception) {
            output.text = ""
            val msg = if (isY2p()) YamlPropertiesConverter.describeYamlError(e, text)
            else "✗ 转换失败：${e.message}"
            setStatus(msg, false)
        }
    }

    private fun validateOnly() {
        val text = input.text
        if (text.isBlank()) {
            setStatus("请输入内容", null); return
        }
        if (isY2p()) {
            val v = YamlPropertiesConverter.validateYaml(text)
            setStatus(v.message, v.valid)
        } else {
            setStatus("Properties 格式较宽松，直接点「转换 →」即可", null)
        }
    }

    private fun swap() {
        val out = output.text
        dirCombo.selectedIndex = if (isY2p()) 1 else 0
        if (out.isNotEmpty()) {
            input.text = out; output.text = ""; convert()
        }
    }

    private fun copyOutput() {
        if (output.text.isEmpty()) return
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(output.text), null)
        setStatus("✓ 已复制到剪贴板", true)
    }

    private fun setStatus(msg: String, ok: Boolean?) {
        val html = msg.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>")
        status.text = "<html>$html</html>"
        status.foreground = when (ok) {
            true -> JBColor(Color(0x2E, 0x7D, 0x32), Color(0x6A, 0xAF, 0x6E))
            false -> JBColor(Color(0xC5, 0x30, 0x30), Color(0xEF, 0x53, 0x50))
            else -> JBColor.foreground()
        }
    }
}
