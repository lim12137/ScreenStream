# Win 主控 V1 第一阶段状态与 CI 前置说明

日期：2026-05-01

## 1) 当前已完成的一阶段范围

- `common` 已补齐 Win 主控控制面契约：
  - `ControllerCommandGateway`
  - `ControllerRouteRegistrar`
  - `ControllerSessionSnapshot` / `ControllerCommandResult` / `ControllerTokenExchangeResult`
  - 状态枚举、命令 DTO、错误 DTO、snapshot projector
- `app` 已完成控制面核心接线：
  - `ControllerCommandGatewayImpl`
  - `BaseApp.kt` 里的 Koin 注入
  - `SingleActivity.kt` 里的命令 intent 桥接、`ForegroundStartFailed` 回写、状态观察
- `mjpeg` 已完成单端口挂载 seam：
  - `HttpServerRouting.kt` 把 `/controller/v1` 挂进现有 Ktor routing 树
  - `HttpServer.kt` / `MjpegStreamingService.kt` 支持注入 `ControllerRouteRegistrar`
  - 测试已验证 viewer 根路由和 controller 挂载点可以并存
- 已有验收覆盖了核心单测、控制契约测试和 FDroid debug 构建路径。

## 2) 仍未完成的范围

- 真实的 `/controller/v1` HTTP handler 还没有落成完整可用接口。
- 鉴权收口、token exchange 的完整 HTTP 暴露、权限校验和 Win 主控 UI 仍在后续阶段。
- `mjpeg` 侧仍保留 `NoOpControllerRouteRegistrar` 兜底，未注入真实 registrar 时不会暴露可用业务接口。

## 3) 未 push 原因

- 本次任务边界明确要求“只负责更新本次实施相关文档说明并提交，不做 push”。
- 因此本次只做本地 commit，不执行 push / land。

## 4) 后续 Win 主控 V1 下一步

- 先补 `app` 侧真实 HTTP 路由实现，把控制命令从 gateway 暴露出来。
- 再补鉴权、scope、token exchange 与幂等 / 版本冲突处理。
- 然后补 Win 主控页本体，接入 snapshot 恢复、命令下发和失败回显。
- 最后补契约测试、集成测试和必要的 CI 验证说明。

## 5) action / CI 前置说明

- 当前 GitHub Actions 工作流是 `.github/workflows/android-build.yml`。
- 该工作流在 `push` / `pull_request` / `workflow_dispatch` 下运行，核心验证项是：
  - `./gradlew :app:testFDroidDebugUnitTest --stacktrace`
  - `./gradlew :app:assembleFDroidDebug --stacktrace`
- 运行前置条件：
  - JDK 17
  - Android SDK `platforms;android-36`
  - Android Build Tools `36.1.0`
  - `gradlew` 可执行
- 如果要覆盖 release job，还需要仓库 secrets 中存在 release keystore 相关配置；否则 release job 会自然跳过。

## 6) 验收口径参考

- 这份说明文件只记录当前阶段状态和 CI 前置条件。
- 更完整的历史验收过程与结果，继续以 `docs/win-main-controller-v1-phase1-final-integration-acceptance-2026-04-30.md` 为准。
