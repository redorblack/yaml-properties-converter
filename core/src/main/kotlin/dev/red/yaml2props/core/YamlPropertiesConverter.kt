package dev.red.yaml2props.core

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.MarkedYAMLException

/**
 * YAML ⇄ Properties 转换核心。
 * <p>
 * 纯 JVM 逻辑，不依赖任何 IntelliJ API，方便独立测试与复用。
 * 基于 snakeyaml：锚点 / 别名 / 合并键(<<) 由解析器原生展开，与 Spring Boot 加载行为一致。
 * </p>
 *
 * @author Red
 * @since 2026-08-14
 */
object YamlPropertiesConverter {

    data class Validation(val valid: Boolean, val message: String)

    // allowDuplicateKeys=false：snakeyaml 默认允许重复键（后者覆盖），这里关掉以便校验能报出「重复 key」
    // （README/描述里承诺的常见坑之一，不开这个开关该检测形同虚设）。
    private fun newYaml(): Yaml =
        Yaml(SafeConstructor(LoaderOptions().apply { isAllowDuplicateKeys = false }))

    // ==================== 校验 ====================

    fun validateYaml(text: String): Validation {
        return try {
            var count = 0
            newYaml().loadAll(text).forEach { _ -> count++ }
            Validation(true, "✓ YAML 格式正确" + if (count > 1) "（含 $count 个文档）" else "")
        } catch (e: Exception) {
            Validation(false, describeYamlError(e, text))
        }
    }

    // ==================== YAML → Properties ====================

    fun yamlToProperties(text: String): String {
        val docs = newYaml().loadAll(text).toList()
        val blocks = mutableListOf<String>()
        docs.forEachIndexed { idx, doc ->
            if (doc == null) return@forEachIndexed
            val lines = mutableListOf<Pair<String, String>>()
            flatten(doc, "", lines)
            val body = lines.joinToString("\n") { (k, v) ->
                if (k.startsWith("#")) "$k $v" else escapeKey(k) + "=" + escapeValue(v)
            }
            if (docs.size > 1) blocks.add("#--- YAML 文档 ${idx + 1} ---\n$body") else blocks.add(body)
        }
        return blocks.joinToString("\n\n")
    }

    private fun flatten(node: Any?, prefix: String, out: MutableList<Pair<String, String>>) {
        when (node) {
            null -> out.add(prefix to "")
            is Map<*, *> -> {
                if (node.isEmpty()) {
                    out.add(prefix to "")
                } else {
                    node.forEach { (k, v) ->
                        val child = if (prefix.isEmpty()) k.toString() else "$prefix.$k"
                        flatten(v, child, out)
                    }
                }
            }
            is List<*> -> {
                if (node.isEmpty()) {
                    out.add("#$prefix" to "(空列表，properties 无法表示)")
                } else {
                    node.forEachIndexed { i, item -> flatten(item, "$prefix[$i]", out) }
                }
            }
            else -> out.add(prefix to node.toString())
        }
    }

    private fun escapeValue(v: String): String = v
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun escapeKey(k: String): String {
        val b = k.replace("\\", "\\\\")
        return b.replace(Regex("[ =:#!]")) { "\\" + it.value }
    }

    // ==================== Properties → YAML ====================

    private data class Entry(val key: String, val value: Any?)

    fun propertiesToYaml(text: String): String {
        val entries = parseProperties(text)
        val tree = buildTree(entries)
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            indent = 2
            isPrettyFlow = true
            width = Int.MAX_VALUE
        }
        return Yaml(options).dump(tree)
    }

    private fun parseProperties(text: String): List<Entry> {
        val raw = text.split(Regex("\r?\n"))
        val entries = mutableListOf<Entry>()
        var i = 0
        while (i < raw.size) {
            var line = raw[i]; i++
            val trimmed = line.trimStart()
            if (trimmed.isEmpty() || trimmed[0] == '#' || trimmed[0] == '!') continue
            var full = line
            while (endsWithContinuation(full) && i < raw.size) {
                full = full.replace(Regex("\\\\+$")) { it.value.dropLast(1) }
                full += raw[i].trimStart(); i++
            }
            splitKeyValue(full)?.let { entries.add(it) }
        }
        return entries
    }

    private fun endsWithContinuation(s: String): Boolean {
        val m = Regex("(\\\\+)$").find(s) ?: return false
        return m.groupValues[1].length % 2 == 1
    }

    private fun splitKeyValue(line: String): Entry? {
        var idx = -1
        var j = 0
        while (j < line.length) {
            val c = line[j]
            if (c == '\\') { j += 2; continue }
            if (c == '=' || c == ':') { idx = j; break }
            j++
        }
        val rawKey: String
        val rawVal: String
        if (idx == -1) {
            rawKey = line.trim(); rawVal = ""
        } else {
            rawKey = line.substring(0, idx).trim()
            rawVal = line.substring(idx + 1).trimStart()
        }
        if (rawKey.isEmpty()) return null
        return Entry(unescapeKey(rawKey), coerceValue(unescapeValue(rawVal)))
    }

    private fun unescapeKey(k: String): String =
        k.replace(Regex("\\\\([ =:#!\\\\])")) { it.groupValues[1] }

    private fun unescapeValue(v: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < v.length) {
            val c = v[i]
            if (c == '\\' && i + 1 < v.length) {
                val n = v[i + 1]
                sb.append(
                    when (n) {
                        'n' -> '\n'; 't' -> '\t'; 'r' -> '\r'; else -> n
                    }
                )
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    private fun coerceValue(v: String): Any {
        if (v == "true") return true
        if (v == "false") return false
        if (v.isEmpty()) return ""
        if (Regex("^-?\\d+$").matches(v)) {
            v.toLongOrNull()?.let { if (it.toString() == v) return it }
        }
        if (Regex("^-?\\d*\\.\\d+$").matches(v)) {
            v.toDoubleOrNull()?.let { return it }
        }
        return v
    }

    private sealed class Token
    private data class KeyToken(val name: String) : Token()
    private data class IndexToken(val index: Int) : Token()

    private fun tokenizeKey(key: String): List<Token> {
        val tokens = mutableListOf<Token>()
        for (part in key.split(".")) {
            val re = Regex("([^\\[\\]]+)|\\[(\\d+)\\]")
            for (m in re.findAll(part)) {
                val g2 = m.groups[2]
                if (g2 != null) tokens.add(IndexToken(g2.value.toInt()))
                else tokens.add(KeyToken(m.groups[1]!!.value))
            }
        }
        return tokens
    }

    private fun buildTree(entries: List<Entry>): Any {
        val root = LinkedHashMap<String, Any?>()
        for (e in entries) {
            assign(root, tokenizeKey(e.key), e.value)
        }
        return normalize(root) ?: LinkedHashMap<String, Any?>()
    }

    private fun assign(root: MutableMap<String, Any?>, tokens: List<Token>, value: Any?) {
        var cur: MutableMap<String, Any?> = root
        for (i in tokens.indices) {
            val t = tokens[i]
            val keyName = when (t) {
                is IndexToken -> "__arr__${t.index}"
                is KeyToken -> t.name
            }
            if (i == tokens.size - 1) {
                cur[keyName] = value
            } else {
                val existing = cur[keyName]
                if (existing is MutableMap<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    cur = existing as MutableMap<String, Any?>
                } else {
                    val m = LinkedHashMap<String, Any?>()
                    cur[keyName] = m
                    cur = m
                }
            }
        }
    }

    private fun normalize(node: Any?): Any? {
        if (node !is Map<*, *>) return node
        val keys = node.keys.map { it.toString() }
        val arrKeys = keys.filter { it.startsWith("__arr__") }
        if (arrKeys.isNotEmpty() && arrKeys.size == keys.size) {
            val maxIdx = arrKeys.maxOf { it.substring(7).toInt() }
            val arr = arrayOfNulls<Any?>(maxIdx + 1)
            for (k in arrKeys) {
                val idx = k.substring(7).toInt()
                arr[idx] = normalize(node[k])
            }
            return arr.toList()
        }
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in node) out[k.toString()] = normalize(v)
        return out
    }

    // ==================== 错误友好化 ====================

    fun describeYamlError(e: Exception, text: String): String {
        val sb = StringBuilder("✗ YAML 格式错误\n")
        var lineNo: Int? = null
        if (e is MarkedYAMLException) {
            val mark = e.problemMark
            if (mark != null) {
                lineNo = mark.line + 1
                sb.append("位置：第 ${mark.line + 1} 行，第 ${mark.column + 1} 列\n")
            }
        }
        val reason = e.message ?: e.toString()
        sb.append("原因：${translateReason(reason)}")
        commonMistakeHint(text, lineNo)?.let { sb.append("\n💡 $it") }
        return sb.toString()
    }

    private fun translateReason(reason: String): String {
        val map = listOf(
            Regex("duplicate key", RegexOption.IGNORE_CASE) to "存在重复的键（同一层级有两个相同的配置项）",
            Regex("found character .*tab", RegexOption.IGNORE_CASE) to "使用了制表符 Tab 缩进，YAML 只允许用空格缩进",
            Regex("mapping values are not allowed", RegexOption.IGNORE_CASE) to "冒号使用位置不对（值里含冒号需加引号，如 url: \"a:b\"）",
            Regex("could not find expected", RegexOption.IGNORE_CASE) to "格式不完整，缺少预期的符号（如冒号或缩进）",
            Regex("while scanning|unexpected end", RegexOption.IGNORE_CASE) to "内容意外结束，可能有未闭合的引号或括号"
        )
        for ((re, cn) in map) {
            if (re.containsMatchIn(reason)) return "$cn"
        }
        return reason
    }

    private fun commonMistakeHint(text: String, lineNo: Int?): String? {
        if (text.contains('\t')) {
            val tabLines = mutableListOf<Int>()
            text.split(Regex("\r?\n")).forEachIndexed { i, l ->
                if (Regex("^\\s*\t").containsMatchIn(l) || l.startsWith("\t")) tabLines.add(i + 1)
            }
            if (tabLines.isNotEmpty()) {
                return "检测到第 ${tabLines.take(5).joinToString("、")} 行用了 Tab 缩进，请改成空格（一般 2 个空格一级）。"
            }
        }
        if (lineNo != null) {
            val arr = text.split(Regex("\r?\n"))
            val line = arr.getOrNull(lineNo - 1) ?: ""
            if (Regex("^\\s*[^\\s#][^:]*:[^\\s]").containsMatchIn(line) && !line.contains("://")) {
                return "冒号后面要有一个空格，例如 name: demo（不能写成 name:demo）。"
            }
        }
        return null
    }
}
