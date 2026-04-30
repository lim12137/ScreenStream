# Win 主控 V1 Phase 2 工程审查记录

日期：2026-05-01

来源：Hubble 子代理工程审查结果正式落盘。

## 结论

- 评分：7/10
- 当前方案可以继续推进，但还没有达到可直接进入真实控制面验收的状态。
- 现阶段的核心问题不是协议描述不足，而是落地链路仍停留在 seam 和桥接层，真实 `/controller/v1` 控制面还没有完成闭环。

## 已确认的主要问题

- 真实 registrar 未落地。
  - `mjpeg` 侧仍依赖注入 seam，而不是已实现的真实 `ControllerRouteRegistrar`。
- 真实 `/controller/v1` HTTP 控制面未实现。
  - 当前还没有可直接对外提供的完整控制 handler。
- 鉴权仍是占位。
  - 主控鉴权、scope、token exchange 的实际收口还没有形成可验收实现。
- 当前控制链路仍是 Activity intent 桥接。
  - 控制命令还主要通过 `SingleActivity` / intent / gateway 传递，没有变成真正的 HTTP 控制面。
- 主 CI 未覆盖 `common` / `mjpeg` 单测。
  - 现在的主验收仍偏向 `app` 路径，缺少对控制契约和服务端基础模块的常规 CI 保护。

## 必须锁死的边界

- `mjpeg` 继续只负责单端口宿主和路由挂载，不承担主控协议演进的业务真值。
- `app` 负责控制命令和鉴权逻辑的实现，不把 viewer PIN 直接升级成主控凭据。
- `common` 继续承载契约、DTO 和状态投影，不把 UI 或服务端私有逻辑塞进去。
- 真实控制面必须收口到 `/controller/v1`，不能再依赖 Activity intent 作为最终控制通道。
- 鉴权必须独立于 viewer 侧逻辑，不能用临时占位逻辑混过首轮落地。

## 推荐实施顺序

1. 先落地真实 `ControllerRouteRegistrar`，把 `/controller/v1` 真实挂到 `mjpeg` 宿主上。
2. 再把真实 `/controller/v1` HTTP handler 接出来，替换当前 Activity intent 桥接。
3. 再补完整鉴权收口、token exchange、scope 和错误码。
4. 再接 Win 主控 UI，让 UI 只消费稳定的 HTTP 控制面。
5. 最后补 CI 门禁，把 `common` 和 `mjpeg` 的基础单测纳入主验证链。

## 最小测试 / CI 闭环

- `./gradlew :common:testDebugUnitTest`
- `./gradlew :mjpeg:testDebugUnitTest`
- `./gradlew :app:testFDroidDebugUnitTest`
- `./gradlew :app:assembleFDroidDebug`

最小要求不是只跑 `app`，而是要让 `common` / `mjpeg` 的基础单测进入主 CI 可见范围，避免控制契约和服务端宿主退化后直到集成阶段才暴露。

## 结论

这份审查结论支持继续做 Phase 2，但前提是严格按“真实控制面 -> 鉴权 -> UI -> CI”顺序推进，不要把现在的 intent 桥接误判成已经完成的 HTTP 控制面。
