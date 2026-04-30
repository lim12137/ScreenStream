# Win 主控 V1 的 LAN 控制协议草案 + Android 壳裁剪边界

## 1) 目标与前提

- 目标：先落一个能在局域网内跑通的 Win 主控 V1，把“开始 / 切换 / 结束 / 查看状态”做成最小闭环。
- 前提：Android / Harmony 端继续作为会话真值和流媒体宿主，`MeetingSessionCoordinator` 仍是状态机核心，不在 Win 侧另造一套状态机。
- 前提：当前产品方向仍是局域网优先、单模式 MJPEG，不引入云中继、WebRTC、RTSP 作为 V1 前提。
- 前提：本草案只定义控制面，不改业务代码，不重画现有宿主结构。

## 2) 为什么 Win 主控 V1 先用浏览器 / H5

- 交付快：先用浏览器 / H5 可以直接做页面、调试和联机验证，减少 Win 原生壳和打包依赖。
- 联机简单：局域网控制面本质是少量命令 + 状态回传，H5 足够承载，不需要先引入复杂桌面框架。
- 便于复用：后续同一套页面逻辑可以复用到桌面浏览器、内嵌 WebView 或测试页。
- 便于对齐现有实现：当前仓库已经有明确的会话状态机和网页内容面，Win 侧先做浏览器控制台更容易和现有状态语义对齐。
- 便于裁剪：V1 先证明“控制闭环可用”，再决定是否做原生 Win 宿主，不把 UI 复杂度提前锁死。

## 3) 协议草案

### 3.1 协议定位

- 传输建议：LAN 内 `HTTP + JSON`。
- 控制原则：Win 侧只发命令和读状态，不持有房间真值。
- 状态原则：Android / Harmony 端返回的状态快照是唯一可信结果，Win 侧只做展示和发令。

### 3.2 接口语义

| 接口 / 消息 | 方向 | 语义 | 对应 `MeetingSessionEvent` |
| --- | --- | --- | --- |
| `state` | Android / Harmony -> Win | 读取当前会话快照；用于页面初始化、刷新、重连后恢复 | 不直接映射事件；它投影的是当前 `MeetingSessionState` |
| `start` | Win -> Android / Harmony | 创建或进入一个房间，带 `roomId`、`targetId`、`entryUrl` | `MeetingSessionEvent.StartRoom(roomId, targetId, entryUrl)` |
| `switch` | Win -> Android / Harmony | 在同一 `roomId` 内切换当前目标，带新的 `targetId`、`entryUrl` | `MeetingSessionEvent.SwitchTarget(roomId, nextTargetId, nextEntryUrl)` |
| `end` | Win -> Android / Harmony | 显式结束当前房间，默认是控制端主动结束 | `MeetingSessionEvent.EndRoom(roomId, reason = CONTROLLER_EXPLICIT)` |

### 3.3 状态返回建议

- `state` 至少返回：`roomId`、`status`、`targetId`、`entryUrl`、`hostVisibility`、`reason`。
- `status` 建议直接对齐现有状态语义：`Idle`、`Active`、`Ending`、`StartRejected`。
- `switch` 只允许作用于当前活动 `roomId`，否则应返回冲突结果，不得隐式拉起新房间。
- `end` 只允许显式结束，不得由页面刷新、后台切换或宿主重建自动触发。

### 3.4 失败与拒绝

- 如果前台服务、通知或宿主条件失败，Android / Harmony 端应把失败收敛为 `ForegroundStartFailed` 语义。
- 该失败语义映射到 `MeetingSessionEvent.ForegroundStartFailed(roomId?, reason)`，并让当前会话进入 `StartRejected`。
- Win 侧看到 `StartRejected` 后，应停止继续认为自己处于“开会中”。

## 4) Android / Harmony 端保留职责

- 保留会话真值：`MeetingSessionCoordinator` 继续负责房间、目标、前后台可见性和结束状态。
- 保留宿主生命周期：`onResume`、`onStop`、前台服务、资源回收和最终释放路径仍在宿主侧处理。
- 保留流媒体能力：MJPEG 采集、输出、启动、停止和帧推送仍由宿主完成。
- 保留错误回传：前台启动失败、权限失败、房间冲突等结果由宿主统一回传。
- 保留状态投影：宿主对外只输出会话快照，不把控制逻辑下沉到页面里。

## 5) Android 宿主壳应裁剪 / 停止继续扩展的内容

- 不再扩一套并行控制台：宿主壳不应再长出和 Win 主控重复的管理 UI。
- 不再复制状态机：房间状态、目标状态、结束状态只保留一份真值来源。
- 不再引入云依赖：不新增云端中继、账号体系、远程登录作为 V1 前提。
- 不再回退到 RTSP / WebRTC 主路径：这两条线继续作为历史残留或清理对象，不作为 Win 主控 V1 的控制面基础。
- 不再把复杂控制逻辑塞进页面：H5 / WebView 只做展示和局域网控制的轻量入口，不接管宿主职责。
- 不再做超出 V1 的壳扩展：例如多入口编排、复杂多房间管理、跨设备编组、权限向导集成等，先全部收口。

## 6) 实施顺序建议

1. 先冻结协议词汇：`state / start / switch / end` 和返回状态枚举先定死。
2. 再对齐状态机映射：确认每个命令如何落到 `MeetingSessionEvent`，以及哪些场景必须拒绝。
3. 然后做 Win 浏览器 / H5 控制页：先出可点可看可回显的最小页面。
4. 再接 Android / Harmony 宿主适配：把命令接到现有会话协调器和流媒体宿主。
5. 最后做真机局域网联调：确认状态回读、切换、结束、失败回传都能闭环。

## 7) 参考的现有实现

- `common/src/main/java/info/dvkr/screenstream/common/session/MeetingSessionState.kt`
- `common/src/main/java/info/dvkr/screenstream/common/session/MeetingSessionCoordinator.kt`
- `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`
- `docs/main-controller-boundary-2026-04-30.md`
