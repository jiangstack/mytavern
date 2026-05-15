# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# My tavern 我的酒馆

安卓原生 AI 角色扮演应用。

## 常用命令

```bash
# 构建 debug APK
./gradlew :app:assembleDebug

# 安装到连接的设备
./gradlew :app:installDebug

# 运行单元测试
./gradlew :app:testDebugUnitTest

# 运行单个单元测试类
./gradlew :app:testDebugUnitTest --tests "org.jiangstack.mytavern.ExampleUnitTest"

# 运行 instrumentation 测试
./gradlew :app:connectedAndroidTest

# 清理构建产物
./gradlew clean
```

## 技术栈

- **UI**：Jetpack Compose + Material 3（纯 Compose，无 XML Layout）
- **架构**：MVVM + Repository 模式
- **依赖注入**：手动 DI（`AppContainer.kt`）
- **导航**：Compose Navigation（路由定义见 `ui/navigation/Screen.kt`）
- **数据持久化**：Room + SQLite（版本 5，schema 导出至 `app/schemas`）
- **网络请求**：Retrofit + OkHttp + kotlinx.serialization
- **图片加载**：Coil
- **异步**：Kotlin Coroutines + Flow

## 项目结构

```
org.jiangstack.mytavern
├── data/
│   ├── local/          # Room 数据库、Entity、Dao
│   ├── remote/         # LLM API 接口（OpenAI / OpenResponses / Anthropic）
│   └── repository/     # Repository 实现
├── domain/
│   ├── model/          # 业务实体（角色、世界书、对话等）
│   ├── repository/     # Repository 接口
│   └── service/        # LLMService（流式/非流式请求、多 API 适配）
├── ui/
│   ├── theme/          # 主题、颜色、字体
│   ├── navigation/     # Compose Navigation 路由
│   ├── character/      # 角色管理界面
│   ├── worldbook/      # 世界书管理界面
│   ├── chat/           # 聊天界面
│   └── settings/       # 设置界面
├── AppContainer.kt     # 手动 DI 容器：单例数据库、Repository、Retrofit、OkHttp
├── MyTavernApplication.kt
└── MainActivity.kt
```

## 依赖注入（AppContainer）

`AppContainer` 在 `MyTavernApplication` 中创建，通过 `LocalContext.current.applicationContext as MyTavernApplication` 在 Composable 中获取。不依赖 Hilt/Koin 等框架。

## 导航路由

| 路由 | 说明 |
|---|---|
| `character_list` | 角色列表 |
| `character_detail/{characterId}` | 角色详情/编辑 |
| `worldbook_list` | 世界书列表 |
| `worldbook_detail/{worldBookId}` | 世界书详情/编辑 |
| `chat_list` | 聊天会话列表 |
| `chat_detail/{sessionId}` | 聊天详情 |
| `settings` | 设置 |

## LLM 服务架构

`LlmService` 统一处理三种 API：
- **OpenAI Chat API**：标准 chat completions
- **OpenResponses API**：OpenAI Responses API
- **Anthropic API**：Messages API

支持流式（`Flow<StreamChunk>`）和非流式响应。Retrofit baseUrl 默认为 `https://api.openai.com/`，实际请求 URL 由 `LlmConfig.baseUrl` 动态决定（通过 OkHttp 拦截器替换）。

流式请求通过 OkHttp 直接发起（非 Retrofit），SSE 解析在 `LlmService.sendChatMessageStream` 中逐行处理。请求中设置 `stream_options: { "include_usage": true }` 后，OpenAI 会在最后一个 SSE chunk（`choices: []`）中返回 `usage` 对象（`prompt_tokens`、`completion_tokens`、`total_tokens`）。`StreamChunk` 中通过 `usage` 字段传递该信息，ViewModel 在 collect 结束后将 usage 保存到 `ChatMessage`。

## 聊天消息模型

`ChatMessage` 关键字段：
- `senderId: Long?` — `null` 表示用户发送的消息，非 `null` 表示 AI 角色 ID
- `senderName: String?` — AI 角色名称，用户消息时为 `null`
- `promptTokens / completionTokens / totalTokens: Int?` — LLM 返回的 usage 统计

消息列表通过 `senderId == null` 区分用户消息和 AI 消息，UI 中分别靠右/靠左显示不同颜色的气泡。

## 群聊机制

群聊中用户发送消息后，AI 回复的目标角色选择逻辑（`ChatDetailViewModel.sendMessage`）：
1. **@提及优先**：解析消息中的 `@角色名`，只让被提及的角色回复
2. **接话模式**：无 @ 提及时，找到最后一条 AI 消息的发送者，让该角色继续回复
3. **默认第一个角色**：以上都不满足时，让群聊角色列表中的第一个角色回复

多个目标角色的回复是**并发同时请求**的，每个角色有独立的协程和流式状态（`streamingStates: Map<Long, StreamingState>`），以角色 ID 为 key 管理。双击某条 AI 消息的角色名可触发该角色的下一次回复（调用 `triggerCharacterReply`）。

## Room 数据库

- **数据库名**：`mytavern.db`
- **当前版本**：5
- **迁移**：`Migration_1_2`、`Migration_2_3`、`Migration_3_4`、`Migration_4_5`（位于 `data/local/`）
  - `Migration_3_4`：新增 `session_characters` 关联表（支持群聊多角色）
  - `Migration_4_5`：为 `chat_messages` 表添加 `promptTokens`、`completionTokens`、`totalTokens` 字段（LLM usage 统计）
- **schema 导出**：`ksp { arg("room.schemaLocation", "$projectDir/schemas") }`

## 导出/导入

- **导出**：将 Room 数据库文件复制到用户指定路径（`.db` 格式）
- **导入**：从 `.db` 文件恢复，替换当前数据库（需确认覆盖）

## 开发规范

- 所有 UI 使用 Compose 实现，不使用 XML Layout
- 状态管理使用 ViewModel + StateFlow
- 数据库操作统一在 Repository 层，通过 Flow 暴露给 UI
- 字符串统一放入 `res/values/strings.xml`
- 颜色/主题使用 Material 3 动态颜色或预定义配色
