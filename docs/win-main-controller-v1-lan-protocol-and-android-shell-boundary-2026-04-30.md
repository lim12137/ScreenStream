# Win 主控 V1 的 LAN 控制协议与 Android 壳边界

> 说明：本文档定义的是可实施版本的控制面边界。它优先于旧的 viewer / 顶栏相关描述，且不允许回到 `mjpeg/src/main/assets/index.html`、`/socket`、`/start-stop` 这一套协议基础上。

## 1) 目标与结论

- 目标：先落一个局域网可用的 Win 主控 V1，覆盖“开始 / 切换 / 结束 / 查看状态 / 失败回显”的最小闭环。
- 结论：Win 主控页是独立控制页，不是 viewer 页面换皮。
- 结论：控制 API 可以复用现有单端口 Ktor，但控制边界必须通过 `ControllerCommandGateway` 独立收口。
- 结论：Android / Harmony 端继续作为会话真值和流媒体宿主，但不直接把 `MeetingSessionState` 暴露给 Win 主控。
- 结论：主控鉴权必须独立于 viewer PIN。

## 2) 控制面分层与落点

| 层 | 负责什么 | 不负责什么 |
| --- | --- | --- |
| `common` | `ControllerCommandGateway` 接口、`ControllerSessionSnapshot`、命令 / 结果 DTO、状态投影规则 | 网络监听、页面渲染、具体 HTTP 实现 |
| `app` | gateway 实现、Ktor 挂载、DI 组装、会话协调器接入、鉴权装配 | viewer 页面 DOM、`mjpeg` 具体实现细节 |
| `mjpeg` | 仅保留 MJPEG 推流和 viewer 侧历史路由 | 主控命令、主控路由、主控鉴权 |
| Win 主控页 | 独立路由、独立消息模型、控制台 UI | 直接读取 `MeetingSessionState`、复用 viewer 通道 |

- 推荐继续复用现有 Ktor 运行时和单端口监听，但控制路由必须独立挂载。
- `ControllerCommandGateway` 是控制命令的唯一边界，路由层只负责把 HTTP / JSON 请求翻译成 gateway 调用。
- `mjpeg -> app` 不能建立反向依赖；如果 `mjpeg` 需要接入控制能力，只能依赖 `common` 的接口和 DTO，并通过 DI 注入具体实现。

## 3) 独立消息模型

### 3.1 读模型

`ControllerSessionSnapshot` 是 Win 主控页的唯一读模型，不直接暴露 `MeetingSessionState`。

最小字段必须包含：

- `sessionStatus`
- `streamStatus`
- `lastError`
- `hostVisibility`
- `target`
- `updatedAt`
- `ownerControllerId`
- `lastAppliedCommandId`

建议同时在快照或其外层元数据中携带：

- `controllerSessionId`
- `stateVersion`

### 3.2 字段语义

| 字段 | 语义 |
| --- | --- |
| `sessionStatus` | 宿主会话态的控制面投影，不直接暴露内部 state 类名 |
| `streamStatus` | 流媒体 / 输出态投影，和会话态解耦 |
| `lastError` | 最近一次拒绝、失败或运行时错误的可读原因 |
| `hostVisibility` | 当前宿主前后台可见性 |
| `target` | 当前目标信息，至少包含 `roomId`、`targetId`、`entryUrl` |
| `updatedAt` | 快照最后一次刷新时间 |
| `ownerControllerId` | 当前持有控制权的控制端标识 |
| `lastAppliedCommandId` | 最近一次真正生效或已确认消费的命令 ID |

### 3.3 状态投影规则

| 内部状态 | `sessionStatus` 投影 | `streamStatus` 投影 | 说明 |
| --- | --- | --- | --- |
| `Idle` | `idle` | `stopped` | 无房间、无控制权 |
| `Active` | `active` | `starting` / `live` | 取决于流是否已确认就绪 |
| `Ending` | `ending` | `stopping` | 房间即将释放 |
| `StartRejected` | `start-rejected` | `failed` / `stopped` | 失败必须回写到 `lastError` |

- `MeetingSessionState` 只能留在内部协调器和投影器里，不允许直接作为 Win 主控 API 输出。
- `streamStatus` 不要被弱化成布尔值，至少要区分启动中、直播中、停止中、失败。

## 4) 命令模型与幂等语义

### 4.1 请求字段

每个控制命令都必须携带：

- `commandId`
- `controllerSessionId`
- `stateVersion`
- `source`
- 命令动作和 payload

### 4.2 `source` 分类

| source | 语义 |
| --- | --- |
| `controller-command` | 来自 Win 主控页 / 主控 API 的显式控制命令 |
| `local-manual` | 来自 Android 本地 UI、系统回调、手工恢复、自动 attach / auto start 的本地入口 |

- 所有会改状态的入口都必须先标记 source，再进入统一收口。
- 自动 attach / 自动 start 不能绕过 source 标记直接调用协调器。
- 当 `controller-command` 已持有房间控制权时，`local-manual` 的抢占性启动必须被拒绝或降级为 no-op。

### 4.3 结果语义

| 结果 | 语义 |
| --- | --- |
| `applied` | 命令生效，状态发生变化 |
| `already-applied` | 相同 `commandId` 已处理过，返回之前的结果 |
| `rejected-conflict` | 命令合法但与当前控制权、房间归属或运行状态冲突 |
| `rejected-unauthorized` | 鉴权失败、权限不足、token 失效 |
| `rejected-stale` | `stateVersion` 过旧或并发前置条件不满足 |
| `noop` | 命令合法，但不会导致任何状态变化 |

### 4.4 幂等与版本

- `commandId` 用于请求去重，重复提交必须返回 `already-applied` 或等价的已处理结果。
- `stateVersion` 用于乐观并发控制，任何写命令都必须带版本前置条件。
- 每次真正改变控制面状态时，`stateVersion` 必须单调递增。
- `lastAppliedCommandId` 记录最近一次真正落地的命令，方便 Win 主控刷新和恢复。

## 5) 鉴权边界

- viewer PIN 与主控鉴权是两个不同的凭据空间。
- viewer PIN 只能覆盖 viewer 读面，不能直接拿去写控制面。
- V1 建议采用“配对 token -> Bearer token”的两段式主控鉴权。
- `pairing token` 用于首次配对或短期换发。
- `Bearer token` 用于后续 API 调用，必须携带明确的读写权限边界。

### 5.1 权限边界

| 权限 | 可做什么 | 不可做什么 |
| --- | --- | --- |
| `read` | 拉取 `ControllerSessionSnapshot` | 发送 start / switch / end 命令 |
| `write` | 拉取 snapshot 并发送控制命令 | 直接跳过版本检查或控制权检查 |

- 主控 token 必须绑定 `controllerSessionId`，并能追踪 `ownerControllerId`。
- 同一时刻允许读取的客户端可以多于一个，但写入权必须严格收口到当前控制权持有者。

## 6) Android / Harmony 壳边界

- Android / Harmony 端继续负责宿主生命周期、前台服务、权限、流媒体启停和最终释放。
- 宿主壳不再新增第二套控制台，也不再把控制逻辑下沉到 viewer 页面。
- 控制入口必须统一进入一条收口链路，再决定是否调用 `MeetingSessionCoordinator`。
- 任何自动 attach / 自动 start 都必须作为 `local-manual` 入口处理，不能被当成独立的业务真值。
- 当 Win 主控已持有房间时，本地自动恢复只能做状态恢复，不能偷偷把房间重新开走。

## 7) 失败桥接

必须打通下面这条链路：

`ForegroundStartFailed` -> `StartRejected` -> `ControllerSessionSnapshot.lastError`

- 当前台启动、通知、权限或宿主条件失败时，服务侧先产生 `ForegroundStartFailed`。
- 协调器将状态收敛到 `StartRejected`。
- `ControllerSessionSnapshot.lastError` 必须同步表达这次失败，不能只在日志里出现。
- 失败快照要带上和命令关联的上下文，至少包含 `commandId`、`controllerSessionId`、`source`、`roomId`、`targetId`、错误代码和错误文本。
- 主控页收到失败结果后，必须立即停止把自己当成“已开房可继续操作”的状态。

## 8) 实施顺序

1. 先在 `common` 定义 `ControllerCommandGateway`、`ControllerSessionSnapshot`、`ControllerCommand`、`ControllerCommandResult` 和版本字段。
2. 再在 `app` 里把 gateway 实现接到现有 `MeetingSessionCoordinator` 和宿主壳。
3. 然后把控制路由挂到现有 Ktor 监听上，确保控制面和 viewer 面分路。
4. 接着补鉴权、权限、幂等、版本检查和失败桥接。
5. 最后实现 Win 主控页，并用契约测试 + 集成测试把 `start -> stream live` 闭环跑通。

## 9) 测试策略

| 测试类型 | 覆盖点 | 通过标准 |
| --- | --- | --- |
| controller API 契约测试 | request / response 结构、结果枚举、版本语义 | 结果码和字段和文档一致 |
| 鉴权测试 | viewer PIN 不能写、Bearer scope、过期 token、controllerSessionId 绑定 | 写权限和读权限严格分离 |
| 幂等测试 | `commandId` 重复提交、`stateVersion` 并发冲突、重复回放 | 返回 `already-applied` / `rejected-stale` 符合预期 |
| 失败桥接测试 | `ForegroundStartFailed -> StartRejected -> lastError` | snapshot 和命令结果一致 |
| 集成 / E2E 测试 | `start -> stream live`、switch、end、刷新恢复 | 控制页和宿主都能闭环 |

## 10) 当前明确不做

- 不用 `mjpeg/src/main/assets/index.html` 作为主控协议基础。
- 不用 `/socket` 和 `/start-stop` 作为 Win 主控 API 的语义基础。
- 不把 viewer PIN 直接升级成主控鉴权。
- 不让 Win 主控直接依赖 `MeetingSessionState`。
- 不把 `mjpeg` 改造成控制面宿主的反向依赖入口。
