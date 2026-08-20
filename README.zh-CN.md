# YAML ⇄ Properties Converter

[English](README.md) | **中文**

> **轻量 · 100% 本地离线 · 永久免费。**
> Spring Boot 配置文件在 **YAML** 与 **.properties** 之间双向转换——
> 在 IDE 里,或用一个单文件 HTML。全程不上传,零敏感信息泄漏风险。

把配置粘进在线转换网站,等于把凭据交给别人的服务器。本工具**全程在本机运行**,
含密码、密钥的生产配置也能放心处理。

## ✨ 亮点

- **轻量** —— 一个自包含的 HTML 文件,或一个小巧的 IDE 插件。无账号、无后端、无臃肿。
- **两种形态,零配置** —— HTML 双击浏览器打开,或把 IntelliJ IDEA 插件停在侧边栏。
- **准确,不是"差不多"** —— YAML 锚点 `&` / 别名 `*` / 合并键 `<<` 的展开结果与 Spring Boot 运行时完全一致。
- **边角都处理** —— 多文档 `---`、数组 `key[n]` 还原、`.properties` 转义都能忠实往返。
- **友好校验** —— 精确定位 YAML 错误的行/列,并用中文说明怎么修。
- **隐私设计** —— 零网络请求,不上传、不记录、不追踪。

## 两种形态

| 形态 | 路径 | 说明 |
|------|------|------|
| **单文件 HTML** | [`web/yaml-properties-converter.html`](web/yaml-properties-converter.html) | 内联 js-yaml,双击浏览器打开即用,零联网零安装。 |
| **IntelliJ IDEA 插件** | [`plugin/`](plugin/) | Kotlin + snakeyaml,右侧边栏工具窗口。 |

## 项目结构

Gradle 多模块,插件即仓库根:

```
├── core/     纯 JVM 转换内核(不依赖 IntelliJ,带单元测试)
├── plugin/   IntelliJ 平台 UI(依赖 core)
└── web/      单文件 HTML 版
```

## 构建

```bash
./gradlew test           # 跑 core 单元测试
./gradlew buildPlugin     # 产出 plugin/build/distributions/yaml-properties-converter-<版本>.zip
./gradlew runIde          # 启动带插件的沙箱 IDE 试用
```

安装:`Settings > Plugins > ⚙️ > Install Plugin from Disk...` → 选 zip → 重启。

## 反馈

有 bug 或功能建议,欢迎提 [GitHub Issues](https://github.com/redorblack/yaml-properties-converter/issues)。

## 许可

[MIT](LICENSE) © 2026 Red。个人为爱发电——**永久免费,无广告、无遥测、无收费。**

内联/依赖的第三方库:[js-yaml](https://github.com/nodeca/js-yaml)(MIT)、[snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml)(Apache-2.0)。
