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
- **数据持久化**：Room + SQLite（版本 3，schema 导出至 `app/schemas`）
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

## Room 数据库

- **数据库名**：`mytavern.db`
- **当前版本**：3
- **迁移**：`Migration_1_2`、`Migration_2_3`（位于 `data/local/`）
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
