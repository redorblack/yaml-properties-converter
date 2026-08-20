plugins {
    kotlin("jvm")
}

dependencies {
    // core 不依赖 IntelliJ API，可独立编译/测试。
    compileOnly(kotlin("stdlib"))

    // YAML 解析内核：锚点/别名/合并键由 snakeyaml 原生展开，与 Spring Boot 加载行为一致。
    // 声明为 implementation → 会随 plugin 的 project(":core") 依赖打进插件自带 classloader。
    implementation("org.yaml:snakeyaml:2.2")

    testImplementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
