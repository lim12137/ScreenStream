# Win 主控 V1 第一阶段最终整合与验收报告

日期：2026-04-30

## 整合结论

- 当前分支：`feat/harmony42-foreground-hardening-phase1`
- 目标提交：`530885770302d6bccb3fbc5a9c8edda2ebc77347`
- 结论：`5308857` 已经是当前分支 `HEAD`，不存在“未并入当前分支”的额外 cherry-pick/merge 动作。
- 冲突检查结果：当前工作区仅存在 `common/app` 的未提交第一阶段整合改动；未发现冲突标记，`git diff --check` 通过，和 `5308857` 的 `mjpeg` seam 可直接共同验收。

## 已实现范围

- `common/controller` 契约
  - `ControllerContracts.kt`
  - `ControllerCommandGateway.kt`
  - `ControllerRouteRegistrar.kt`
  - `ControllerSessionSnapshotProjector.kt`
- `app` 第一阶段控制侧整合
  - `ControllerCommandGatewayImpl`：控制命令仲裁、本地手动命令桥接、状态快照投影、token exchange 基础实现
  - `BaseApp.kt`：Koin 注册 `ControllerCommandGateway` 与 `ControllerHostLauncher`
  - `SingleActivity.kt`：控制命令 intent 元数据、状态观察、前台启动失败回写、SingleActivity 接线
  - `app/src/test/.../ControllerCommandGatewayImplTest.kt`：阶段一核心单测
- `mjpeg` 单端口 `/controller/v1` seam
  - `HttpServerRouting.kt` 将控制路由根挂载到现有 HTTP server
  - `HttpServer.kt` 使用 `registerHttpServerRoutes(...)` 统一挂接 viewer 路由与控制 seam
  - `MjpegKoinModule.kt` / `MjpegStreamingService.kt` 支持注入 `ControllerRouteRegistrar`
  - `HttpServerRoutingTest.kt` 验证 viewer 根路由与 `/controller/v1/snapshot` 可同时存在

## 最小验收命令与结果

已在 `JAVA_HOME=D:\JAVA\jdk-17` 下执行。

1. `./gradlew.bat :common:testDebugUnitTest`
   - 结果：通过
   - 关键信息：`BUILD SUCCESSFUL`

2. `./gradlew.bat :mjpeg:testDebugUnitTest`
   - 结果：通过
   - 关键信息：`BUILD SUCCESSFUL`

3. `./gradlew.bat :app:compileFDroidDebugKotlin`
   - 结果：通过
   - 关键信息：`BUILD SUCCESSFUL`

4. `./gradlew.bat :app:assembleFDroidDebug`
   - 结果：通过
   - 关键信息：`BUILD SUCCESSFUL`

## 残余限制

- `mjpeg` 当前仅提供 `/controller/v1` 路由挂载 seam，仓内尚未落地真实的 HTTP 控制 handler/序列化实现；现阶段验收标准满足“单端口 seam 已接入”，不代表完整远控 API 已实现。
- `ControllerRouteRegistrar` 在 `mjpeg` 侧仍允许缺省为 `NoOp`，因此未注入真实 registrar 时控制根路径不会暴露可用业务接口。
- 本次仅执行了要求中的最小验收集合，未额外执行更大范围的集成/UI/设备侧联调。
