# Win 主控边界：独立控制页与客户端页分离

## 1) 结论

- Win 主控 V1 必须是独立控制页、独立路由、独立消息模型。
- Win 主控页不能复用 `mjpeg/src/main/assets/index.html`、`/socket`、`/start-stop` 作为协议基础。
- `mjpeg/src/main/assets/index.html` 继续只作为 viewer / 内容页，不承担主控协议、主控鉴权和主控状态真值。
- 主控页只消费 `ControllerSessionSnapshot` 和 `ControllerCommandResult`，不直接读取或暴露 `MeetingSessionState`。

## 2) 当前仓库中的真实边界

| 层 | 现状职责 | 说明 |
| --- | --- | --- |
| `app` | 宿主壳、页面容器、生命周期、DI 组装 | 这里是 Android / Harmony 宿主侧的边界，不是 Win 主控页本身 |
| `common` | 会话协调器、共享模型、共享设置 | `MeetingSessionCoordinator` 仍是当前会话真值来源 |
| `mjpeg` | MJPEG 推流与 viewer 侧 HTTP / WebSocket | 现有 `/socket`、`/start-stop` 仍属于 viewer 侧历史实现残留 |
| Win 主控 V1 | 独立控制页 + 独立协议入口 | 这是新增控制面，不是 `mjpeg` 页面换皮 |

- 当前仓库里，`MeetingSessionCoordinator` 只负责会话状态机，不负责控制页渲染。
- 当前 `mjpeg/src/main/assets/index.html` 只是 viewer 内容页，它可以继续演化，但不能成为 Win 主控协议的基础页。
- 任何未来控制面实现都必须通过独立路由和独立消息模型进入，不得通过 viewer 页面脚本直接接管主控。

## 3) Win 主控 V1 的边界要求

### 3.1 独立路由

- Win 主控页必须挂在自己的控制路由下，例如 `/controller/v1/*` 这一类前缀。
- 控制页路由和 viewer 路由必须分开注册，不能用 `mjpeg` 的页面路由承载控制协议。
- 控制页首次加载、刷新、重连都只能依赖控制路由返回的 snapshot，不依赖 viewer DOM 状态。

### 3.2 独立消息模型

- 控制页与宿主之间只传控制消息，不传 viewer 专用消息。
- 控制消息至少分成三类：
  - 读模型：`ControllerSessionSnapshot`
  - 写命令：`ControllerCommand`
  - 写结果：`ControllerCommandResult`
- 控制消息必须自带 `commandId`、`controllerSessionId`、`stateVersion` 等幂等和并发控制字段。
- `ControllerSessionSnapshot` 的首包/首个快照响应必须必带 `controllerSessionId`、`stateVersion`、`lastAppliedCommandId`、`updatedAt`、`ownerControllerId`。
- 写接口统一挂载到单端口 `mjpeg` Ktor 宿主内的 `/controller/v1`，由 `common` 的 `ControllerRouteRegistrar` seam 负责注入；`app` 只提供 `ControllerCommandGateway` 和控制路由实现，`mjpeg` 不直接依赖 `app`。

### 3.2.1 路由 seam 约束

- V1 真值是 `ControllerRouteRegistrar`，它定义在 `common` 或等价非 `app` 私有层。
- `mjpeg / HttpServer` 只接收这个接口，不接收 `app` 的具体控制路由类。
- V1 不再拆分 `ControllerHttpRouteBinder` 命名，统一用 `ControllerRouteRegistrar`。
- `app` 在启动时把 `controllerRouteRegistrar` 作为构建参数注入给 `mjpeg / HttpServer`，由后者在构建 routing 时挂载 `/controller/v1`。
- 这样依赖方向固定为 `app -> common`、`mjpeg -> common`，不会出现 `mjpeg -> app` 的反向编译依赖。

```
app
 ├─ 提供 ControllerCommandGateway
 └─ 提供 ControllerRouteRegistrar 实现

mjpeg / HttpServer
 └─ 依赖 common.ControllerRouteRegistrar
     └─ 挂载 /controller/v1
```

### 3.3 独立鉴权

- Win 主控鉴权必须独立于 viewer PIN。
- viewer PIN 只能用于 viewer 读面，不能直接作为主控 bearer。
- 主控页必须明确区分只读权限和可写权限。

### 3.4 独立状态投影

- 控制页只看 `ControllerSessionSnapshot`，不直接读 `MeetingSessionState`。
- `ControllerSessionSnapshot` 是对宿主会话状态、流状态、错误状态和控制权状态的投影。
- `sessionStatus` 只表示房间/控制权状态，`streamStatus` 只表示出流态；前者可为 `starting/active/switching/ending/start-rejected`，后者固定按 `starting/live/stopping/failed/stopped` 解释。
- 只读页面刷新时，应该通过 snapshot 恢复当前控制面，而不是重放 viewer 页面事件。

## 4) 明确不复用的基础

| 不复用项 | 原因 |
| --- | --- |
| `mjpeg/src/main/assets/index.html` 作为控制面基础 | 它是 viewer 内容页，不是主控页 |
| `/socket` 作为控制协议基础 | 它是 viewer 历史通信通道，不适合作为主控命令面 |
| `/start-stop` 作为控制协议基础 | 它是 viewer 侧历史开停入口，不是主控协议语义 |
| viewer PIN 作为主控鉴权 | 安全边界不同，权限语义不同 |
| 直接暴露 `MeetingSessionState` | 这是内部状态机，不是 Win 主控读模型 |
| 以页面 DOM 作为控制真值 | 刷新、重载、回退都会破坏控制一致性 |

## 5) 控制面落地原则

- 控制能力必须收敛在 `ControllerCommandGateway` 边界之后。
- 控制页、宿主、状态机之间通过 common 层 DTO 和接口交互，不允许 `mjpeg -> app` 反向依赖。
- 可以复用现有单端口 Ktor 运行时，但控制路由必须作为独立挂载点接入。
- 只要存在 viewer 路由和控制路由，就必须把两者的消息模型、鉴权和状态投影分开。

## 6) 结论性验收标准

- Win 主控页有独立路由。
- Win 主控页有独立消息模型。
- Win 主控页有独立鉴权。
- Win 主控页不依赖 `mjpeg/src/main/assets/index.html`、`/socket`、`/start-stop` 作为协议基础。
- `ControllerCommandGateway`、`ControllerSessionSnapshot`、`ControllerCommandResult` 成为控制面的唯一收口。
