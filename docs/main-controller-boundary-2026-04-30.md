# 主控端逻辑边界说明

## 1) 当前现状

- 当前宿主入口是 `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`。
- `SingleActivity` 已经同时承担了：
  - `Intent` 命令入口解析
  - 房间/目标切换
  - WebView 宿主容器搭建
  - 前后台生命周期处理
  - MJPEG 模块启动与 WebView 帧推送
- 会话状态机已经在 `common/src/main/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinator.kt` 中单独存在，宿主层只是调用它。
- `mjpeg/src/main/assets/index.html` 是 WebView 内容页，当前主要负责页面渲染、WebSocket 连接、PIN、重连和页面内展示，不是 Android 宿主层。

## 2) 已确认问题 / 缺口

- 目前缺口不是“没有主控逻辑”，而是“主控逻辑的边界还没有完全收口”。
- 现在已经有宿主侧主控入口，但边界需要明确成一条线：
  - 宿主层负责决定房间、目标、生命周期和模块启停
  - 页面层只负责展示与页面内交互
- 如果把主控逻辑继续下沉到 `mjpeg/index.html`，会把 Android 侧能力拆散到 WebView 里，后续会出现职责重叠和恢复逻辑不一致的问题。

## 3) 为什么主控端逻辑应落在 `app/SingleActivity` 宿主层，而不是 `mjpeg/index.html`

- `Intent` 路由只能在宿主层完成，`index.html` 不能接管 `getStartRoomIntent`、`getSwitchTargetIntent`、`getEndRoomIntent` 这类入口。
- `onNewIntent`、`onResume`、`onPause`、`onStop`、`onDestroy` 都是 Android 宿主生命周期，页面层无法成为可靠真值来源。
- `SingleActivity` 需要直接控制 WebView、系统栏、权限请求和模块启停，这些都属于原生壳职责，不适合放进 HTML。
- `MeetingSessionCoordinator` 已经是 native 状态机，主控逻辑继续放在宿主层，才能保证“状态机 + 生命周期 + 页面加载”共用同一条状态链。
- `index.html` 即使能通过 JS 表现出控制面，也只能算页面内状态，不能替代宿主层的权限、任务栈和恢复逻辑。

## 4) 现有可复用入口清单

| 入口 | 位置 | 用途 | 复用建议 |
| --- | --- | --- | --- |
| room intent | `SingleActivity.getStartRoomIntent(...)` / `getSwitchTargetIntent(...)` / `getEndRoomIntent(...)` | 对外提供房间开始、切目标、结束的宿主命令入口 | 继续作为主控入口，不新增页面侧路由 |
| `meetingHost` | `SingleActivityMeetingHost` | 把宿主事件翻译成会话更新 | 继续作为 `SingleActivity` 的主控适配层 |
| `coordinator` | `MeetingSessionCoordinator` | 维护房间/目标/前后台状态机 | 保持为单一状态真值来源 |
| `mjpeg` module | `MjpegStreamingModule` 及其流服务 | 负责 MJPEG 流和 WebView 帧通道 | 只承接流与帧，不承接主控状态 |

## 5) 下一步主控端逻辑的最小范围建议

- 只在 `app/SingleActivity` 宿主层继续补主控逻辑。
- 继续复用现有 `meetingHost + coordinator` 链路，不把状态机搬进 `index.html`。
- 若要新增主控操作，优先加到 `SingleActivity` 的原生顶栏或宿主菜单里，再映射到现有 room intent。
- `mjpeg` 只保留流、帧、WebSocket 和页面内容相关能力，不扩展成房间控制中心。
- 新增逻辑以“能跑通宿主主控闭环”为最小目标，不做跨层重构。

## 6) 暂不做的内容

- 不把主控逻辑迁移到 `mjpeg/src/main/assets/index.html`。
- 不在页面层新增一套房间状态机。
- 不重写 `mjpeg` 模块的流模型或事件模型。
- 不做 WebRTC / RTSP 相关回归性改造。
- 不做与本次主控边界无关的 UI 重构或业务代码改动。
