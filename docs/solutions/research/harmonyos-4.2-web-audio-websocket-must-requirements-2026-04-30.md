---
title: 鸿蒙4.2上网页录音与WebSocket稳定保活的必须要求
date: 2026-04-30
category: docs/solutions/research/
module: app,mjpeg
problem_type: research
severity: medium
tags:
  - harmonyos-4.2
  - webview
  - getusermedia
  - record-audio
  - websocket
  - heartbeat
  - keepalive
---

# 鸿蒙4.2上网页录音与WebSocket稳定保活的必须要求

## 1) 背景与问题定义

本仓库当前的宿主实现是 Android `WebView` + 本地 `MJPEG` 服务，但目标讨论点是鸿蒙 4.2 上的网页录音与 WebSocket 稳定保活。

这里的核心问题不是“网页能否打开”，而是两条链路能否同时稳定成立：

1. 网页音频采集链路
   - 页面发起麦克风采集。
   - 宿主 WebView 正确接收并处理权限请求。
   - 系统运行时权限和网页权限在目标设备上都能闭环。

2. WebSocket 控制链路
   - 页面和服务端之间的连接在长时间空闲、前后台切换、弱网和省电策略下仍可恢复。
   - 心跳不依赖“连接看起来还开着”，而要有明确的 ping/ack/reconnect 机制。

对这个项目来说，必须把“网页录音”与“WebSocket 保活”分开看：

- 网页录音属于浏览器/WebView 的权限与安全上下文问题。
- WebSocket 保活属于传输层与页面生命周期问题。

## 2) 已证实的关键约束/事实

### 平台侧事实

- WebView 中的网页权限不是自动放行的，宿主需要处理 `WebChromeClient.onPermissionRequest`，并显式 `grant()` 或 `deny()`。
- `PermissionRequest.RESOURCE_AUDIO_CAPTURE` 对应网页音频采集能力。
- 网页麦克风采集通常还需要宿主侧的运行时麦克风权限，也就是 `RECORD_AUDIO`。
- `getUserMedia()` 属于受安全上下文约束的 Web API；标准浏览器环境下通常要求 HTTPS 或等价的安全来源。
- WebSocket 本身没有“自动稳定保活”能力，应用层必须自己做 heartbeat / pong / reconnect。

### 仓库侧事实

- `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`
  - 已实现 `WebChromeClient.onPermissionRequest`。
  - 已把 `RESOURCE_AUDIO_CAPTURE` 和 `RECORD_AUDIO` 打通。
  - 已做可信来源校验 `isTrustedAudioPermissionOrigin(...)`。
  - 已在 `onDestroy()` 停止 WebView 帧循环并回收网页录音流。
- `mjpeg/src/main/assets/index.html`
  - WebSocket 连接写死为 `ws://`。
  - 客户端心跳参数是 `pingTimeout=1000`、`pongTimeout=1000`、`reconnectTimeout=2000`。
  - 客户端会在收到服务端 `HEARTBEAT` 时继续维持连接。
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/HttpServer.kt`
  - 服务端对 `HEARTBEAT` 会回发同名消息，说明当前 keepalive 依赖应用层心跳协议。
- `app/src/main/AndroidManifest.xml`
  - 已声明 `INTERNET` 与 `RECORD_AUDIO`。
- `mjpeg/src/main/AndroidManifest.xml`
  - 已声明 `INTERNET`、`FOREGROUND_SERVICE`、`WAKE_LOCK` 等网络与保活相关权限。

## 3) 对本项目的直接影响

1. 网页录音在鸿蒙 4.2 上不能只看“页面写了 `getUserMedia()`”
   - 还要看宿主是否能正确弹出并处理网页权限请求。
   - 还要看系统是否允许运行时麦克风授权。
   - 还要看页面是否处于标准要求的安全上下文。

2. 当前仓库的默认 LAN HTTP 入口对网页录音有天然风险
   - `mjpeg/src/main/assets/index.html` 使用的是 `ws://`。
   - 如果页面仍是普通 `http://` 局域网地址，而目标 WebView 按标准安全上下文规则执行，那么网页录音可能直接失败。
   - 也就是说，网页录音的成败可能不是权限弹窗，而是来源不满足安全条件。

3. WebSocket 的稳定性不能只靠“服务还活着”
   - 当前实现依赖前端 1 秒心跳和 2 秒重连。
   - 如果鸿蒙 4.2 的 WebView 在后台、锁屏或省电策略下暂停 JS 定时器，心跳会失效。
   - 这时必须依赖更宽松的服务端超时、前端恢复重连和页面生命周期回收。

4. 当前主线程压力不小
   - `SingleActivity.kt` 以 100ms 间隔把 `WebView.draw(Canvas)` 结果推给 MJPEG。
   - 同一页面还要处理网页权限和 WebSocket 事件。
   - 在性能紧张的设备上，这会放大误判断线、授权卡顿和帧率抖动。

## 4) 必须条件 vs 增强项

| 级别 | 条件 | 为什么必须 | 当前仓库状态 |
| --- | --- | --- | --- |
| 必须 | 宿主能处理网页权限请求 | 没有 `onPermissionRequest`，网页拿不到音频采集授权 | 已实现于 `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt` |
| 必须 | 宿主侧运行时 `RECORD_AUDIO` 权限 | 没有系统麦克风权限，网页音频授权无法闭环 | 已实现于 `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt` 和 `app/src/main/AndroidManifest.xml` |
| 必须 | 页面处于安全上下文或平台等价安全来源 | 标准 Web Audio / `getUserMedia()` 依赖安全来源 | 当前默认入口仍是 LAN `http://` 风险态 |
| 必须 | WebSocket 有应用层 heartbeat/ack/reconnect | WebSocket 不会自动替你做稳定保活 | 已实现于 `mjpeg/src/main/assets/index.html` 与 `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/HttpServer.kt` |
| 必须 | 页面销毁时回收录音与连接状态 | 避免悬挂请求、泄漏和僵尸连接 | 已在 `SingleActivity.kt` 做了停止与回收 |
| 增强 | 把 `ws://` 升级为 `wss://` | 如果后续转 HTTPS，这能降低混合内容和中间人风险 | 当前未做 |
| 增强 | heartbeat 配置可配置 | 不同设备和网络环境对 1s/1s/2s 的容忍度不同 | 当前是硬编码 |
| 增强 | 使用指数退避重连 | 避免弱网下频繁重连打爆服务端和电量 | 当前未做 |
| 增强 | 监听页面可见性/前后台事件 | 让 JS 定时器暂停和恢复更可控 | 当前未做 |
| 增强 | 服务端也做 idle timeout 和会话复位 | 只靠前端定时器不够稳 | 当前实现偏前端驱动 |

## 5) 需实机验证项

建议在鸿蒙 4.2 真实设备上逐项验证，不要只看模拟器或桌面浏览器：

1. 网页录音是否真的可用
   - `http://` 局域网页面是否能触发麦克风授权。
   - `https://` 页面是否表现不同。
   - `RECORD_AUDIO` 被拒绝、授予、撤销后的页面行为。

2. WebView 权限回调是否完整
   - `onPermissionRequest` 是否能收到 `RESOURCE_AUDIO_CAPTURE`。
   - `grant()` 是否必须在 UI 线程。
   - 页面跳转、刷新、关闭时是否会留下未完成的权限请求。

3. WebSocket 稳定保活
   - 锁屏、息屏、返回桌面、切后台后，连接是否还能按预期恢复。
   - Wi-Fi 切换、IP 变化、弱网抖动时是否能自动重连。
   - 长时间空闲时是否会被系统或中间网络设备断开。

4. 页面生命周期与定时器
   - WebView 进入后台后，JS `setTimeout` / `setInterval` 是否被明显节流。
   - 1 秒心跳是否会因为节流而误判断线。

5. 端到端回归
   - 音频授权成功后，页面录音是否仍能继续推送控制消息。
   - 连接恢复后，网页音频状态和 WebSocket 状态是否同步重建。

## 6) 结合当前仓库代码的宿主风险摘要

- `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`
  - 风险点是网页录音完全依赖 WebView 权限回调 + `RECORD_AUDIO` 的双重闭环。
  - 如果鸿蒙 4.2 的 WebView 实现不稳定地暴露 `RESOURCE_AUDIO_CAPTURE`，这条链路会直接断。
  - 100ms 的 `WebView.draw(Canvas)` 轮询与授权流程共享 UI 线程，设备压力大时会放大卡顿。

- `mjpeg/src/main/assets/index.html`
  - 风险点是 `ws://` 与固定 1s 心跳。
  - 这对纯局域网明文场景可用，但不适合把同一页面迁到 HTTPS 或更严格的 WebView 安全策略。
  - 心跳和重连是纯前端 JS 实现，若 WebView 后台节流，保活会失真。

- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/HttpServer.kt`
  - 风险点是服务端当前只对 `HEARTBEAT` 做回声处理，没有看到更强的会话存活策略。
  - 如果客户端心跳丢失，服务端只能被动等待连接失效。

- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/MjpegStreamingService.kt`
  - 风险点是 WebView 流、MJPEG 状态机和前台服务生命周期绑得很紧。
  - 这对单机局域网流媒体是合理的，但对“网页录音 + WebSocket 长连接”来说，任何前台策略变化都会影响稳定性。

- `app/src/main/AndroidManifest.xml`
  - 风险点是当前权限模型是 Android 语义。
  - 真正落到鸿蒙 4.2 时，必须重新核对网络、麦克风和后台执行的等价要求。

## 7) 参考来源链接

### 官方 / 一手来源

- Huawei Developer codelab: `https://developer.huawei.com/consumer/en/codelab/HarmonyOS-WebView/`
- Android `WebChromeClient` 文档: `https://developer.android.com/reference/android/webkit/WebChromeClient.html`
- Android `PermissionRequest` 文档: `https://developer.android.com/reference/android/webkit/PermissionRequest`
- Android 音频采集文档: `https://developer.android.com/guide/topics/media/audio-capture`
- MDN `getUserMedia()` 文档: `https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia`
- MDN WebSocket 客户端应用文档: `https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_client_applications`
- RFC 6455: `https://www.rfc-editor.org/rfc/rfc6455`

### 仓库代码引用

- `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `mjpeg/src/main/assets/index.html`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/HttpServer.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/MjpegStreamingService.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/MjpegStreamingModule.kt`
