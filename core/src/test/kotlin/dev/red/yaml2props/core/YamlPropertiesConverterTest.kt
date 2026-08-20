package dev.red.yaml2props.core

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 转换内核回归测试。覆盖：扁平化、类型还原、锚点/合并键展开、数组下标、转义、多文档、校验。
 */
class YamlPropertiesConverterTest {

    // ---------- YAML → Properties ----------

    @Test
    fun `nested map flattens to dotted keys`() {
        val props = YamlPropertiesConverter.yamlToProperties("server:\n  port: 8080\n  name: demo\n")
        assertContains(props, "server.port=8080")
        assertContains(props, "server.name=demo")
    }

    @Test
    fun `list becomes indexed keys`() {
        val props = YamlPropertiesConverter.yamlToProperties("items:\n  - a\n  - b\n")
        assertContains(props, "items[0]=a")
        assertContains(props, "items[1]=b")
    }

    @Test
    fun `anchor alias and merge key expand like spring boot`() {
        val yaml = """
            defaults: &d
              timeout: 30
              retries: 3
            prod:
              <<: *d
              timeout: 60
        """.trimIndent()
        val props = YamlPropertiesConverter.yamlToProperties(yaml)
        // 合并键把 defaults 的字段并入 prod，本地覆盖 timeout
        assertContains(props, "prod.timeout=60")
        assertContains(props, "prod.retries=3")
    }

    @Test
    fun `backslash and newline in value are escaped`() {
        val props = YamlPropertiesConverter.yamlToProperties("path: \"a\\\\b\"\nmulti: \"x\\ny\"\n")
        assertContains(props, "path=a\\\\b")
        assertContains(props, "multi=x\\ny")
    }

    @Test
    fun `multi document is split with header comment`() {
        val props = YamlPropertiesConverter.yamlToProperties("a: 1\n---\nb: 2\n")
        assertContains(props, "YAML 文档 1")
        assertContains(props, "a=1")
        assertContains(props, "b=2")
    }

    // ---------- Properties → YAML ----------

    @Test
    fun `scalar types are coerced`() {
        val yaml = YamlPropertiesConverter.propertiesToYaml("flag=true\ncount=42\nratio=1.5\ntext=hello\n")
        assertContains(yaml, "flag: true")
        assertContains(yaml, "count: 42")
        assertContains(yaml, "ratio: 1.5")
        assertContains(yaml, "text: hello")
    }

    @Test
    fun `indexed keys rebuild a list`() {
        val yaml = YamlPropertiesConverter.propertiesToYaml("a.b[0]=x\na.b[1]=y\n")
        assertContains(yaml, "- x")
        assertContains(yaml, "- y")
    }

    @Test
    fun `round trip preserves nested structure`() {
        val original = "server:\n  port: 8080\n  servlet:\n    context-path: /api\n"
        val props = YamlPropertiesConverter.yamlToProperties(original)
        val back = YamlPropertiesConverter.propertiesToYaml(props)
        assertContains(back, "port: 8080")
        assertContains(back, "context-path: /api")
    }

    // ---------- 校验 ----------

    @Test
    fun `valid yaml passes validation`() {
        val v = YamlPropertiesConverter.validateYaml("a: 1\nb: 2\n")
        assertTrue(v.valid)
    }

    @Test
    fun `duplicate key fails validation`() {
        val v = YamlPropertiesConverter.validateYaml("a: 1\na: 2\n")
        assertFalse(v.valid)
        assertContains(v.message, "重复")
    }

    @Test
    fun `tab indentation is reported with hint`() {
        val v = YamlPropertiesConverter.validateYaml("a:\n\tb: 1\n")
        assertFalse(v.valid)
        assertContains(v.message, "Tab")
    }

    @Test
    fun `multi document count is reported`() {
        val v = YamlPropertiesConverter.validateYaml("a: 1\n---\nb: 2\n")
        assertTrue(v.valid)
        assertContains(v.message, "2")
    }
}
