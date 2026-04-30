# common 模块 `R` 解析 CI 修复报告

日期：2026-05-01
分支：`feat/harmony42-foreground-hardening-phase1`

## 根因

`common` 是 Android library 模块，包含 `res/` 资源并在 `common/ui` 中直接引用 `info.dvkr.screenstream.common.R`，但模块缺少 `common/src/main/AndroidManifest.xml`。

这让模块结构不完整，容易在 CI 的干净环境里出现资源符号生成/解析链路不稳定，最终表现为 `:common:compileDebugKotlin` 和 `:common:compileReleaseKotlin` 阶段 `R` 无法解析。

## 最小改动

新增文件：

- `common/src/main/AndroidManifest.xml`

内容为最小 Android library 清单：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

未修改 `common` 业务逻辑、资源键、UI 代码或其它模块。

## 验证命令与结果

环境：

```powershell
$env:JAVA_HOME='D:\JAVA\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

1. `./gradlew.bat :common:compileDebugKotlin --rerun-tasks`

- 结果：`BUILD SUCCESSFUL`
- 关键任务：`generateDebugRFile`, `compileDebugKotlin`

2. `./gradlew.bat :common:compileReleaseKotlin --rerun-tasks`

- 结果：`BUILD SUCCESSFUL`
- 关键任务：`generateReleaseRFile`, `compileReleaseKotlin`

3. `./gradlew.bat :app:assembleFDroidDebug`

- 结果：`BUILD SUCCESSFUL`
- 产物链路验证通过，`common` 被 `app` 正常消费

## 结论

本次修复通过补齐 `common` 的最小 AndroidManifest，恢复了标准 Android library 结构，并完成 `common` Debug/Release Kotlin 编译及 `app` 组装验收。
