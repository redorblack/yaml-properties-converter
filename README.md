# YAML ⇄ Properties Converter

**English** | [中文](README.zh-CN.md)

> **Lightweight · 100% local & offline · Free forever.**
> Convert Spring Boot configuration between **YAML** and **.properties**, both ways —
> in your IDE or from a single HTML file. Nothing is uploaded, so there's zero risk of leaking secrets.

Copy-pasting configs into an online converter means handing your credentials to
someone else's server. This tool runs **entirely on your machine**, so it's safe for
production configs full of passwords and secret keys.

## ✨ Why you'll like it

- **Lightweight** — a single self-contained HTML file, or a tiny IDE plugin. No account, no backend, no bloat.
- **Two ways, zero setup** — double-click the HTML in any browser, or dock the IntelliJ IDEA plugin in your sidebar.
- **Correct, not just close** — YAML anchors `&`, aliases `*` and merge keys `<<` are expanded exactly the way Spring Boot resolves them at runtime.
- **Handles the tricky bits** — multi-document `---`, indexed arrays `key[n]`, and `.properties` escaping are all round-tripped faithfully.
- **Friendly validation** — pinpoints the line/column of a YAML error and explains the fix in plain language.
- **Private by design** — no network calls at all. Nothing is uploaded, logged, or tracked.

## Two forms

| Form | Path | Notes |
|------|------|-------|
| **Single-file HTML** | [`web/yaml-properties-converter.html`](web/yaml-properties-converter.html) | Inlines js-yaml. Double-click to open in any browser — no install, no network. |
| **IntelliJ IDEA plugin** | [`plugin/`](plugin/) | Kotlin + snakeyaml. A tool window on the right sidebar. |

## Project structure

Gradle multi-module; the plugin *is* the repo root:

```
├── core/     Pure-JVM conversion engine (no IntelliJ deps, unit-tested)
├── plugin/   IntelliJ Platform UI (depends on core)
└── web/      Single-file HTML version
```

## Build

```bash
./gradlew test           # run core unit tests
./gradlew buildPlugin     # -> plugin/build/distributions/yaml-properties-converter-<version>.zip
./gradlew runIde          # launch a sandbox IDE with the plugin for a test drive
```

Install: `Settings > Plugins > ⚙️ > Install Plugin from Disk...` → pick the zip → restart.

## Feedback

Found a bug or have an idea? Open a [GitHub Issue](https://github.com/redorblack/yaml-properties-converter/issues).

## License

[MIT](LICENSE) © 2026 Red. A labor of love — **free forever, no ads, no telemetry, no paywall.**

Third-party libraries: [js-yaml](https://github.com/nodeca/js-yaml) (MIT), [snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml) (Apache-2.0).
