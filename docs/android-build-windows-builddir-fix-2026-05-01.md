# Android Build 跨平台 `buildDir` 修复报告

日期：2026-05-01
分支：`feat/harmony42-foreground-hardening-phase1`

## 根因

`common/build.gradle.kts` 和 `mjpeg/build.gradle.kts` 把 `layout.buildDirectory` 硬编码到 Windows 路径 `C:/tmp/screenstream-build/...`。

在 Ubuntu GitHub Actions runner 上，这会把构建产物与 `generate*RFile`、`R.jar` 相关输出导向一个不适用的路径，干净环境下资源生成链路失效。

## 修复策略

仅在 Windows 上保留 ASCII `buildDir`，其他平台回退默认 Gradle 目录：

- `common/build.gradle.kts`
- `mjpeg/build.gradle.kts`

未回滚或修改 `common/src/main/AndroidManifest.xml`。

## 本地验证

环境：

```powershell
$env:JAVA_HOME='D:\JAVA\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

1. `./gradlew.bat clean :common:compileDebugKotlin --rerun-tasks`

- 结果：`BUILD SUCCESSFUL`
- 关键任务：`generateDebugRFile`, `compileDebugKotlin`

2. `./gradlew.bat :common:compileReleaseKotlin --rerun-tasks`

- 结果：`BUILD SUCCESSFUL`
- 关键任务：`generateReleaseRFile`, `compileReleaseKotlin`

3. `./gradlew.bat :mjpeg:testDebugUnitTest`

- 结果：`BUILD SUCCESSFUL`

4. `./gradlew.bat :app:assembleFDroidDebug`

- 结果：`BUILD SUCCESSFUL`

## GitHub Actions

使用命令：

```powershell
gh run list --workflow "Android Build" --limit 1 --json databaseId,displayTitle,status,conclusion,headBranch,headSha,event,url,createdAt,updatedAt
gh run watch 25192725557 --exit-status
gh run view 25192725557 --json databaseId,displayTitle,status,conclusion,url,jobs,headSha,headBranch,event,createdAt,updatedAt
```

结果：

- 最新 run：`25192725557`
- 链接：`https://github.com/lim12137/ScreenStream/actions/runs/25192725557`
- 触发方式：`push`
- 分支：`feat/harmony42-foreground-hardening-phase1`
- 提交：`bb298b5c9a35fc3f2c55f649d9af84433f3d47e7`
- 总结论：`success`

Job 结果：

- `build-fdroid-debug`：`success`
- `build-fdroid-release`：`success`

本次 run 未再出现 Ubuntu 上因 Windows-only `buildDir` 导致的 `generate*RFile` / `R.jar` 失败。

额外观察：

- workflow 存在 GitHub Actions `Node.js 20` deprecation warning，但不影响本次构建结论。
