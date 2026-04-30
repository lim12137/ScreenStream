# 主控端与客户端并行边界说明

## 1) 当前边界

- 当前宿主入口是 `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`。
- `SingleActivity` 已经承担了：
  - `Intent` 命令入口解析
  - 房间/目标切换
  - WebView 宿主容器搭建
  - 前后台生命周期处理
  - MJPEG 模块启动与 WebView 帧推送
- 会话状态机已经在 `common/src/main/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinator.kt` 中单独存在，宿主层只负责调用。
- `mjpeg/src/main/assets/index.html` 是客户端内容页，当前主要负责页面渲染、WebSocket 连接、PIN、重连、回退和页面内展示，不是 Android 宿主层。

## 2) 主控端 V1 最小切片

主控端只保留“宿主真值”能力，不把控制逻辑下沉到页面。

| 最小切片 | 具体职责 | 现有落点 |
| --- | --- | --- |
| 宿主命令入口 | 接收开始房间、切换目标、结束房间 | `SingleActivity.getStartRoomIntent(...)` / `getSwitchTargetIntent(...)` / `getEndRoomIntent(...)` |
| 宿主状态桥 | 把宿主事件翻译成会话更新 | `SingleActivityMeetingHost` |
| 会话真值 | 维护房间、目标、前后台状态机 | `MeetingSessionCoordinator` |
| 原生壳能力 | 管理生命周期、权限、WebView、系统栏、流启动/停止 | `SingleActivity` |

- 主控端的最小目标是跑通“开始 - 切换 - 结束 - 恢复”闭环。
- 主控端不承担页面渲染细节，也不把状态机搬进 `index.html`。

## 3) 客户端 V1 继续完善的最小切片

这里的“客户端”指 `mjpeg/src/main/assets/index.html` 这一侧的 WebView/H5 内容端。

| 最小切片 | 具体职责 |
| --- | --- |
| 页面渲染 | 负责流页面、状态区、提示区、按钮区的视觉输出 |
| 连接管理 | 负责 WebSocket 连接、heartbeat、重连、恢复提示 |
| 访问控制 | 负责 PIN 输入、校验结果展示、封禁/拒绝提示 |
| 画面承载 | 负责 MJPEG 画面展示、JPEG 回退、可选的 PiP / 全屏控制 |

- 客户端 V1 继续完善的边界是“把连接和展示做稳”，不是“接管主控权”。
- 客户端只消费宿主下发的状态和地址，不定义房间真值。
- 客户端可以继续增强页面体验，但不能新增一套和宿主并行的控制状态机。

## 4) 两条线的共享复用点

| 共享点 | 主控端复用方式 | 客户端复用方式 |
| --- | --- | --- |
| 房间 / 目标 / 入口地址 | 由 `SingleActivity` 和 `MeetingSessionCoordinator` 统一管理 | 作为页面连接和显示所需参数使用 |
| 会话状态 | 作为唯一真值来源 | 作为 UI 状态、提示文案和重连状态来源 |
| MJPEG 通道 | 通过 `MjpegStreamingModule` 启停流与帧推送 | 通过 WebSocket / 画面地址接收流 |
| 状态枚举与消息类型 | 通过宿主事件和更新对象流转 | 通过页面消息处理、错误提示和恢复逻辑消费 |

## 5) 当前明确不做的内容

- 不把主控逻辑迁移到 `mjpeg/src/main/assets/index.html`。
- 不在页面层新增一套房间状态机。
- 不让客户端接管宿主生命周期、权限、任务栈或系统栏控制。
- 不重写 `mjpeg` 模块的流模型或事件模型。
- 不做 WebRTC / RTSP 相关回归性改造。
- 不做与本次边界无关的 UI 重构或业务代码改动。

## 6) 结论

- 主控端 V1 和客户端 V1 继续并行推进，但边界是分开的。
- 主控端先稳住宿主真值和命令闭环，客户端继续补连接、展示和恢复能力。
- 两条线只共享状态与通道，不共享控制职责。
