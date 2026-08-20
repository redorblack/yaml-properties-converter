import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // 转换内核（含 snakeyaml，随本依赖打进插件 lib/）
    implementation(project(":core"))

    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )
        instrumentationTools()
        // 二进制兼容性校验工具（verifyPlugin 任务需要）
        pluginVerifier()
        // Marketplace ZIP 签名器（signPlugin 任务需要，缺它报 No ZIP Signer executable）
        zipSigner()
    }
}

intellijPlatform {
    // 产物名与插件安装目录名用 pluginName，而不是模块名 plugin
    projectName = providers.gradleProperty("pluginName").get()

    // JetBrains pluginVerifier 二进制兼容性校验（marketplace 审核同款）。
    pluginVerification {
        ides {
            // 覆盖 sinceBuild=232 下限与一个较新版本；两者 gradle 缓存已有，不重复下载。
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2023.2")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2")
            // 可选：额外对本机 IDE 验证。留空避免把本机路径硬编码进仓库。
            providers.gradleProperty("verifyAgainstLocalIde").orNull
                ?.takeIf { it.isNotBlank() }
                ?.let { local(file(it)) }
        }
    }

    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = "232"
            // 不锁上限，避免每次 IDEA 升级插件失效
            untilBuild = provider { null }
        }
    }

    // 插件签名（Marketplace 自 2021 起强制签名）。三项全走环境变量，绝不硬编码。
    // 证书 / 私钥生成步骤见 docs/PUBLISHING.md；本地不设这些变量时 signPlugin 自动跳过。
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    // 发布到 JetBrains Marketplace。token 从 Marketplace → My Tokens 生成，走环境变量。
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

kotlin {
    jvmToolchain(17)
}
