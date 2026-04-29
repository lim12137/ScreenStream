# WebView 内容 MJPEG 推流验收报告

日期：2026-04-29
分支：master

## 1) 变更范围

- `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/MjpegStreamingModule.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/MjpegEvent.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/MjpegStreamingService.kt`
- `gradle/gradle-daemon-jvm.properties`

## 2) 重点检查结论

- `SingleActivity.kt`
  - 通过 Koin 获取 `StreamingModuleManager`，并按现有模块管理流程 `startModule(MjpegStreamingModule.Id, this)` 启动 MJPEG 模块。
  - WebView 帧采集在主线程 `Handler(Looper.getMainLooper())` 循环提交到 `submitWebViewFrame`，生命周期在 `onDestroy` 中停止循环并停止流。
  - 网页音频权限仍受限于 `RESOURCE_AUDIO_CAPTURE` 且保留同源校验 `isTrustedAudioPermissionOrigin`。
  - 本次路径不调用 `startProjection`，不会主动触发 MediaProjection 授权流。
- `MjpegEvent.kt` / `MjpegStreamingService.kt` / `MjpegStreamingModule.kt`
  - 新增 `StartWebViewStream` 与 `WebViewFrame` 事件，并在 service 内引入 `webViewStreaming` 状态与 `bitmapStateFlow` 写入路径，符合现有 event-driven 状态机处理方式。
  - 停流时针对 `webViewStreaming` 分支避免无关 `componentCallbacks` 注销。
- `gradle/gradle-daemon-jvm.properties`
  - `toolchainVersion=17` 与仓库基线（JVM toolchain 17）一致，保留该改动。

## 3) 本地验证命令

```powershell
$env:JAVA_HOME='D:\JAVA\jdk-17'
$env:PATH="D:\JAVA\jdk-17\bin;" + $env:PATH
./gradlew :app:compileFDroidDebugKotlin --stacktrace
```

## 4) 结果摘要

- 结果：失败（未进入 Kotlin 编译，依赖下载阶段失败）
- 关键错误：
  - `Could not resolve all artifacts for configuration 'classpath'`
  - `Could not resolve org.apache.commons:commons-compress:1.27.1`
  - `Could not HEAD 'https://repo.maven.apache.org/...commons-compress-1.27.1.pom'`
  - `Connect to repo.maven.apache.org:443 ... failed: Connection timed out`
- 结论：当前为外部 Maven 网络超时阻塞，非代码编译错误。

## 5) GitHub Actions

- Run URL（最终验收）：
  - https://github.com/lim12137/ScreenStream/actions/runs/25118982163
- 最终状态：Success
- 覆盖任务：
  - `build-fdroid-debug`
  - `build-fdroid-release`
- 说明：
  - 第 4 节本地失败原因为 Maven Central 连接超时（外部网络问题），不属于代码正确性回归。
  - 本次改动最终以 GitHub Actions 成功结果作为验收结论。
