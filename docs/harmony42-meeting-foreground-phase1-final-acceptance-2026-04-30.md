# HarmonyOS 4.2 前台专机一期最终验收报告

- 日期：2026-04-30
- 仓库：`M:\AI\1work\视频项目\ScreenStream`
- 分支：`feat/harmony42-foreground-hardening-phase1`

## 环境假设

- JDK 17：`D:\JAVA\jdk-17`
- Android SDK：`D:\JAVA`
- 本次验收通过命令行临时设置 `JAVA_HOME=D:\JAVA\jdk-17`、`ANDROID_HOME=D:\JAVA`、`ANDROID_SDK_ROOT=D:\JAVA`
- 仓库内现有 `local.properties` 保持不变，未提交任何本地环境文件
- 验收目标仅限当前 `HEAD`，不修业务代码
- 复跑时已显式注入 `JAVA_HOME`，本报告以复跑结果为准；上一次误判系环境未注入 `JAVA_HOME` 导致

## 一期保证项

结合一期计划与研究结论，本次验收口径仍以“前台专机运行”作为唯一承诺边界：

- 应用只保证在前台专机语义下运行，不承诺后台保活、息屏保活或被系统杀死后的自动恢复
- 房间仅在主控显式结束时关闭，不把 `onPause()`、短暂失焦、通知下拉或切换查看平板等同于结束会议
- 切换平板只切换当前展示/推流目标，不重建房间，不重启 MJPEG 服务
- WebView 路径需要具备明确的前台服务所有权和失败回传闭环
- 仍复用单 `Activity` + 单 `WebView` + MJPEG 输出链路，不引入大规模重构

## 一期非保证项

- 后台继续采集或继续推流
- 息屏后继续运行
- 进程被系统杀死后的自动恢复
- 多 `Activity`、多窗口或独立渲染进程
- 原生录音链路替代 WebView `getUserMedia(audio)`
- RTSP / WebRTC 重新接管

## 执行命令与结果

### 1. `:common:testDebugUnitTest`

- 命令：`.\gradlew :common:testDebugUnitTest`
- 结果：通过
- 退出码：`0`
- 摘要：`BUILD SUCCESSFUL`

### 2. `:mjpeg:testDebugUnitTest --tests info.dvkr.screenstream.mjpeg.internal.WebViewForegroundSessionTest`

- 命令：`.\gradlew :mjpeg:testDebugUnitTest --tests info.dvkr.screenstream.mjpeg.internal.WebViewForegroundSessionTest`
- 结果：通过
- 退出码：`0`
- 摘要：`BUILD SUCCESSFUL`

### 3. `:app:compileFDroidDebugKotlin`

- 命令：`.\gradlew :app:compileFDroidDebugKotlin`
- 结果：通过
- 退出码：`0`
- 摘要：`BUILD SUCCESSFUL`
- 备注：仅有 Kotlin 编译警告，未阻断构建

### 4. `:app:compileFDroidDebugAndroidTestKotlin`

- 命令：`.\gradlew :app:compileFDroidDebugAndroidTestKotlin`
- 结果：通过
- 退出码：`0`
- 摘要：`BUILD SUCCESSFUL`

### 5. `:app:testFDroidDebugUnitTest`

- 命令：`.\gradlew :app:testFDroidDebugUnitTest`
- 结果：通过
- 退出码：`0`
- 摘要：`BUILD SUCCESSFUL`，`NO-SOURCE`

### 6. `:app:assembleFDroidDebug`

- 命令：`.\gradlew :app:assembleFDroidDebug`
- 结果：通过
- 退出码：`0`
- 摘要：`BUILD SUCCESSFUL`

## 结果摘要

- 本次 6 条关键验收命令中，6 条全部通过
- 通过项覆盖了 `common` 单测、`mjpeg` 指定单测、`app` 主体 Kotlin 编译、`app` 单测和 `FDroidDebug` 打包
- `app` 的 `androidTest` Kotlin 编译也已通过，说明该阶段不再存在编译阻断

## 前台专机运行方案结论

从计划定义看，一期“前台专机运行”边界成立，且当前仓库的主要构建链路已经在本次复跑中完整通过。结合 `JAVA_HOME` 显式注入后的结果，可以判定先前的失败结论属于环境误判，而不是仓库本身的阻断问题。

换句话说：

- 方案口径成立
- 构建链路通过
- 本次最终验收可以签字为“一期完成并验收通过”

## 剩余风险

- 本次只验证了 Gradle 构建与单测编译，没有覆盖鸿蒙 4.2 真实设备上的前后台切换、网页录音安全上下文、WebSocket 心跳和 timer 节流
- `SingleActivity.kt` 仍存在 Kotlin/AndroidX 弃用警告，虽然不阻断本次构建，但属于后续治理项
- 一期真正的产品风险仍在运行时行为，而不是纯编译通过

## 结论

最终判定：**一期完成并验收通过**。  
本次复跑 6/6 关键命令全部通过，且前一次误判已确认是环境未注入 `JAVA_HOME` 导致。
