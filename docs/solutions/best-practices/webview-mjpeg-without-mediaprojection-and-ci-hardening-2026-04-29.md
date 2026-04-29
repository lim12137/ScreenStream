---
title: WebView内容采集接入MJPEG并完成JDK17与CI验证
date: 2026-04-29
category: docs/solutions/best-practices/
module: app,mjpeg,ci
problem_type: best_practice
component: development_workflow
severity: medium
applies_when:
  - 只需要推送应用内WebView内容而不是系统全屏
  - 希望复用现有MJPEG HTTP输出链路并降低MediaProjection权限依赖
  - 本地网络不稳定但可以用GitHub Actions完成真实验证
tags:
  - android
  - webview
  - mjpeg
  - mediaprojection-removal
  - record-audio
  - jdk17
  - gradle-wrapper-mirror
  - github-actions
---

# Context

在 ScreenStream 的本地优先 MJPEG 目标下，本次实现将“画面来源”从 MediaProjection 截屏切换为应用内 WebView 绘制结果。  
核心做法是保留现有 `HttpServer` + `bitmapStateFlow` 输出链路，只替换帧输入为 `WebView.draw(Canvas)`。  
本地一次验证过程中出现 Maven Central 超时，最终以 GitHub Actions 成功结果作为真实验收依据。

# Guidance

1. WebView 内容采集不需要 MediaProjection  
只要采集的是应用自己进程内 View 的绘制结果（WebView 属于 app 内视图树），即可通过 `draw(Canvas)` 获取帧，不需要系统级屏幕捕获授权。

2. 保持流媒体链路稳定，仅替换帧源  
沿用 MJPEG 模块现有 `HttpServer` 与 `bitmapStateFlow`，新增 WebView 帧事件路径，将 `Bitmap` 写入统一帧流，避免改动传输层与客户端兼容行为。

3. 音频权限与视频流链路分离  
`RECORD_AUDIO` 仅用于网页 `getUserMedia` 音频采集授权，继续保留同源校验。MJPEG 仍只传图像帧，不承载原生音频流，避免协议边界混淆。

4. CI 作为最终验收来源  
本地依赖下载可受网络波动影响（如 Maven Central 超时），不能直接代表代码正确性。最终以 GitHub Actions 成功为准：  
`https://github.com/lim12137/ScreenStream/actions/runs/25118982163`，且覆盖 `build-fdroid-debug` 与 `build-fdroid-release`。

5. 工具链一致性  
统一使用 JDK 17；Gradle 侧可配置可达性更好的镜像（如华为云镜像）提升稳定性；Actions 使用 `setup-java` 锁定 17，避免环境漂移。

6. 生命周期与性能控制  
在 `onDestroy` 停止帧循环与流服务，防止泄漏。高分辨率 WebView 连续 `draw` 与 Bitmap 分配会增加主线程与 GC 压力，应控制帧率、分辨率与复用策略。

# Why This Matters

- 降低权限复杂度：去除 MediaProjection 交互，减少系统授权失败面。  
- 降低改造风险：复用成熟 MJPEG 输出链路，改动聚焦在采集层。  
- 提升可验证性：把“网络环境问题”与“代码正确性”拆分，CI 给出可复现、可追踪的最终结论。  
- 预防运行时问题：提前明确生命周期与性能边界，减少后续卡顿与泄漏风险。

# When to Apply

- 业务目标是推送应用内 H5/网页画面，而不是整个系统桌面。  
- 已有稳定 MJPEG 输出模块，希望最小改动接入新画面来源。  
- 团队存在本地网络不稳定场景，需要以云端 CI 作为验收主依据。

# Examples

- 画面路径：`WebView.draw(Canvas)` -> `Bitmap` -> `bitmapStateFlow` -> `HttpServer` MJPEG 输出。  
- 音频路径：WebView `getUserMedia(audio)` 由 `RECORD_AUDIO` + 同源策略控制；不并入 MJPEG 视频通道。  
- 验证路径：本地失败日志标注为“依赖下载网络问题”，最终以 Actions Run 成功结论收敛。

# Related

- `docs/webview-mjpeg-verification.md`
- `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/MjpegStreamingService.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/MjpegStreamingModule.kt`
