# 原生顶栏 + WebView 内容边界与下拉交互规范

> 历史参考说明：本文档不再作为 Win 主控 V1 的当前真值，只保留当前 Android 宿主壳的历史边界记录。Win 主控边界以后以 `docs/main-controller-boundary-2026-04-30.md` 和 `docs/win-main-controller-v1-lan-protocol-and-android-shell-boundary-2026-04-30.md` 为准。

## 1) 目标与适用范围

- 目标：把 ScreenStream 的“宿主控制面”和“网页内容面”分开，形成稳定、可维护的单 `Activity` 结构。
- 适用范围：当前本地局域网 MJPEG 方案、单 `Activity` 宿主、单 `WebView` 内容页。
- 不适用范围：`mjpeg` 协议层、服务层状态机、WebRTC/RTSP 路径、页面内业务流程重构。

## 2) 架构边界

| 层级 | 职责 | 说明 |
| --- | --- | --- |
| 原生顶栏 | 宿主入口、状态展示、下拉触发、页面级控制 | 放在 `SingleActivity` 的原生 UI 外层，不进 WebView。 |
| WebView 内容 | 页面渲染、页面内交互、网页端视觉内容 | 只承载网页内容，不承担宿主控制面职责。 |
| `mjpeg` 模块 | 帧采集、MJPEG 输出、推流状态 | 仅接收帧和流事件，不接管顶栏结构。 |

- 当前实现边界应保持：顶栏在 `SingleActivity` 原生外层，WebView 只负责内容区。
- 顶栏触发的状态变化必须先落到 app 层状态，再决定是否刷新 WebView。
- WebView 内如果保留页面自己的顶部展示条，只能作为页面内容的一部分，不能替代宿主顶栏。

## 3) 顶栏结构

建议顶栏按三段式组织：

1. 左侧：品牌/页面标题区
   - 标题
   - 简短副标题或当前模式说明
2. 中间：状态摘要区
   - 主机信息
   - 接入策略
   - 当前状态
   - 传输通道
3. 右侧：操作区
   - 常用动作按钮
   - 更多操作入口
   - 下拉触发器

结构原则：

- 顶栏信息优先展示“当前房间/当前目标/当前连接状态”。
- 高频动作放右侧，低频动作收进下拉。
- 顶栏高度固定，避免因内容变化导致 WebView 内容区抖动。

## 4) 下拉触发器与浮层样式规范

### 4.1 触发器

- 触发器采用“标签 + 当前值 + 下拉箭头”的组合。
- 触发面积不小于 `44dp`，保证手指点击稳定。
- 触发器只表达当前选择，不直接承担复杂编辑。
- 选中态需要有可见反馈，例如底色、描边或轻量阴影变化。

### 4.2 浮层

- 浮层应锚定触发器展开，优先向下展开；空间不足时自动翻转。
- 浮层宽度以内容为主，最大宽度受屏幕约束，避免覆盖整个内容区。
- 浮层圆角建议统一，视觉层级高于页面内容，但低于系统级弹窗。
- 浮层背景保持高对比和可读性，不透明度不要过低。
- 浮层内项高保持一致，点击反馈明确，危险项单独强调。

### 4.3 视觉约束

- 标题、摘要、值域文字层级要清晰分离，避免“所有信息都一样重”。
- 下拉箭头仅作辅助，不作为唯一可点击暗示。
- 浮层出现时，顶栏其余元素不要同步闪烁或重排。

## 5) 交互规则

- 单击触发器打开或关闭对应浮层。
- 同时只允许一个浮层处于打开状态。
- 点击外部区域、按返回键、切换页面时关闭浮层。
- 选择某项后先更新 app 层状态，再决定是否刷新 WebView 或切换目标。
- 顶栏动作不能依赖 WebView DOM 状态作为唯一真值。
- 顶栏与 WebView 的状态必须可独立恢复，避免页面重载导致控制面丢失。
- 误触成本要低，危险操作必须二次确认。

## 6) 当前实现建议（只改 app 层，不改 mjpeg）

- 只在 `app` 层做改动，把原生顶栏放进 `SingleActivity` 的宿主容器。
- `SingleActivity` 当前是直接 `setContentView(webView)`，如果要落地原生顶栏，应先把宿主改成“顶栏 + WebView 内容”的组合容器。
- 顶栏状态建议继续复用 `SingleActivityMeetingHost` / `MeetingSessionCoordinator` 的 app 侧状态，不把顶栏逻辑下沉到 `mjpeg`。
- 顶栏的下拉、切换、选择结果只驱动 app 层状态和页面加载，不修改 `mjpeg` 的事件模型。
- `mjpeg` 保持现有职责：接收 `WebView` 帧、维持 MJPEG 推流、处理流状态。
- 如果需要页面内的展示条，可以保留在 `mjpeg/src/main/assets/index.html`，但它只属于 WebView 内容，不是宿主顶栏。
- 这份文档里的顶栏 / WebView 约定只适用于当前 Android 宿主壳，不代表 Win 主控页的设计真值。

## 7) 相关代码参考路径

- `app/src/main/java/info/dvkr/screenstream/SingleActivity.kt`
- `app/src/main/java/info/dvkr/screenstream/ui/ScreenStreamContent.kt`
- `app/src/main/java/info/dvkr/screenstream/ui/tabs/stream/StreamTabContent.kt`
- `mjpeg/src/main/assets/index.html`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/MjpegStreamingModule.kt`
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/MjpegStreamingService.kt`
