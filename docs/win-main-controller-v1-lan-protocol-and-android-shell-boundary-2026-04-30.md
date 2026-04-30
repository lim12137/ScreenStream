# Win 主控 V1 的 LAN 控制协议与 Android 壳边界

> 说明：本文档定义的是可实施版本的控制面边界。它优先于旧的 viewer / 顶栏相关描述，且不允许回到 `mjpeg/src/main/assets/index.html`、`/socket`、`/start-stop` 这一套协议基础上。

## 1) 目标与结论

- 目标：先落一个局域网可用的 Win 主控 V1，覆盖“开始 / 切换 / 结束 / 查看状态 / 失败回显”的最小闭环。
- 结论：Win 主控页是独立控制页，不是 viewer 页面换皮。
- 结论：控制 API 必须复用现有单端口 `mjpeg` Ktor 宿主，并由 `app` 注入 `ControllerCommandGateway` 后挂载 `/controller/v1` 路由，控制边界仍然独立收口。
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

快照首包必须携带以下必带字段：

- `controllerSessionId`
- `stateVersion`
- `lastAppliedCommandId`
- `updatedAt`
- `ownerControllerId`

快照主体还必须携带以下控制面字段：

- `sessionStatus`
- `streamStatus`
- `hostVisibility`
- `target`
- `lastError`

### 3.2 字段语义

| 字段 | 语义 |
| --- | --- |
| `controllerSessionId` | 当前控制会话标识，snapshot 和首包都必须带 |
| `stateVersion` | 控制面乐观并发版本号，写命令和快照都必须带 |
| `lastAppliedCommandId` | 最近一次真正生效或已确认消费的命令 ID |
| `updatedAt` | 快照最后一次刷新时间 |
| `ownerControllerId` | 当前持有控制权的控制端标识 |
| `sessionStatus` | 宿主会话态的控制面投影，不直接暴露内部 state 类名 |
| `streamStatus` | 流媒体 / 输出态投影，和会话态解耦 |
| `hostVisibility` | 当前宿主前后台可见性，idle 时可为空 |
| `target` | 当前目标信息，至少包含 `roomId`、`targetId`、`entryUrl` |
| `lastError` | 最近一次拒绝、失败或运行时错误的可读原因 |

#### 3.2.1 复合字段约束

- `target` 为可空对象；当 `sessionStatus=idle` 时可以为空。
- `lastError` 为可空对象；当 `sessionStatus=start-rejected` 或 `streamStatus=failed` 时必须带值。
- `ownerControllerId` 为可空字段；未配对或未抢到控制权时应返回 `null`，但字段本身必须出现。

### 3.3 状态投影规则

| 内部状态 | `sessionStatus` 投影 | `streamStatus` 投影 | 说明 |
| --- | --- | --- | --- |
| `Idle` | `idle` | `stopped` | 无房间、无控制权 |
| `Active` | `starting` / `active` | `starting` / `live` | `starting` 表示命令已接收但首帧或前台确认未完成 |
| `Ending` | `ending` | `stopping` | 房间即将释放，停止过程未完成时保持 `stopping` |
| `StartRejected` | `start-rejected` | `failed` | 失败必须回写到 `lastError` |

- `sessionStatus` 代表房间与控制权状态，回答“谁在管、命令到哪一步”。
- `streamStatus` 代表出流准备与媒体态，回答“流是否真的可用”。
- `sessionStatus` 可以是 `active` 但 `streamStatus` 仍是 `starting`；两者不能再混成一个布尔值。
- `MeetingSessionState` 只能留在内部协调器和投影器里，不允许直接作为 Win 主控 API 输出。

### 3.4 HTTP 契约层

控制 API 只暴露 5 个端点，统一挂在单端口 `mjpeg` Ktor 宿主下的 `/controller/v1` 前缀。

| Endpoint | Method | 请求体 | 响应体 | 说明 |
| --- | --- | --- | --- | --- |
| `/controller/v1/snapshot` | `GET` | 无 | `ControllerSessionSnapshot` | 首次加载和刷新使用 |
| `/controller/v1/start` | `POST` | `ControllerStartRequest` | `ControllerCommandResult` | 开始房间 |
| `/controller/v1/switch` | `POST` | `ControllerSwitchRequest` | `ControllerCommandResult` | 切换当前目标 |
| `/controller/v1/end` | `POST` | `ControllerEndRequest` | `ControllerCommandResult` | 显式结束房间 |
| `/controller/v1/token/exchange` | `POST` | `ControllerTokenExchangeRequest` | `ControllerTokenExchangeResult` | 配对 token 换发 Bearer token，统一口径，不再另写别名 |

#### 3.4.1 `ControllerStartRequest`

必填字段：

- `commandId`
- `controllerSessionId`
- `stateVersion`
- `roomId`
- `targetId`
- `entryUrl`

可选字段：

- `source`，默认 `controller-command`；HTTP 层若传入则必须为 `controller-command`

#### 3.4.2 `ControllerSwitchRequest`

必填字段：

- `commandId`
- `controllerSessionId`
- `stateVersion`
- `roomId`
- `nextTargetId`
- `nextEntryUrl`

可选字段：

- `source`，默认 `controller-command`

#### 3.4.3 `ControllerEndRequest`

必填字段：

- `commandId`
- `controllerSessionId`
- `stateVersion`
- `roomId`

可选字段：

- `reason`，默认 `controller-explicit`
- `source`，默认 `controller-command`

#### 3.4.4 `ControllerTokenExchangeRequest`

必填字段：

- `pairingToken`
- `controllerSessionId`

可选字段：

- `deviceName`
- `clientNonce`

#### 3.4.5 `ControllerCommandResult`

必带字段：

- `commandId`
- `controllerSessionId`
- `stateVersion`
- `result`
- `snapshot`

可选字段：

- `error`

`result` 取值统一为：

- `applied`
- `already-applied`
- `noop`
- `rejected-unauthorized`
- `rejected-stale`
- `rejected-conflict`
- `rejected-unavailable`

`error` 的字段和 `snapshot.lastError` 保持同构，用于把命令失败直接带回主控页。

#### 3.4.6 `ControllerTokenExchangeResult`

必带字段：

- `controllerSessionId`
- `ownerControllerId`
- `bearerToken`
- `expiresAt`

可选字段：

- `stateVersion`
- `scope`

#### 3.4.7 状态码表

| HTTP 状态码 | 适用场景 | 典型 `result` |
| --- | --- | --- |
| `200 OK` | snapshot 成功、命令已应用、幂等重放命中、token 换发成功 | `applied` / `already-applied` / `noop` |
| `400 Bad Request` | JSON 格式错误、缺少必填字段、枚举非法、字段类型不对 | 无 |
| `401 Unauthorized` | bearer token 失效、pairing token 失效、鉴权头缺失 | `rejected-unauthorized` |
| `403 Forbidden` | scope 不足、controllerSessionId 不匹配、非当前 owner 试图写入 | `rejected-unauthorized` / `rejected-conflict` |
| `409 Conflict` | `stateVersion` 过旧、roomId 不一致、并发抢占冲突 | `rejected-stale` / `rejected-conflict` |
| `503 Service Unavailable` | foreground 拉起失败、宿主/前台服务不可用、流启动无法完成 | `rejected-unavailable` |

- `ControllerCommandGateway` 负责把 HTTP 请求翻译成上述结果，不在路由层拼业务状态。
- `GET /controller/v1/snapshot` 不需要 request body，只返回当前投影快照。
- `start` / `switch` / `end` 的成功响应都必须带回最新 `snapshot`，保证主控首包可恢复。

## 4) 命令模型与幂等语义

### 4.1 请求字段

每个控制命令都必须携带：

- `commandId`
- `controllerSessionId`
- `stateVersion`
- `source`
- 命令动作和 payload

- HTTP 请求里 `source` 可以默认补成 `controller-command`，但内部命令 DTO 里它必须显式存在。
- 读命令只需要 `controllerSessionId` 和鉴权，不需要 `commandId`；写命令必须完整带齐幂等字段。

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
| `rejected-unavailable` | 前台服务、流宿主或控制宿主暂时不可用 |
| `noop` | 命令合法，但不会导致任何状态变化 |

### 4.4 幂等与版本

- `commandId` 用于请求去重，重复提交必须返回 `already-applied` 或等价的已处理结果。
- `stateVersion` 用于乐观并发控制，任何写命令都必须带版本前置条件。
- 每次真正改变控制面状态时，`stateVersion` 必须单调递增。
- `lastAppliedCommandId` 记录最近一次真正落地的命令，方便 Win 主控刷新和恢复。

## 5) 鉴权边界

- viewer PIN 与主控鉴权是两个不同的凭据空间。
- viewer PIN 只能覆盖 viewer 读面，不能直接拿去写控制面。
- V1 统一采用 `POST /controller/v1/token/exchange` 做配对 token 换发，响应中发放 Bearer token。
- `pairingToken` 只用于换发，不得直接进入 start / switch / end 路由。
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

事件源、收口者和投影者必须分工固定：

| 环节 | 责任方 | 必做动作 |
| --- | --- | --- |
| 事件源 | `mjpeg` 内的前台服务 / 流宿主 | 只在前台启动、通知、权限或宿主条件失败时发出 `ForegroundStartFailed` |
| 收口者 | `app` 内的 `ControllerCommandGateway` 适配层 | 立即把失败事件喂给 `MeetingSessionCoordinator.handleEvent(...)` |
| 状态收敛 | `MeetingSessionCoordinator` | 把当前态收敛成 `StartRejected`，保留 `roomId`、`lastTarget` 和失败原因 |
| 投影者 | `ControllerCommandGateway` | 把 `StartRejected` 投影为 `streamStatus=failed`、`sessionStatus=start-rejected`，并写入 `ControllerSessionSnapshot.lastError` |

- 失败必须在当前写命令响应内闭环，不能拖到下一次轮询。
- `lastError` 必须包含 `commandId`、`controllerSessionId`、`source`、`roomId`、`targetId`、错误代码、错误文本和 `occurredAt`。
- `sessionStatus` 与 `streamStatus` 必须同时更新，不能只改日志或只改一个字段。
- 主控页收到失败结果后，必须立即停止把自己当成“已开房可继续操作”的状态。

## 8) 实施顺序

1. 先在 `common` 定义 `ControllerCommandGateway`、`ControllerSessionSnapshot`、`ControllerCommand`、`ControllerCommandResult` 和版本字段。
2. 再在 `app` 里把 gateway 实现接到现有 `MeetingSessionCoordinator` 和宿主壳。
3. 然后把控制路由挂到现有 Ktor 监听上，确保控制面和 viewer 面分路。
4. 接着补鉴权、权限、幂等、版本检查和失败桥接。
5. 最后实现 Win 主控页，并用契约测试 + 集成测试把 `start -> stream live` 闭环跑通。

## 9) 测试策略

| 测试类型 | 覆盖点 | 对应接口 / 状态 | 通过标准 |
| --- | --- | --- | --- |
| controller API 契约测试 | request / response 结构、结果枚举、版本语义 | `GET /snapshot`、`POST /start`、`POST /switch`、`POST /end`、`POST /token/exchange` | 结果码、必填字段、DTO 结构和文档一致 |
| 鉴权测试 | viewer PIN 不能写、Bearer scope、过期 token、controllerSessionId 绑定 | 所有 `/controller/v1/*` 写接口 | 写权限和读权限严格分离 |
| 幂等测试 | `commandId` 重复提交、`stateVersion` 并发冲突、重复回放 | `POST /start`、`POST /switch`、`POST /end` | 返回 `already-applied` / `rejected-stale` 符合预期 |
| 失败桥接测试 | `ForegroundStartFailed -> StartRejected -> lastError` | `POST /start` + `GET /snapshot` | snapshot、命令结果、`lastError` 一致 |
| 状态投影测试 | `starting/live/stopping/failed` 的判定来源 | `sessionStatus` / `streamStatus` | `sessionStatus` 与 `streamStatus` 不再混用 |
| 集成 / E2E 测试 | `start -> stream live`、switch、end、刷新恢复 | 控制页和宿主都能闭环 | 控制页和宿主都能闭环 |

## 10) 当前明确不做

- 不用 `mjpeg/src/main/assets/index.html` 作为主控协议基础。
- 不用 `/socket` 和 `/start-stop` 作为 Win 主控 API 的语义基础。
- 不把 viewer PIN 直接升级成主控鉴权。
- 不让 Win 主控直接依赖 `MeetingSessionState`。
- 不把 `mjpeg` 改造成控制面宿主的反向依赖入口。
