# privchat-ui

[English](./README.md) | 简体中文

PrivChat 的共享 UI 层：全部页面与聊天组件用 Kotlin Multiplatform 写一遍，Android 与 iOS
App 原样复用。

- 坐标：`com.netonstream.privchat:privchat-ui`（目前只作 composite build 使用，尚未发布）
- 平台：Android、iOS（arm64 / 模拟器 arm64 / x64）
- 基于 [GearUI Kit](https://github.com/gearui/gearui-kit) 做界面，
  基于 [privchat-sdk-kotlin](https://github.com/privchat/privchat-sdk-kotlin) 取数据

## 所处位置

```
┌──────────────────────────────────────────────────────────┐
│  宿主 App              最终发行的 App：登录、启动、导航壳、  │
│                        推送、设置                          │
├──────────────────────────────────────────────────────────┤
│  privchat-ui（本仓库）  页面 + 聊天组件 + 运行时状态；        │
│                        不拥有任何网络逻辑                    │
├────────────────────────────┬─────────────────────────────┤
│  gearui-kit                │  privchat-sdk-kotlin         │
│  设计系统、token、72 个组件、│  PrivchatClient、DTO、事件——  │
│  i18n、浮层 + 安全区运行时   │  Rust SDK 的 UniFFI 绑定      │
├────────────────────────────┴─────────────────────────────┤
│  KuiklyUI（Compose Multiplatform 渲染器） · privchat-sdk（Rust） │
└──────────────────────────────────────────────────────────┘
```

从这张图推出两条规则，代码结构就是为了让它们一直成立：

1. **privchat-ui 只通过 `PrivchatClient` 接触网络。** 它不开 socket、不解析线上报文、
   不决定何时重连。这些都由 Rust SDK 做，Kotlin 绑定暴露出来，本层负责渲染。
2. **privchat-ui 依赖的是 GearUI Kit，不是 KuiklyUI。** `build.gradle.kts` 里没有任何
   KuiklyUI 依赖声明。按钮、输入框、列表、底部面板、toast、对话框、主题、间距、i18n 管线
   ——全部来自 `com.gearui.*`。页面代码里出现的 Compose 原语（`com.tencent.kuikly.compose.*`
   下的 `Modifier`、布局、动画）是 GearUI Kit 以 `api` 方式对外暴露的那部分，传递依赖过来的；
   kit 哪天换渲染器，它们跟着 kit 走。

## SDK 层怎么用

SDK 经由一个门面 `PrivChat` 和一个运行时仓库 `ClientRuntime` 进入 UI。页面自己不持有 client。

### `PrivChat`——数据门面

`com.netonstream.privchat.ui.PrivChat` 是单例，App 注入一次 client，之后所有人从它读。

```kotlin
// App 层，SDK client 建好之后
PrivChat.init(client)          // client: com.netonstream.privchat.sdk.PrivchatClient
```

它把 SDK **自己的 DTO** 以 `StateFlow` 暴露——**没有映射层，没有自己的 view model**。
SDK 叫 `ChannelListEntry` / `MessageEntry` / `FriendEntry` / `GroupEntry` / `PresenceEntry`
的东西，页面收到的就是同一个类型：

```kotlin
val channels   by PrivChat.channels.collectAsState()      // List<ChannelListEntry>
val messages   by PrivChat.messages.collectAsState()      // List<MessageEntry>
val friends    by PrivChat.friends.collectAsState()       // List<FriendEntry>
val groups     by PrivChat.groups.collectAsState()        // List<GroupEntry>
val presences  by PrivChat.presences.collectAsState()     // Map<ULong, PresenceEntry>
val me         by PrivChat.currentUserId.collectAsState()
```

写操作直接打到 client，它就是为此暴露的：

```kotlin
PrivChat.client.sendText(channelId, channelType, "hello")
PrivChat.client.forwardMessage(...)
```

`PrivChat` 在 SDK 之上唯一多出来的，是 **SDK 不该知道的本地 UI 状态**：每个频道的输入草稿与
回复对象（通过平台 `DraftStore` 持久化、冷启恢复）、服务端还没追上时先把未读角标清零的
「已读水位」、发送中的图片尺寸、语音播放进度之类。它们放在 `channelLocalStates` 与 `uiState`。

登出 / 切号调 `PrivChat.reset()` 清空一切，账号之间不串数据。

### `ClientRuntime`——连接 / 同步 / 发送队列的唯一真源

`com.netonstream.privchat.ui.runtime.ClientRuntime` 是**三条稳定性主链的统一真源**：
连接、同步、发送队列。App 层把 SDK 事件喂进来，页面只订阅它的 `StateFlow`。

```
SDK 事件 ──▶ 宿主 App 的事件处理 ──▶ ClientRuntime.on…()
                                                             │
   ConnectivityState / SyncState / SendQueueState  ◀─────────┘
                     │
                     ▼
   页面读这些；没有任何页面自己去看 SDK 连接状态
```

路由进来的事件：`connection_state_changed`、`network_hint_changed`、`resume_sync_*`、
`sync_entities_applied`、`message_send_status_changed`、`outbound_queue_updated`、`forced_logout`。

这层存在的意义在于一条规则：**页面不得自行判断「离线 / 同步中 / 发送中」**。在它出现之前
每个页面各猜各的，屏幕上互相打架。现在会话列表标题变成「重连中…」和消息页发送按钮置灰，
读的是同一个布尔值。

错误以 sealed 的 `ClientRuntimeError` 跨层（`NetworkUnavailable` / `GatewayDisconnected` /
`AuthExpired` / `ServerBusy` / `SyncFailed` / `Unknown`），只能经 `userFacingMessage(strings)`
变成本地化文案给用户。原始 reason 与异常串只进日志，不上屏。

### 页面接收回调，不接收 SDK

页面签名是「数据进、意图出」：

```kotlin
@Composable
fun ConversationPage(
    onChannelClick: (ChannelListEntry) -> Unit,
    onGlobalSearch: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    statusTitle: String? = null,       // 「重连中…」顶替标题位显示
    statusBusy: Boolean = false,       // 还在努力时旁边转圈
    onPinChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    ...
)
```

导航是 App 的决定，所以以 lambda 传入。数据在页面内部从 `PrivChat` 的 flow 收集。需要 SDK
干活的，要么直接调 `PrivChat.client`，要么是 App 提供的 suspend 回调。现有页面：会话列表、
消息、联系人、好友申请、好友 / 聊天 / 群设置、建群 / 邀请 / 成员 / 审批、用户资料、全局搜索、
转发选择，以及改名 / 改备注的编辑页。

## GearUI Kit 怎么用

privchat-ui 是 GearUI Kit 的**消费方**——就是 kit 自己 README 里说的那种：依赖已发布的组件与
token，不 fork、不改皮。

- **组件**：`NavBar`、`Tabs`、`Input`、`AutoResizeTextarea`、`Button`、`Checkbox`、`Switch`、
  `Cell`、`SearchBar`、`SwipeCell`、`Swiper`、`ActionSheet`、`Dialog` / `ConfirmDialog`、
  `ContextMenu`、`Toast`、`Loading`、`EmptyState`、`GearImage`、`Icons`——全仓 176 处
  `com.gearui.components` import。聊天特有的部件（消息气泡、操作菜单、带在线点的头像、
  媒体缩略图、语音条）都是拿这些拼的，不是从零写的。
- **用 token 不用字面量**：颜色取 `Theme.colors`，字体取 `Typography`，间距取 `Spacing`，
  圆角取 `Theme.shapes`——kit 自己 CI 里守着的四条轴。kit 换主题，App 整体换皮。
  （如实说明：还有约二十处 `Color(0x…)` 字面量散在五个文件里，多是聊天气泡与九宫格头像的
  着色。那是债不是设计；kit 的颜色护栏尚未在本仓库运行。）
- **运行时**：`PageScaffold` 消费安全区；底部面板与 toast 走 kit 的 `OverlayRoot`；
  本仓库没有任何地方手读 `safeAreaInsets`。
- **i18n**：`PrivChatI18nProvider` 接进 kit 的语言管线。它读 kit 的 `App` 已经提供的
  `LocalLanguageTag`，解析出对应的 `PrivChatStrings` 语言包（内置 zh-Hans / zh-Hant / en / vi）。
  App **只在** `App(languageTag = …)` 设一次语言；这个 provider 不能再单独传 tag。
  文案按域拆分，任何单个类都不会撞上 Android 的 DEX / 255 参数上限。

关于 `com.tencent.kuikly.compose.*` 下的 `Modifier`、布局容器和动画 import：它们不是第二个
依赖。GearUI Kit 把 `com.tencent.kuikly-open:compose` 以 `api` 暴露，所以它们本来就是「依赖
GearUI Kit」的一部分；privchat-ui 自己没有声明任何 KuiklyUI。

## 接进 App

宿主 App 这样把三层拼起来——摘自真实 App，不是为 README 编的：

```kotlin
// 1. SDK client 由 App 创建（设备 id、服务器、存储路径……）
val client = PrivchatClient(...)
PrivChat.init(client)

// 2. 把 SDK 事件路由进运行时仓库（事件带字符串 `type`）
fun handleSdkEvent(envelope: SdkEventEnvelope) {
    when (envelope.event.type.lowercase()) {
        "connection_state_changed" ->
            envelope.event.toState?.let { ClientRuntime.onConnectionStateChanged(it) }
        "network_hint_changed" -> {
            val offline = envelope.event.toNetworkHint.equals("offline", ignoreCase = true)
            ClientRuntime.onNetworkReachableChanged(!offline)
        }
        "forced_logout" -> ClientRuntime.onAuthExpired()
        // resume_sync_* / sync_entities_applied / message_send_status_changed / …
    }
}

// 3. Compose 根：GearUI App → PrivChat i18n → 你的页面
App(
    themeMode = settings.themeMode,
    isSystemDark = isSystemDark,
    languageTag = settings.languageTag,       // 语言唯一的设置点
) {
    PrivChatI18nProvider {
        ConversationPage(
            onChannelClick = { navigate(it) },
            statusTitle = runtimeTitle,        // 由 ClientRuntime 推出
            statusBusy = runtimeBusy,
        )
    }
}
```

`PrivChatI18nProvider` 在 `App` **之内**、所有页面之外。

## 目录结构

```
src/commonMain/kotlin/com/netonstream/privchat/ui/
├── PrivChat.kt            SDK 数据门面（StateFlow + 本地 UI 状态）
├── runtime/               ClientRuntime：连接 / 同步 / 发送队列真源
├── pages/                 一屏一文件
├── components/            聊天特有组合件（气泡、操作菜单、头像）
├── models/                SDK DTO 之上的展示模型与小工具
├── state/                 群 / 审批 store
├── i18n/                  PrivChatStrings 分域 + 语言包（zh-Hans、zh-Hant、en、vi）
├── platform/              expect 声明：剪贴板、草稿、外链、提醒、系统栏
├── media/  voice/  avatar/  forward/  error/
└── common/                基础控件、跨平台适配、工具
src/androidMain, src/iosMain   platform/ 的 actual
src/commonTest                 pages / runtime / components / error 测试
```

## 构建

本仓库以 Gradle **composite build** 方式被消费。`settings.gradle.kts` 把两个依赖替换成
同级 checkout：

```
com.gearui:gearui-kit           → ../gearui-kit
com.netonstream.privchat:sdk    → ../privchat-sdk-kotlin
```

所以三个仓库必须并排放。SDK 的 Rust FFI 由 SDK 自己的 Gradle 串起来构建，这里不用手动
编 `.so` / `.a`。

```bash
./gradlew compileDebugKotlinAndroid          # Android
./gradlew compileKotlinIosSimulatorArm64     # iOS
./gradlew testDebugUnitTest                  # commonTest 走 Android 单元测试 runner
./gradlew iosSimulatorArm64Test              # commonTest 以 Kotlin/Native 在模拟器上跑
```

没有 JVM target，`commonTest` 只能走上面两条之一。

## 相关仓库

- [GearUI Kit](https://github.com/gearui/gearui-kit)——本仓库依赖的设计系统
- [privchat-sdk-kotlin](https://github.com/privchat/privchat-sdk-kotlin)——Kotlin SDK 绑定
