# My tavern 我的酒馆

安卓原生 AI 角色扮演应用。

## 功能

- **角色管理**：角色名称、角色描述、角色头像
- **世界书管理**：世界名称、世界描述
  - 规则：规则名称、规则描述、匹配方式
- **LLM 接入**：兼容 OpenAI Chat API、OpenResponses API、Anthropic API
- **聊天**：单聊、群聊
- **对话分支**：支持对话分叉与回溯
- **聊天背景**：自定义背景（单聊默认使用角色头像）
- **主题**：多界面主题（暗黑、白天等）
- **数据导出/导入**：全部数据支持导出和导入

## 技术栈

- **UI**：Jetpack Compose + Material 3
- **架构**：MVVM + Repository 模式
- **依赖注入**：手动 DI（AppContainer）
- **导航**：Compose Navigation
- **数据持久化**：Room + SQLite
- **网络请求**：Retrofit + OkHttp + kotlinx.serialization
- **图片加载**：Coil
- **异步**：Kotlin Coroutines + Flow

## 项目结构

```
org.jiangstack.mytavern
├── data/
│   ├── local/          # Room 数据库、Entity、Dao
│   ├── remote/         # LLM API 接口、Retrofit 配置
│   └── repository/     # Repository 实现
├── domain/
│   ├── model/          # 业务实体（角色、世界书、对话等）
│   └── repository/     # Repository 接口
├── ui/
│   ├── theme/          # 主题、颜色、字体
│   ├── navigation/     # Compose Navigation 路由
│   ├── character/      # 角色管理界面
│   ├── worldbook/      # 世界书管理界面
│   ├── chat/           # 聊天界面
│   └── settings/       # 设置界面
├── AppContainer.kt     # 手动 DI 容器
├── MyTavernApplication.kt
└── MainActivity.kt
```

## 数据模型

### 角色 (Character)
- id: Long
- name: String
- description: String
- avatarUri: String?

### 世界书 (WorldBook)
- id: Long
- name: String
- description: String

### 规则 (WorldBookRule)
- id: Long
- worldBookId: Long (外键)
- name: String
- description: String
- matchType: String (匹配方式)

### 对话会话 (ChatSession)
- id: Long
- type: String (单聊 / 群聊)
- title: String
- backgroundUri: String?
- createdAt: Long

### 对话消息 (ChatMessage)
- id: Long
- sessionId: Long (外键)
- senderId: Long? (角色 id，null 表示用户)
- content: String
- timestamp: Long
- parentMessageId: Long? (用于分支)

### LLM 配置 (LlmConfig)
- id: Long
- name: String
- apiType: String (openai / openresponses / anthropic)
- baseUrl: String
- apiKey: String
- model: String

## 导出/导入

- 导出：将 Room 数据库文件复制到用户指定路径（.db 格式）
- 导入：从 .db 文件恢复，替换当前数据库（需确认覆盖）

## 开发规范

- 所有 UI 使用 Compose 实现，不使用 XML Layout
- 状态管理使用 ViewModel + StateFlow
- 数据库操作统一在 Repository 层，通过 Flow 暴露给 UI
- 字符串统一放入 `res/values/strings.xml`
- 颜色/主题使用 Material 3 动态颜色或预定义配色
